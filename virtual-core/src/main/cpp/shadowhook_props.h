#pragma once

#include <string>
#include <unordered_map>
#include <mutex>
#include <sys/system_properties.h>

namespace sandboxr {
namespace props {

/**
 * Initializes ShadowHook and hooks libc __system_property_get, __system_property_read,
 * and __system_property_read_callback.
 */
bool init_hooks();

/**
 * Unhooks all property hooks and cleans up ShadowHook resources.
 */
void cleanup_hooks();

/**
 * Sets a spoofed property value for the active virtual environment.
 */
void set_spoofed_property(const std::string& key, const std::string& value);

/**
 * Retrieves the spoofed property value if one exists.
 */
std::string get_spoofed_property(const std::string& key);

/**
 * Clears all registered spoofed property values.
 */
void clear_spoofed_properties();

/**
 * Returns true if ShadowHook property interception is active.
 */
bool is_hook_active();

/**
 * Calls the active libc __system_property_get (verifying that hooks intercept calls).
 */
int test_system_property_get(const char* name, char* value);

} // namespace props
} // namespace sandboxr
