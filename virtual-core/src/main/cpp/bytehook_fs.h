#pragma once

#include <string>
#include <mutex>

namespace sandboxr {
namespace fs {

/**
 * Initializes ByteHook and hooks open, openat, stat, and readlink
 * to intercept /sys/class/net/ and /dev/socket/.
 */
bool init_hooks();

/**
 * Cleans up all ByteHook stubs.
 */
void cleanup_hooks();

/**
 * Configures the spoofed WiFi MAC address for the active environment.
 */
void set_spoofed_mac_address(const std::string& mac_address);

/**
 * Retrieves the spoofed WiFi MAC address.
 */
std::string get_spoofed_mac_address();

/**
 * Configures the redirect directory where spoofed sysfs virtual files reside.
 */
void set_virtual_sysfs_dir(const std::string& path);

/**
 * Returns true if ByteHook filesystem interception is active.
 */
bool is_hook_active();

/**
 * Native test utility: executes raw open() and read() on a path to verify PLT interception.
 */
std::string test_read_file(const std::string& path);

} // namespace fs
} // namespace sandboxr
