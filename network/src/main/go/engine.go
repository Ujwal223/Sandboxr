package firestack

import (
	"encoding/binary"
	"errors"
	"fmt"
	"net"
	"sync"
	"sync/atomic"
	"time"
)

// IP and Protocol Constants
const (
	ProtoICMP = 1
	ProtoTCP  = 6
	ProtoUDP  = 17

	PortDNS = 53
)

// EngineStats holds real-time telemetry metrics for the network engine
type EngineStats struct {
	TotalPacketsReceived uint64
	TotalPacketsSent     uint64
	TotalBytesReceived   uint64
	TotalBytesSent       uint64
	DnsQueriesTotal      uint64
	DnsQueriesBlocked    uint64
	PacketsDropped       uint64
	ActiveRoutesCount    uint32
}

// Engine is the central headless network coordinator
type Engine struct {
	tun       *TunDevice
	blocklist *DnsBlocklist
	routes    *RoutingTable

	running atomic.Bool
	stopCh  chan struct{}
	wg      sync.WaitGroup

	stats EngineStats
}

// NewEngine instantiates a new headless firestack engine
func NewEngine() *Engine {
	return &Engine{
		blocklist: NewDnsBlocklist(),
		routes:    NewRoutingTable(),
		stopCh:    make(chan struct{}),
	}
}

// Start begins packet processing on the given TUN device file descriptor
func (e *Engine) Start(tunFd int, mtu int) error {
	if e.running.Load() {
		return errors.New("engine is already running")
	}

	tun, err := NewTunDevice(tunFd, mtu)
	if err != nil {
		return fmt.Errorf("failed to initialize tun device: %w", err)
	}

	e.tun = tun
	e.stopCh = make(chan struct{})
	e.running.Store(true)

	e.wg.Add(1)
	go e.packetLoop()

	return nil
}

// Stop halts the engine and closes the TUN device
func (e *Engine) Stop() error {
	if !e.running.CompareAndSwap(true, false) {
		return nil
	}

	close(e.stopCh)
	if e.tun != nil {
		_ = e.tun.Close()
	}

	e.wg.Wait()
	return nil
}

// IsRunning returns whether the engine is actively routing
func (e *Engine) IsRunning() bool {
	return e.running.Load()
}

// Blocklist returns the DNS blocklist instance
func (e *Engine) Blocklist() *DnsBlocklist {
	return e.blocklist
}

// Routes returns the routing table instance
func (e *Engine) Routes() *RoutingTable {
	return e.routes
}

// GetStats returns a snapshot of current telemetry statistics
func (e *Engine) GetStats() EngineStats {
	return EngineStats{
		TotalPacketsReceived: atomic.LoadUint64(&e.stats.TotalPacketsReceived),
		TotalPacketsSent:     atomic.LoadUint64(&e.stats.TotalPacketsSent),
		TotalBytesReceived:   atomic.LoadUint64(&e.stats.TotalBytesReceived),
		TotalBytesSent:       atomic.LoadUint64(&e.stats.TotalBytesSent),
		DnsQueriesTotal:      atomic.LoadUint64(&e.stats.DnsQueriesTotal),
		DnsQueriesBlocked:    atomic.LoadUint64(&e.stats.DnsQueriesBlocked),
		PacketsDropped:       atomic.LoadUint64(&e.stats.PacketsDropped),
		ActiveRoutesCount:    uint32(e.routes.Count()),
	}
}

// packetLoop processes inbound packets from the TUN interface
func (e *Engine) packetLoop() {
	defer e.wg.Done()

	for {
		select {
		case <-e.stopCh:
			return
		default:
		}

		packet, err := e.tun.ReadPacket()
		if err != nil {
			if e.running.Load() {
				// Avoid busy spin on error
				time.Sleep(2 * time.Millisecond)
			}
			continue
		}

		if len(packet) < 20 {
			continue
		}

		atomic.AddUint64(&e.stats.TotalPacketsReceived, 1)
		atomic.AddUint64(&e.stats.TotalBytesReceived, uint64(len(packet)))

		e.handleInboundPacket(packet)
	}
}

// handleInboundPacket inspects an IP packet and routes or blocks it
func (e *Engine) handleInboundPacket(packet []byte) {
	ipVersion := packet[0] >> 4

	if ipVersion == 4 {
		e.handleIPv4Packet(packet)
	} else if ipVersion == 6 {
		e.handleIPv6Packet(packet)
	}
}

// handleIPv4Packet processes IPv4 packet
func (e *Engine) handleIPv4Packet(packet []byte) {
	ihl := int((packet[0] & 0x0F) * 4)
	if len(packet) < ihl {
		return
	}

	protocol := packet[9]
	srcIP := net.IP(packet[12:16])
	dstIP := net.IP(packet[16:20])

	if protocol == ProtoUDP && len(packet) >= ihl+8 {
		udpHeader := packet[ihl : ihl+8]
		srcPort := binary.BigEndian.Uint16(udpHeader[0:2])
		dstPort := binary.BigEndian.Uint16(udpHeader[2:4])
		udpLen := int(binary.BigEndian.Uint16(udpHeader[4:6]))

		if dstPort == PortDNS && len(packet) >= ihl+udpLen {
			dnsPayload := packet[ihl+8 : ihl+udpLen]
			atomic.AddUint64(&e.stats.DnsQueriesTotal, 1)

			if e.handleDnsQuery(dnsPayload, srcIP, dstIP, srcPort, dstPort) {
				// Blocked and synthetic response written
				return
			}
		}
	}
}

// handleIPv6Packet processes IPv6 packet
func (e *Engine) handleIPv6Packet(packet []byte) {
	if len(packet) < 40 {
		return
	}
	nextHeader := packet[6]
	srcIP := net.IP(packet[8:24])
	dstIP := net.IP(packet[24:40])

	if nextHeader == ProtoUDP && len(packet) >= 48 {
		srcPort := binary.BigEndian.Uint16(packet[40:42])
		dstPort := binary.BigEndian.Uint16(packet[42:44])
		udpLen := int(binary.BigEndian.Uint16(packet[44:46]))

		if dstPort == PortDNS && len(packet) >= 40+udpLen {
			dnsPayload := packet[48 : 40+udpLen]
			atomic.AddUint64(&e.stats.DnsQueriesTotal, 1)

			_ = e.handleDnsQuery(dnsPayload, srcIP, dstIP, srcPort, dstPort)
		}
	}
}

// handleDnsQuery parses DNS query and sends synthetic response if domain is blocked,
// or forwards to upstream resolver if allowed.
// Returns true if handled.
func (e *Engine) handleDnsQuery(payload []byte, srcIP, dstIP net.IP, srcPort, dstPort uint16) bool {
	header, question, err := ParseDnsQuery(payload)
	if err != nil || question == nil {
		return false
	}

	if e.blocklist.IsBlocked(question.Name) {
		atomic.AddUint64(&e.stats.DnsQueriesBlocked, 1)

		// Synthesize blocked response (0.0.0.0 for A, :: for AAAA)
		dnsResp := BuildBlockedResponse(payload, header, question, false)

		// Construct IPv4 UDP reply packet back to source
		if srcIP4 := srcIP.To4(); srcIP4 != nil {
			replyPacket := makeIPv4UDPPacket(dstIP.To4(), srcIP4, dstPort, srcPort, dnsResp)
			_, _ = e.tun.WritePacket(replyPacket)
			atomic.AddUint64(&e.stats.TotalPacketsSent, 1)
			atomic.AddUint64(&e.stats.TotalBytesSent, uint64(len(replyPacket)))
		}
		return true
	}

	// Forward non-blocked DNS queries to upstream resolver
	go e.forwardDnsToUpstream(payload, srcIP, dstIP, srcPort, dstPort)
	return true
}

// forwardDnsToUpstream forwards an allowed DNS query to upstream resolver and writes reply back to TUN
func (e *Engine) forwardDnsToUpstream(payload []byte, srcIP, dstIP net.IP, srcPort, dstPort uint16) {
	upstream := "1.1.1.1:53"
	conn, err := net.DialTimeout("udp", upstream, 2*time.Second)
	if err != nil {
		return
	}
	defer conn.Close()

	_ = conn.SetDeadline(time.Now().Add(3 * time.Second))
	if _, err := conn.Write(payload); err != nil {
		return
	}

	buf := make([]byte, 4096)
	n, err := conn.Read(buf)
	if err != nil || n <= 0 {
		return
	}

	upstreamResp := buf[:n]
	if srcIP4 := srcIP.To4(); srcIP4 != nil {
		replyPacket := makeIPv4UDPPacket(dstIP.To4(), srcIP4, dstPort, srcPort, upstreamResp)
		if e.tun != nil {
			_, _ = e.tun.WritePacket(replyPacket)
			atomic.AddUint64(&e.stats.TotalPacketsSent, 1)
			atomic.AddUint64(&e.stats.TotalBytesSent, uint64(len(replyPacket)))
		}
	}
}

// makeIPv4UDPPacket builds a raw IPv4 UDP packet
func makeIPv4UDPPacket(srcIP, dstIP net.IP, srcPort, dstPort uint16, payload []byte) []byte {
	ipHeaderLen := 20
	udpHeaderLen := 8
	totalLen := ipHeaderLen + udpHeaderLen + len(payload)

	packet := make([]byte, totalLen)

	// IPv4 Header
	packet[0] = 0x45 // Version 4, IHL 5 (20 bytes)
	packet[1] = 0x00 // DSCP / ECN
	binary.BigEndian.PutUint16(packet[2:4], uint16(totalLen))
	binary.BigEndian.PutUint16(packet[4:6], 0x1234) // Identification
	binary.BigEndian.PutUint16(packet[6:8], 0x4000) // Flags: Don't Fragment
	packet[8] = 64                                 // TTL
	packet[9] = ProtoUDP                           // Protocol: UDP
	copy(packet[12:16], srcIP)
	copy(packet[16:20], dstIP)

	// IP Checksum
	binary.BigEndian.PutUint16(packet[10:12], calculateChecksum(packet[0:20]))

	// UDP Header
	udpOffset := 20
	binary.BigEndian.PutUint16(packet[udpOffset:udpOffset+2], srcPort)
	binary.BigEndian.PutUint16(packet[udpOffset+2:udpOffset+4], dstPort)
	binary.BigEndian.PutUint16(packet[udpOffset+4:udpOffset+6], uint16(udpHeaderLen+len(payload)))
	packet[udpOffset+6] = 0 // Checksum (0 = optional in IPv4)
	packet[udpOffset+7] = 0

	// UDP Payload
	copy(packet[udpOffset+8:], payload)

	return packet
}

// calculateChecksum computes RFC 1071 internet checksum
func calculateChecksum(b []byte) uint16 {
	var sum uint32
	for i := 0; i < len(b)-1; i += 2 {
		sum += uint32(binary.BigEndian.Uint16(b[i : i+2]))
	}
	if len(b)%2 == 1 {
		sum += uint32(b[len(b)-1]) << 8
	}
	for sum > 0xFFFF {
		sum = (sum >> 16) + (sum & 0xFFFF)
	}
	return ^uint16(sum)
}
