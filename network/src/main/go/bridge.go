package firestack

import (
	"sync"
)

var (
	globalEngine   *Engine
	globalEngineMu sync.Mutex
)

// GetGlobalEngine returns or instantiates the singleton firestack engine
func GetGlobalEngine() *Engine {
	globalEngineMu.Lock()
	defer globalEngineMu.Unlock()
	if globalEngine == nil {
		globalEngine = NewEngine()
	}
	return globalEngine
}

// StartFirestack initializes and starts the global headless engine on tunFd
func StartFirestack(tunFd int, mtu int) error {
	return GetGlobalEngine().Start(tunFd, mtu)
}

// StopFirestack halts the global headless engine
func StopFirestack() error {
	return GetGlobalEngine().Stop()
}

// IsFirestackRunning returns whether the engine is active
func IsFirestackRunning() bool {
	return GetGlobalEngine().IsRunning()
}

// LoadBlocklist adds a batch of blocked domains into the in-RAM blocklist
func LoadBlocklist(domains []string) {
	GetGlobalEngine().Blocklist().LoadDomains(domains)
}

// IsDomainBlocked checks whether a domain is present in the blocklist
func IsDomainBlocked(domain string) bool {
	return GetGlobalEngine().Blocklist().IsBlocked(domain)
}

// SetEnvRoute configures the network mode and proxy/vpn parameters for an environment
func SetEnvRoute(envID string, mode int, proxyHost string, proxyPort int, wgConfig string, dnsUpstream string, adBlockOn bool) {
	route := &EnvRoute{
		EnvID:       envID,
		Mode:        NetworkMode(mode),
		ProxyHost:   proxyHost,
		ProxyPort:   proxyPort,
		WgConfig:    wgConfig,
		DnsUpstream: dnsUpstream,
		AdBlockOn:   adBlockOn,
	}
	GetGlobalEngine().Routes().SetRoute(route)
}

// RemoveEnvRoute removes the route configuration for an environment
func RemoveEnvRoute(envID string) {
	GetGlobalEngine().Routes().RemoveRoute(envID)
}

// GetTelemetryStats returns formatted telemetry numbers from the global engine
func GetTelemetryStats() EngineStats {
	return GetGlobalEngine().GetStats()
}
