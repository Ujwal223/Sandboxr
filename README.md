<div align="center">

# SANDBOXR

**Android Userspace Virtualization & Privacy Launcher**

*Inspired by GrapheneOS principles. No Pixel required.*

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android%2010--15-brightgreen?style=flat&logo=android)](https://developer.android.com)
[![No Root Required](https://img.shields.io/badge/Root-Not%20Required-success?style=flat)]()
[![Zero Telemetry](https://img.shields.io/badge/Telemetry-Zero-red?style=flat)]()

</div>

---

## What is SANDBOXR?

SANDBOXR was born from a simple frustration: **GrapheneOS has incredible privacy features — isolated profiles, per-app GMS control, hardware identity separation — but it only runs on Pixel phones.**

SANDBOXR brings those same principles to any Android 10+ device as a userspace application. It does not provide hardware-backed security equivalent to GrapheneOS (it is not an OS), but it faithfully mimics its core privacy model:

- **Separate Profiles** — Each environment is a true isolated container with its own apps, files, identity, and network path.
- **GMS Control** — Toggle Google Play Services on or off per profile. Run one environment with full Google access, another completely GMS-free.
- **Hardware Identity Virtualization** — Each profile presents a completely different device fingerprint (IMEI, Android ID, MAC, GAID, build properties) at the native `libc` level.
- **Profile-Level Network Routing** — Route each profile through a different path: Direct internet, SOCKS5 proxy, WireGuard, or a complete network cutoff.

> **Honest Positioning:** SANDBOXR is not a replacement for a hardened OS. It is a powerful privacy tool for users who want meaningful isolation without buying a new phone or flashing a ROM.

---

## Feature Highlights

| Feature | Details |
|---|---|
| **Rootless Containerization** | Unlimited isolated environments — no root, no bootloader unlock, no ADB setup |
| **Hardware Identity Spoofing** | Per-profile IMEI, Android ID, Serial, MAC, GAID, Model, Manufacturer, Build props |
| **Dual-Layer Native Hooks** | ShadowHook (inline) + ByteHook (PLT) intercept `libc` at native level |
| **Local DNS & Ad Firewall** | Go-based tun2socks engine with StevenBlack blocklist; <1ms evaluation in RAM |
| **Per-Profile Network Routing** | Direct / SOCKS5 / WireGuard / Blocked, independently per environment |
| **Signature Independence** | Run Modded APK in Profile 1 and Official APK in Profile 2 — zero conflicts |
| **GMS Toggle** | Enable or completely disable Google Play Services per profile |
| **Encrypted Vault Export** | `.senv` containers using AES-256-GCM + PBKDF2 (100k rounds) |
| **Liquid Glass Launcher** | Jetpack Compose UI with dark-first aesthetic, blur, and environment card carousel |
| **Zero Telemetry** | 100% FOSS. No analytics, no crash trackers, no proprietary binaries |

---

## How SANDBOXR Differs from Work Profiles

> Samsung Secure Folder, Shelter, and Island all use Android's native **Work Profile** subsystem. They share the host OS package registry and cannot avoid signature conflicts, hardware ID leakage, or system-wide VPN limitations.

| Capability | Work Profile (Shelter/Island) | Samsung Secure Folder | SANDBOXR |
|---|---|---|---|
| Simultaneous signature support | ❌ Blocked | ❌ Blocked | ✅ Full (per-env) |
| Per-environment VPN / Proxy | ❌ One system VPN | ❌ One system VPN | ✅ Per-profile routing |
| Hardware ID spoofing | ❌ None | ❌ None | ✅ Native libc level |
| GMS per-profile control | ⚠️ Cloned from host | ❌ Forced on | ✅ Toggle per profile |
| Requires OEM / Knox support | No | Yes (Samsung only) | No (Universal Android 10–16+) |
| At-rest encrypted vault export | ❌ Manual backup | ⚠️ Samsung Cloud | ✅ Native `.senv` AES-256-GCM |
| Requires device admin or root | Device Owner required | System-level | ✅ Zero setup |

---

## Architecture

SANDBOXR is structured as four decoupled layers:

```
┌──────────────────────────────────────────────────────────────────┐
│  Layer 1: Presentation & Launcher UI (Kotlin / Jetpack Compose)  │
│  · Environment Carousel  · Per-Environment App Drawer            │
│  · APK Staging Handler   · Environment Chooser Activity          │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  Layer 2: Virtual Core Runtime (Kotlin / Java)                   │
│  · DexClassLoader Isolated Runtime                               │
│  · Binder Dynamic Proxies (~12 System Services: AM, PM, Telephony)│
│  · Namespaced Storage: /envs/{id}/data/{package}                 │
│  · Google Play Services Stub & Interception (GmsInterceptor)     │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  Layer 3: Native Interception Engine (C/C++ / Bionic)            │
│  · ShadowHook: Inline hooks on __system_property_get             │
│  · ByteHook: PLT hooks on open / openat / stat / readlinkat      │
│  · Android 15/16 16KB Page-Size Compliance                       │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  Layer 4: Network & DNS Stack (Go / Kotlin VpnService)           │
│  · Headless Go Tun2Socks Engine (TUN Interface)                  │
│  · In-RAM Packed HashSet DNS Filter (<1ms per query)             │
│  · Per-Environment Socket Dispatch                               │
│  · Host Self-Exclusion Rules (prevents traffic loops)            │
└──────────────────────────────────────────────────────────────────┘
```

### Module Breakdown

| Module | Responsibility |
|---|---|
| [`app`](app/) | Environment manager, settings UI, APK staging, encrypted vault engine, lifecycle monitors |
| [`launcher`](launcher/) | Jetpack Compose launcher: home screen, environment carousel, gestures, per-profile app grids |
| [`virtual-core`](virtual-core/) | Binder proxies, storage redirection, hidden API bypass, native C++ hooks |
| [`network`](network/) | Go tun2socks engine, Kotlin VpnService, DNS ad blocking, per-environment proxy routing |
| [`aidl`](aidl/) | Clean AIDL IPC definitions for container service interfaces |

---

## Security & Isolation Model

### What SANDBOXR Provides

1. **Storage Isolation** — All file operations are validated against the environment's canonical path. Deepest-ancestor resolution blocks symlink traversal and sandbox escapes.
2. **Cross-Profile IPC Blocking** — Intents, service bindings, and content provider resolutions are scoped strictly by `envId` via `VActivityManagerService`. Profile 1 cannot broadcast to Profile 2.
3. **Signature Independence** — Packages are indexed as `"$envId:$packageName"` in an in-process registry, completely decoupled from the host OS `PackageManagerService`.
4. **Encrypted Vault at Rest** — `.senv` exports use AES-256-GCM with PBKDF2-HMAC-SHA256 (100,000 iterations). The decryption stream is fully read to EOF before any file is written, validating the 16-byte GCM auth tag.
5. **Clipboard Isolation** — Configurable `ClipboardMode` (`ISOLATED` or `BLOCKED`) prevents copy-paste data from leaking between profiles.
6. **Scoped File Sharing** — `FileProvider` paths are strictly restricted to staging directories; `file://` URIs are blocked.

### Honest Limitations

| Threat | SANDBOXR Defense | Realistic Resilience |
|---|---|---|
| Standard app fingerprinting (Java APIs) | Binder proxies + ShadowHook | ✅ High — consumer and social apps fully deceived |
| Native `getprop` / `libc` queries | ShadowHook inline + ByteHook PLT | ✅ High — spoofed at the Bionic level |
| `/proc/self/maps` inspection | ByteHook filters process maps | ⚠️ Medium-High — raw `svc` syscalls can bypass |
| Hardware-backed Play Integrity (STRONG) | GmsInterceptor stubs `MEETS_BASIC_INTEGRITY` | ❌ Fundamental OS limit — TEE cannot be spoofed |
| Host keyboard / IME | User advisory | ⚠️ User responsibility — use open-source IME |

> SANDBOXR is **not a hardened OS**. It does not provide the hardware root-of-trust or kernel-level isolation that GrapheneOS achieves. Banking apps enforcing `MEETS_STRONG_INTEGRITY` will detect the virtualization environment.

---

## Technical Specifications

| Parameter | Value |
|---|---|
| Target Android Versions | Android 10 – 15 (API 29 – 35) |
| Architecture Support | `arm64-v8a`, `armeabi-v7a` |
| Page Size Support | 4KB and 16KB (Android 15+ compatible) |
| Language Stack | Kotlin 2.0+, Java 21, C++20, Go 1.22+ |
| UI Framework | Jetpack Compose, Material Design 3, Liquid Glass |
| Build System | Gradle 8.9+, AGP 8.9+, CMake 3.22+, NDK r28 |
| Version | 1.0.0 |
| License | GNU General Public License v3.0 |

---

## Building from Source

### Prerequisites

| Tool | Required Version |
|---|---|
| JDK | 21 (OpenJDK 21 recommended) |
| Android SDK | Platform API 35, Build-Tools 35.0.0 |
| Android NDK | r28 (`28.0.12433566`) |
| CMake | 3.22.1+ |
| Go | 1.22+ (for native network components) |
| Git | 2.40+ |

### Clone

```bash
git clone https://github.com/Ujwal223/Sandboxr.git
cd Sandboxr/sandboxr
```

### Run Tests

```bash
./gradlew testDebugUnitTest
```

### Build Debug APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Environment Configuration

Copy `.env.example` to `.env` and fill in the required values:

```bash
ANDROID_HOME=/opt/android-sdk       # Path to Android SDK
ANDROID_NDK_VERSION=28.2.13676358   # NDK r28
JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

For release signing and Pro companion integration, see [`.env.example`](../.env.example) for the full configuration reference.

---

## Distribution

SANDBOXR is distributed through:

- **F-Droid** — Available via official and custom F-Droid repositories
- **GitHub Releases** — Signed APK binaries and source archives

> SANDBOXR is **not** published on the Google Play Store. Google Play policies prohibit userspace virtualization engines and dynamic code execution, which are core to how SANDBOXR works.

---

## Open Source Foundations

SANDBOXR builds on the work of these open-source projects:

| Project | Role | License |
|---|---|---|
| [Lawnchair](https://github.com/LawnchairLauncher/lawnchair) | Modern launcher foundation | GPL-3.0 |
| [NewBlackBox](https://github.com/ALEX5402/NewBlackbox) / [BlackBox](https://github.com/FBlackBox/BlackBox) | Userspace virtualization framework | Apache-2.0 |
| [RethinkDNS](https://github.com/celzero/rethink-app) | Network stack & tun2socks engine | Apache-2.0 |
| [ShadowHook](https://github.com/bytedance/android-inline-hook) | Android inline hooking (ARM/ARM64) | MIT |
| [ByteHook](https://github.com/bytedance/bhook) | Android PLT hook library | MIT |
| [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) | Hidden API access without root | Apache-2.0 |
| [StevenBlack/hosts](https://github.com/StevenBlack/hosts) | Unified ad & tracker DNS blocklist | MIT |

---

## Contributing

Contributions are welcome. Please follow these guidelines:

1. **Code Quality** — Kotlin and Java code must pass `./gradlew testDebugUnitTest` before submission.
2. **Architecture Boundaries** — Maintain strict decoupling between `launcher` (UI), `virtual-core` (virtualization), and `network` (routing). Cross-module dependencies must go through `aidl`.
3. **Security First** — Any change affecting filesystem paths, IPC proxies, or native hooks requires automated regression tests validating container boundary enforcement.
4. **Clean Licensing** — All contributions must comply with GPL-3.0. No proprietary binaries or untracked native dependencies.

---

## Support

SANDBOXR is completely free and open-source, developed and maintained independently.

If you find SANDBOXR useful, consider supporting ongoing development:

- ☕ [Buy me a momo](https://buymemomo.com/ujwal)

---

## License

```
SANDBOXR — Android Userspace Virtualization & Privacy Launcher
Copyright (C) 2026 Ujwal

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
