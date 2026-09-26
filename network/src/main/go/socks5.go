package firestack

import (
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"time"
)

// Socks5Client handles SOCKS5 proxy protocol connections (RFC 1928)
type Socks5Client struct {
	ProxyHost string
	ProxyPort int
	Timeout   time.Duration
}

// NewSocks5Client creates a new SOCKS5 proxy client
func NewSocks5Client(host string, port int, timeout time.Duration) *Socks5Client {
	if timeout <= 0 {
		timeout = 10 * time.Second
	}
	return &Socks5Client{
		ProxyHost: host,
		ProxyPort: port,
		Timeout:   timeout,
	}
}

// Dial connects to a remote destination address through the SOCKS5 proxy
func (s *Socks5Client) Dial(network, targetAddr string) (net.Conn, error) {
	proxyTarget := net.JoinHostPort(s.ProxyHost, strconv.Itoa(s.ProxyPort))
	conn, err := net.DialTimeout("tcp", proxyTarget, s.Timeout)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to socks5 proxy %s: %w", proxyTarget, err)
	}

	// Step 1: Version negotiation
	// Client sends: [VER = 0x05, NMETHODS = 1, METHODS = 0x00 (NO AUTH)]
	if _, err := conn.Write([]byte{0x05, 0x01, 0x00}); err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to send socks5 handshake: %w", err)
	}

	// Server replies: [VER = 0x05, METHOD = 0x00]
	resp := make([]byte, 2)
	if _, err := io.ReadFull(conn, resp); err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to read socks5 handshake response: %w", err)
	}
	if resp[0] != 0x05 || resp[1] != 0x00 {
		conn.Close()
		return nil, fmt.Errorf("socks5 auth rejected or unsupported: %v", resp)
	}

	// Step 2: Connection request
	// Split target host and port
	host, portStr, err := net.SplitHostPort(targetAddr)
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("invalid target address: %w", err)
	}
	port, err := strconv.Atoi(portStr)
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("invalid target port: %w", err)
	}

	req := []byte{0x05, 0x01, 0x00} // VER=5, CMD=1 (CONNECT), RSV=0
	ip := net.ParseIP(host)
	if ip4 := ip.To4(); ip4 != nil {
		req = append(req, 0x01) // ATYP = IPv4
		req = append(req, ip4...)
	} else if ip6 := ip.To16(); ip6 != nil {
		req = append(req, 0x04) // ATYP = IPv6
		req = append(req, ip6...)
	} else {
		req = append(req, 0x03) // ATYP = DOMAINNAME
		req = append(req, byte(len(host)))
		req = append(req, []byte(host)...)
	}

	// Port in big endian
	req = append(req, byte(port>>8), byte(port&0xFF))

	if _, err := conn.Write(req); err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to send socks5 connect request: %w", err)
	}

	// Read reply: [VER, REP, RSV, ATYP, BND.ADDR, BND.PORT]
	replyHdr := make([]byte, 4)
	if _, err := io.ReadFull(conn, replyHdr); err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to read socks5 connect reply header: %w", err)
	}

	if replyHdr[1] != 0x00 {
		conn.Close()
		return nil, fmt.Errorf("socks5 connection failed with reply code: 0x%x", replyHdr[1])
	}

	// Drain bound address
	switch replyHdr[3] {
	case 0x01: // IPv4
		bnd := make([]byte, 4+2)
		if _, err := io.ReadFull(conn, bnd); err != nil {
			conn.Close()
			return nil, err
		}
	case 0x04: // IPv6
		bnd := make([]byte, 16+2)
		if _, err := io.ReadFull(conn, bnd); err != nil {
			conn.Close()
			return nil, err
		}
	case 0x03: // Domain
		lenBuf := make([]byte, 1)
		if _, err := io.ReadFull(conn, lenBuf); err != nil {
			conn.Close()
			return nil, err
		}
		bnd := make([]byte, int(lenBuf[0])+2)
		if _, err := io.ReadFull(conn, bnd); err != nil {
			conn.Close()
			return nil, err
		}
	default:
		conn.Close()
		return nil, errors.New("unknown socks5 address type in reply")
	}

	return conn, nil
}
