package firestack

import (
	"encoding/binary"
	"errors"
	"net"
	"strings"
	"sync"
)

// DNS Record Types & Codes
const (
	TypeA     uint16 = 1
	TypeNS    uint16 = 2
	TypeCNAME uint16 = 5
	TypeSOA   uint16 = 6
	TypePTR   uint16 = 12
	TypeMX    uint16 = 15
	TypeTXT   uint16 = 16
	TypeAAAA  uint16 = 28
	TypeSRV   uint16 = 33
	TypeANY   uint16 = 255

	ClassINET uint16 = 1

	// DNS RCODEs
	RCodeNoError  uint8 = 0
	RCodeFormat   uint8 = 1
	RCodeServer   uint8 = 2
	RCodeNXDomain uint8 = 3
	RCodeRefused  uint8 = 5
)

// DnsHeader represents standard 12-byte DNS wire format header
type DnsHeader struct {
	ID      uint16
	Flags   uint16
	QDCount uint16
	ANCount uint16
	NSCount uint16
	ARCount uint16
}

// DnsQuestion represents a single question in DNS packet
type DnsQuestion struct {
	Name  string
	Type  uint16
	Class uint16
}

// DnsBlocklist holds in-memory domain blocklist for sub-millisecond matching
type DnsBlocklist struct {
	domains map[string]struct{}
	mu      sync.RWMutex
}

// NewDnsBlocklist creates a new empty thread-safe blocklist
func NewDnsBlocklist() *DnsBlocklist {
	return &DnsBlocklist{
		domains: make(map[string]struct{}),
	}
}

// LoadDomains populates the blocklist with domain names
func (b *DnsBlocklist) LoadDomains(domainList []string) {
	b.mu.Lock()
	defer b.mu.Unlock()

	for _, d := range domainList {
		norm := strings.ToLower(strings.TrimSpace(d))
		if norm != "" && !strings.HasPrefix(norm, "#") {
			b.domains[norm] = struct{}{}
		}
	}
}

// AddDomain adds a single domain to blocklist
func (b *DnsBlocklist) AddDomain(domain string) {
	norm := strings.ToLower(strings.TrimSpace(domain))
	if norm == "" {
		return
	}
	b.mu.Lock()
	defer b.mu.Unlock()
	b.domains[norm] = struct{}{}
}

// IsBlocked checks if a domain or any of its parent domains are blocked
func (b *DnsBlocklist) IsBlocked(domain string) bool {
	norm := strings.ToLower(strings.Trim(domain, "."))
	if norm == "" {
		return false
	}

	b.mu.RLock()
	defer b.mu.RUnlock()

	// Exact match
	if _, ok := b.domains[norm]; ok {
		return true
	}

	// Subdomain match: e.g. "ad.tracker.google.com" checks "tracker.google.com", "google.com"
	parts := strings.Split(norm, ".")
	for i := 1; i < len(parts)-1; i++ {
		parent := strings.Join(parts[i:], ".")
		if _, ok := b.domains[parent]; ok {
			return true
		}
	}

	return false
}

// Count returns total number of blocked domains
func (b *DnsBlocklist) Count() int {
	b.mu.RLock()
	defer b.mu.RUnlock()
	return len(b.domains)
}

// ParseDnsQuery parses raw DNS wire packet into header and question
func ParseDnsQuery(payload []byte) (*DnsHeader, *DnsQuestion, error) {
	if len(payload) < 12 {
		return nil, nil, errors.New("dns packet too short")
	}

	header := &DnsHeader{
		ID:      binary.BigEndian.Uint16(payload[0:2]),
		Flags:   binary.BigEndian.Uint16(payload[2:4]),
		QDCount: binary.BigEndian.Uint16(payload[4:6]),
		ANCount: binary.BigEndian.Uint16(payload[6:8]),
		NSCount: binary.BigEndian.Uint16(payload[8:10]),
		ARCount: binary.BigEndian.Uint16(payload[10:12]),
	}

	if header.QDCount == 0 {
		return header, nil, errors.New("no question in dns packet")
	}

	offset := 12
	name, nextOffset, err := parseDomainName(payload, offset)
	if err != nil {
		return header, nil, err
	}

	if len(payload) < nextOffset+4 {
		return header, nil, errors.New("incomplete dns question")
	}

	qType := binary.BigEndian.Uint16(payload[nextOffset : nextOffset+2])
	qClass := binary.BigEndian.Uint16(payload[nextOffset+2 : nextOffset+4])

	question := &DnsQuestion{
		Name:  name,
		Type:  qType,
		Class: qClass,
	}

	return header, question, nil
}

// parseDomainName extracts dot-separated domain name from DNS packet
func parseDomainName(payload []byte, offset int) (string, int, error) {
	var parts []string
	curr := offset

	for {
		if curr >= len(payload) {
			return "", 0, errors.New("unexpected end of dns labels")
		}

		length := int(payload[curr])
		if length == 0 {
			curr++
			break
		}

		// Pointer (compression) check (0xC0)
		if length&0xC0 == 0xC0 {
			if curr+1 >= len(payload) {
				return "", 0, errors.New("incomplete compression pointer")
			}
			ptrOffset := int(binary.BigEndian.Uint16(payload[curr:curr+2]) & 0x3FFF)
			curr += 2
			subName, _, err := parseDomainName(payload, ptrOffset)
			if err != nil {
				return "", 0, err
			}
			parts = append(parts, subName)
			return strings.Join(parts, "."), curr, nil
		}

		curr++
		if curr+length > len(payload) {
			return "", 0, errors.New("label extends beyond packet boundary")
		}

		parts = append(parts, string(payload[curr:curr+length]))
		curr += length
	}

	return strings.Join(parts, "."), curr, nil
}

// BuildBlockedResponse constructs a synthesized DNS response with 0.0.0.0 or NXDOMAIN
func BuildBlockedResponse(reqPayload []byte, header *DnsHeader, question *DnsQuestion, nxdomain bool) []byte {
	resp := make([]byte, 0, len(reqPayload)+16)

	// Response ID matches query ID
	flags := uint16(0x8180) // Standard query response, recursion desired, recursion available
	if nxdomain {
		flags = 0x8183 // NXDOMAIN
	}

	anCount := uint16(0)
	if !nxdomain && (question.Type == TypeA || question.Type == TypeAAAA) {
		anCount = 1
	}

	// 12-byte header
	hdr := make([]byte, 12)
	binary.BigEndian.PutUint16(hdr[0:2], header.ID)
	binary.BigEndian.PutUint16(hdr[2:4], flags)
	binary.BigEndian.PutUint16(hdr[4:6], header.QDCount)
	binary.BigEndian.PutUint16(hdr[6:8], anCount)
	binary.BigEndian.PutUint16(hdr[8:10], 0)
	binary.BigEndian.PutUint16(hdr[10:12], 0)
	resp = append(resp, hdr...)

	// Append Question Section verbatim from request
	questionBytes := reqPayload[12:]
	nameLen := 0
	for i := 12; i < len(reqPayload); i++ {
		if reqPayload[i] == 0 {
			nameLen = i - 12 + 1
			break
		}
	}
	if nameLen > 0 && len(questionBytes) >= nameLen+4 {
		resp = append(resp, questionBytes[:nameLen+4]...)
	}

	// If returning 0.0.0.0 or :: answer
	if !nxdomain && anCount == 1 {
		// Pointer to question name (0xC00C)
		resp = append(resp, 0xC0, 0x0C)

		if question.Type == TypeA {
			// Type A, Class IN, TTL 300, RdLength 4, IP 0.0.0.0
			answer := make([]byte, 14)
			binary.BigEndian.PutUint16(answer[0:2], TypeA)
			binary.BigEndian.PutUint16(answer[2:4], ClassINET)
			binary.BigEndian.PutUint32(answer[4:8], 300) // TTL 300s
			binary.BigEndian.PutUint16(answer[8:10], 4)  // IPv4 length
			copy(answer[10:14], net.IPv4zero.To4())
			resp = append(resp, answer...)
		} else if question.Type == TypeAAAA {
			// Type AAAA, Class IN, TTL 300, RdLength 16, IP ::
			answer := make([]byte, 26)
			binary.BigEndian.PutUint16(answer[0:2], TypeAAAA)
			binary.BigEndian.PutUint16(answer[2:4], ClassINET)
			binary.BigEndian.PutUint32(answer[4:8], 300)
			binary.BigEndian.PutUint16(answer[8:10], 16)
			copy(answer[10:26], net.IPv6zero)
			resp = append(resp, answer...)
		}
	}

	return resp
}
