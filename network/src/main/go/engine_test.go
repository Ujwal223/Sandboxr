package firestack

import (
	"encoding/binary"
	"testing"
	"time"
)

func TestDnsBlocklistSubMillisecondMatching(t *testing.T) {
	bl := NewDnsBlocklist()
	domains := []string{
		"doubleclick.net",
		"google-analytics.com",
		"telemetry.app.com",
		"ads.tracker.org",
	}
	bl.LoadDomains(domains)

	if bl.Count() != 4 {
		t.Fatalf("expected 4 domains, got %d", bl.Count())
	}

	// Benchmark single lookup time to ensure sub-millisecond requirement
	start := time.Now()
	blocked := bl.IsBlocked("ad.doubleclick.net")
	elapsed := time.Since(start)

	if !blocked {
		t.Fatalf("expected ad.doubleclick.net to be blocked by doubleclick.net")
	}

	if elapsed > time.Millisecond {
		t.Errorf("lookup took %v, expected < 1ms", elapsed)
	}

	// Verify exact and negative cases
	if !bl.IsBlocked("google-analytics.com") {
		t.Errorf("expected google-analytics.com to be blocked")
	}
	if bl.IsBlocked("wikipedia.org") {
		t.Errorf("expected wikipedia.org to be allowed")
	}
}

func TestDnsParseAndSynthesizeResponse(t *testing.T) {
	// Construct a raw DNS query for "example.com" Type A (1)
	query := make([]byte, 0, 32)
	// Header: ID=0x1234, Flags=0x0100 (standard query, recursion desired), QDCOUNT=1, ANCOUNT=0, NSCOUNT=0, ARCOUNT=0
	hdr := make([]byte, 12)
	binary.BigEndian.PutUint16(hdr[0:2], 0x1234)
	binary.BigEndian.PutUint16(hdr[2:4], 0x0100)
	binary.BigEndian.PutUint16(hdr[4:6], 1)
	query = append(query, hdr...)

	// Question: \x07example\x03com\x00, QTYPE=1, QCLASS=1
	query = append(query, 7)
	query = append(query, []byte("example")...)
	query = append(query, 3)
	query = append(query, []byte("com")...)
	query = append(query, 0)

	qTail := make([]byte, 4)
	binary.BigEndian.PutUint16(qTail[0:2], TypeA)
	binary.BigEndian.PutUint16(qTail[2:4], ClassINET)
	query = append(query, qTail...)

	header, question, err := ParseDnsQuery(query)
	if err != nil {
		t.Fatalf("failed to parse dns query: %v", err)
	}

	if header.ID != 0x1234 {
		t.Errorf("expected ID 0x1234, got 0x%x", header.ID)
	}
	if question.Name != "example.com" {
		t.Errorf("expected example.com, got %s", question.Name)
	}
	if question.Type != TypeA {
		t.Errorf("expected TypeA, got %d", question.Type)
	}

	// Build blocked response with 0.0.0.0
	resp := BuildBlockedResponse(query, header, question, false)
	if len(resp) < len(query)+12 {
		t.Fatalf("response too short: %d bytes", len(resp))
	}

	// Verify response header
	respID := binary.BigEndian.Uint16(resp[0:2])
	respFlags := binary.BigEndian.Uint16(resp[2:4])
	respAnCount := binary.BigEndian.Uint16(resp[6:8])

	if respID != 0x1234 {
		t.Errorf("expected respID 0x1234, got 0x%x", respID)
	}
	if respFlags != 0x8180 {
		t.Errorf("expected flags 0x8180, got 0x%x", respFlags)
	}
	if respAnCount != 1 {
		t.Errorf("expected 1 answer, got %d", respAnCount)
	}
}

func TestRoutingTable(t *testing.T) {
	rt := NewRoutingTable()

	rt.SetRoute(&EnvRoute{
		EnvID:     "env-uuid-1",
		Mode:      ModeDirect,
		AdBlockOn: true,
	})

	rt.SetRoute(&EnvRoute{
		EnvID:     "env-uuid-2",
		Mode:      ModeSocks5,
		ProxyHost: "127.0.0.1",
		ProxyPort: 9050,
	})

	rt.SetRoute(&EnvRoute{
		EnvID: "env-uuid-3",
		Mode:  ModeBlocked,
	})

	if rt.Count() != 3 {
		t.Errorf("expected 3 routes, got %d", rt.Count())
	}

	r2, ok := rt.GetRoute("env-uuid-2")
	if !ok || r2.Mode != ModeSocks5 || r2.ProxyPort != 9050 {
		t.Errorf("route 2 mismatch: %+v", r2)
	}

	rt.RemoveRoute("env-uuid-1")
	if rt.Count() != 2 {
		t.Errorf("expected 2 routes after removal, got %d", rt.Count())
	}
}

func TestParseWireGuardConfig(t *testing.T) {
	conf := `
[Interface]
PrivateKey = aaaaaabbbbbbccccccddddddeeeeeeffffff1111=
Address = 10.0.0.2/32, fd00::2/128
DNS = 1.1.1.1
MTU = 1420

[Peer]
PublicKey = 1111112222223333334444445555556666667777=
Endpoint = 198.51.100.1:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
`

	cfg, err := ParseWireGuardConfig(conf)
	if err != nil {
		t.Fatalf("failed to parse wireguard config: %v", err)
	}

	if cfg.Address != "10.0.0.2/32, fd00::2/128" {
		t.Errorf("expected address match, got: %s", cfg.Address)
	}
	if cfg.Endpoint != "198.51.100.1:51820" {
		t.Errorf("expected endpoint match, got: %s", cfg.Endpoint)
	}
	if cfg.PersistentKeepalive != 25 {
		t.Errorf("expected keepalive 25, got: %d", cfg.PersistentKeepalive)
	}
	if cfg.MTU != 1420 {
		t.Errorf("expected MTU 1420, got: %d", cfg.MTU)
	}
}

func TestEngineLifecycleAndStats(t *testing.T) {
	engine := NewEngine()

	if engine.IsRunning() {
		t.Errorf("engine should not be running initially")
	}

	// Invalid tunFd should fail cleanly
	err := engine.Start(-1, 1500)
	if err == nil {
		t.Errorf("expected error starting engine with invalid fd")
	}

	stats := engine.GetStats()
	if stats.TotalPacketsReceived != 0 {
		t.Errorf("expected 0 packets initially, got %d", stats.TotalPacketsReceived)
	}
}
