package firestack

import (
	"fmt"
	"sync"
)

// NetworkMode defines the per-environment routing behavior
type NetworkMode int

const (
	ModeDirect    NetworkMode = 0
	ModeSocks5    NetworkMode = 1
	ModeWireGuard NetworkMode = 2
	ModeBlocked   NetworkMode = 3
)

func (m NetworkMode) String() string {
	switch m {
	case ModeDirect:
		return "DIRECT"
	case ModeSocks5:
		return "SOCKS5"
	case ModeWireGuard:
		return "WIREGUARD"
	case ModeBlocked:
		return "BLOCKED"
	default:
		return fmt.Sprintf("UNKNOWN(%d)", m)
	}
}

// EnvRoute represents configuration for a specific virtual environment
type EnvRoute struct {
	EnvID        string
	Mode         NetworkMode
	ProxyHost    string
	ProxyPort    int
	WgConfig     string
	DnsUpstream  string
	AdBlockOn    bool
}

// RoutingTable manages per-environment route configurations in memory
type RoutingTable struct {
	mu     sync.RWMutex
	routes map[string]*EnvRoute
}

// NewRoutingTable creates an initialized routing table
func NewRoutingTable() *RoutingTable {
	return &RoutingTable{
		routes: make(map[string]*EnvRoute),
	}
}

// SetRoute stores or updates an environment route
func (r *RoutingTable) SetRoute(route *EnvRoute) {
	if route == nil || route.EnvID == "" {
		return
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	r.routes[route.EnvID] = route
}

// GetRoute retrieves an environment's route configuration
func (r *RoutingTable) GetRoute(envID string) (*EnvRoute, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	route, ok := r.routes[envID]
	return route, ok
}

// RemoveRoute removes route when an environment is deleted
func (r *RoutingTable) RemoveRoute(envID string) {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.routes, envID)
}

// Count returns total configured routes
func (r *RoutingTable) Count() int {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return len(r.routes)
}
