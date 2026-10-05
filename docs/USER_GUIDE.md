# SANDBOXR — Official User Manual & Privacy Platform Guide

**Version:** 1.0.0  
**Target Platform:** Android 10.0+ (API 29–36+)  
**License:** GNU General Public License v3.0 (GPL-3.0)  
**Support & Donations:** [buymemomo.com/ujwal](https://buymemomo.com/ujwal)

---

## 1. Introduction: Zero-Root Userspace Privacy

SANDBOXR brings the core security and isolation paradigms of **GrapheneOS** to any modern Android device (Samsung OneUI, Xiaomi HyperOS, OnePlus OxygenOS, Motorola, Google Pixel) without requiring root, custom recovery, bootloader unlocking, or OEM-restricted Work Profile APIs.

Unlike conventional tools (Shelter, Island, Samsung Secure Folder) that rely on Android's device-wide multi-user subsystem and share a single system package registry, SANDBOXR runs an independent, in-process virtualization engine (`virtual-core`). Each virtual environment has:
- Isolated storage under `/data/data/com.ujwal.sandboxr/envs/{uuid}/`
- Dedicated native Bionic `libc` hooks for hardware spoofing (ShadowHook & ByteHook)
- Independent network stack routing (Direct, SOCKS5, WireGuard, or complete network cutoff)
- Granular Google Play Services (GMS) enable/block control
- Ability to install different versions or signatures of the same app side-by-side without conflict

---

## 2. Setting Up SANDBOXR as Default Home Launcher

SANDBOXR is designed to function as your primary Android Home Launcher, replacing stock OEM launchers:

1. **First Launch:** Launch SANDBOXR from your app drawer.
2. **Set as Default:** Tap the **"Set as Default Launcher"** banner or navigate to **Settings > Apps > Default Apps > Home app** and select **SANDBOXR**.
3. **RoleManager Integration:** On Android 10+, SANDBOXR utilizes Android's modern `RoleManager` API to request the `ROLE_HOME` permission with a single confirmation dialog.
4. **Live Wallpaper & Gestures:** 
   - Supports live system wallpaper backdrops with customizable dimming scrims.
   - Edge-to-edge transparent system navigation and status bars.
   - Long-press anywhere on the home screen wallpaper to access **Wallpaper & style**, **Widgets**, **Home settings**, and **Virtual profiles**.
   - Swipe up from anywhere to reveal the **All Apps Drawer**.
   - Swipe down on the home screen to expand system notifications.

### 2.1 Guided Onboarding & Setup Tour
On initial launch, SANDBOXR presents a 6-step guided setup tour written in plain, friendly language:
- **Step 1: Welcome to SANDBOXR** — Fast, battery-friendly, and completely private with zero ads or tracking.
- **Step 2: Separate Spaces for Your Apps** — Run two copies of apps (like WhatsApp) and keep work/personal files separate.
- **Step 3: Easy Navigation & Switching** — Switch profiles with one tap via drawer tabs (`[Personal]`, `[Work]`, `[+ New Profile]`).
- **Step 4: Built-in Privacy Shield** — Automatically blocks apps from tracking your physical phone across spaces.
- **Step 5: Control How Apps Connect** — Easily choose normal internet, secure VPN/proxy, or completely offline mode.
- **Step 6: Make It Your Default Home** — 1-tap setup as your main home screen, with smooth daily gestures.

*Note: You can revisit this guide anytime by long-pressing the home wallpaper and selecting "Virtual Profiles Guide", or opening "Home Settings > HELP & ONBOARDING > Virtual Profiles Setup Guide".*

---

## 3. Virtual Sandbox Profiles

### 3.1 Creating a New Sandbox Profile
To create a new virtual container:
1. Open the **All Apps Drawer** (swipe up).
2. Tap the **"+ New Profile"** chip on the Profile Tab Bar or open the **Profile Switcher** (avatar pill in the search bar).
3. Configure the container:
   - **Display Name:** e.g., "Work", "Finance", "Social", "Incognito".
   - **Color Tag:** Color used for the profile dot and app icon badges.
   - **Network Mode:** Direct, SOCKS5, WireGuard, or Blocked.
   - **Google Play Services (GMS):** Enable or block Google telemetry.
   - **Clipboard Mode:** Shared, Isolated, or Blocked.
4. Tap **"Create Profile"**. The environment is created instantly with a deterministic, unique hardware spoofing profile.

### 3.2 Switching Between Profiles
SANDBOXR provides the authentic **GrapheneOS Launcher** multi-profile feel:
- **Profile Tab Bar:** Directly at the top of the All Apps drawer, tap between **Personal**, **Work**, and your virtual sandbox tabs (`[ Personal ]  [ Work ]  [ Sandbox A ]  [ + New Profile ]`).
- **Profile Switcher Sheet:** Tap the avatar pill on the search bar or long-press the home screen and select **"Virtual profiles"**.
- **Instant App Filtering:** Selecting a profile instantly scopes the visible apps, dock apps, and desktop shortcuts to that profile without system lag.

### 3.3 Adding Apps to a Sandbox
You can populate your sandboxes in three ways:
1. **Clone an Existing App:** Open the profile in the app drawer. Tap **"Clone App to Sandbox"**, choose from your installed host applications, and tap **"Clone"**.
2. **Install APK Directly:** Tap any `.apk` file in your favorite Android file manager. SANDBOXR's **Installer Chooser** appears, letting you select whether to install into the host OS or into any virtual sandbox.
3. **Staged APK Drop:** Use the in-app APK installer to select any downloaded APK from device storage.

---

## 4. Hardware Identity Spoofing & Anti-Tracking

Every virtual profile receives a distinct, randomized hardware identity profile generated at creation time:
- **IMEI:** Valid 15-digit identifier with Luhn algorithm checksum.
- **Android ID:** Cryptographically randomized 64-bit hexadecimal string.
- **WiFi MAC Address:** Randomized IEEE 802-compliant MAC address.
- **Hardware Serial:** Randomized device serial string.
- **Advertising ID (GAID):** Zeroed or randomized tracking identifier.

### How Spoofing Works:
- **Java Layer:** Android system services (`TelephonyManager`, `WifiManager`, `Settings.Secure`, `Build`) are intercepted via dynamic Binder proxies in `virtual-core`.
- **Native Bionic Layer:** Native C/C++ libraries querying `ro.serialno` or hardware nodes are hooked via **ShadowHook** (inline hooking in `libc.so`) and **ByteHook** (PLT hooking on file descriptors like `/sys/class/net/` and `/proc/cpuinfo`). Apps cannot detect your real hardware identity even when using native inspection libraries.

---

## 5. Local DNS Firewall & Per-Profile Routing

SANDBOXR embeds a headless local network engine based on `RethinkDNS`:
- **Local VpnService:** Runs entirely on-device; zero traffic is sent to external proxy servers unless configured.
- **In-RAM Ad Blocking:** Packed hash set of StevenBlack ad and tracking hostnames evaluated in under 1 millisecond.
- **Per-Profile Dispatch:**
  - **Direct:** Uses host device internet connection with local DNS ad-blocking.
  - **SOCKS5:** Directs socket traffic through a local or remote SOCKS5 proxy server.
  - **WireGuard:** Tunnels container traffic through an embedded WireGuard configuration block.
  - **Blocked:** Drops all outgoing network packets; apps run in complete offline isolation.

---

## 6. Encrypted `.senv` Vault Backup & Migration

You can export any virtual sandbox to an encrypted `.senv` file:
- **Encryption:** AES-256-GCM authenticated cipher.
- **Key Derivation:** PBKDF2 with HMAC-SHA256 (100,000 rounds) using a user-chosen passphrase.
- **Container Structure:** Tarball combining environment metadata, installed APKs, and internal `/data/data/` app storage.
- **Restoration:** Restores the exact environment state on fresh installs or different devices without needing cloud servers.

---

## 7. Performance & Optimization on Low-End Devices

SANDBOXR is optimized to run smoothly on low-end hardware (e.g. 1GB–2GB RAM devices):
- **VirtualAppCache:** In-RAM LRU cache eliminates redundant disk reads and uncompressed Bitmap allocations when browsing apps across many profiles.
- **Concurrent Non-Blocking Loading:** Profiles are queried in parallel via Kotlin coroutines without freezing the main UI thread.
- **Compose Stability:** Lazy grids and rows use stable keys (`"${envId}_${packageName}"`) and immutable data models for zero-jank 120Hz scrolling.
- **Process Freezing:** When switching profiles, background processes belonging to inactive containers can be automatically frozen to conserve device battery and RAM.

---

## 8. Frequently Asked Questions (FAQ)

**Q: Can apps in a sandbox see apps installed on my host phone?**  
A: No. In standard Android, app visibility requires querying `system_server`. Virtual apps talk only to SANDBOXR's internal `VPackageManagerService` which isolates visibility strictly to that sandbox.

**Q: Can two profiles communicate or share clipboard data?**  
A: No. IPC, services, and content providers are scoped by `envId`. Setting Clipboard mode to `ISOLATED` or `BLOCKED` ensures copy-pasted text cannot leak between profiles.

**Q: Why is SANDBOXR not on Google Play?**  
A: Google Play Developer Distribution Agreements prohibit applications that execute dynamic, userspace virtualization engines. SANDBOXR is distributed via **F-Droid** and **GitHub Releases** as 100% Free and Open-Source Software.

---

## 9. Support the Project

SANDBOXR is developed and maintained independently with zero venture funding, zero telemetry, and zero ads.

If you enjoy SANDBOXR, please consider supporting development:
- ☕ **Buy me a momo:** [https://buymemomo.com/ujwal](https://buymemomo.com/ujwal)
- ⭐️ **Star on GitHub:** [https://github.com/ujwal/Sandboxr](https://github.com/ujwal/Sandboxr)
