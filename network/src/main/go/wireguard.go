package firestack

import (
	"bufio"
	"fmt"
	"strings"
)

// WireGuardConfig holds parsed wg0.conf parameters
type WireGuardConfig struct {
	// Interface
	PrivateKey string
	Address    string
	DNS        string
	MTU        int
	ListenPort int

	// Peer
	PublicKey           string
	Endpoint            string
	AllowedIPs          string
	PersistentKeepalive int
	PresharedKey        string
}

// ParseWireGuardConfig parses a standard wg0.conf file into a WireGuardConfig struct
func ParseWireGuardConfig(content string) (*WireGuardConfig, error) {
	cfg := &WireGuardConfig{
		MTU: 1420, // Standard WireGuard MTU
	}

	scanner := bufio.NewScanner(strings.NewReader(content))
	section := ""

	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		if strings.HasPrefix(line, "[") && strings.HasSuffix(line, "]") {
			section = strings.ToLower(line[1 : len(line)-1])
			continue
		}

		parts := strings.SplitN(line, "=", 2)
		if len(parts) != 2 {
			continue
		}

		key := strings.ToLower(strings.TrimSpace(parts[0]))
		val := strings.TrimSpace(parts[1])

		switch section {
		case "interface":
			switch key {
			case "privatekey":
				cfg.PrivateKey = val
			case "address":
				cfg.Address = val
			case "dns":
				cfg.DNS = val
			case "mtu":
				var mtu int
				if _, err := fmt.Sscanf(val, "%d", &mtu); err == nil && mtu > 0 {
					cfg.MTU = mtu
				}
			case "listenport":
				var port int
				if _, err := fmt.Sscanf(val, "%d", &port); err == nil && port > 0 {
					cfg.ListenPort = port
				}
			}

		case "peer":
			switch key {
			case "publickey":
				cfg.PublicKey = val
			case "endpoint":
				cfg.Endpoint = val
			case "allowedips":
				cfg.AllowedIPs = val
			case "presharedkey":
				cfg.PresharedKey = val
			case "persistentkeepalive":
				var ka int
				if _, err := fmt.Sscanf(val, "%d", &ka); err == nil && ka > 0 {
					cfg.PersistentKeepalive = ka
				}
			}
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("error reading wireguard config: %w", err)
	}

	if cfg.PrivateKey == "" && cfg.PublicKey == "" {
		return nil, fmt.Errorf("invalid wireguard configuration: missing keys")
	}

	return cfg, nil
}
