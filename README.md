# SwiftSense

SwiftSense is an advanced, privacy-first Android system tuning and optimization toolkit. It enables deep hardware-level tweaks, display optimizations, and package management without root—either via its **built-in Wireless Debugging daemon** or through [Shizuku](https://shizuku.rikka.app/).

---

## Features

### Material 3 Design & Dynamic Theming
- **Expressive M3 Interface:** Clean, modern, and cohesive Material 3 design across all modules.
- **Dynamic & Custom Theming:** Android 12+ wallpaper-based dynamic color matching, plus dedicated accent tones (Pistachio Green, Ocean Blue, Violet Purple, Amber Orange) in light and dark modes.

### Standalone Wireless Debugging & Shizuku
- **Built-in ADB Daemon:** Direct local Wireless Debugging connection without requiring third-party tools.
- **Flexible Shell Bridge:** Seamlessly toggle between SwiftSense's internal ADB daemon and Shizuku according to your preference.

### Package & APK Installer
- **Multi-Format Support:** Fast, native installation for `.apk`, `.xapk`, `.apks`, and `.apkm` packages.
- **Power-User Flags:** Reinstall (`-r`), allow downgrade (`-d`), allow test-only builds (`-t`), grant runtime permissions (`-g`), and bypass verification checks.
- **Certificate Inspection & Multi-User:** View package signature SHA-256 fingerprint, requested permissions, and install across specific user profiles or all accounts simultaneously.

### In-App Releases & Update Center
- **GitHub Release Explorer:** Directly fetch and read SwiftSense changelogs and release notes with full Markdown rendering.
- **One-Tap APK Download:** Download the latest `app-release.apk` with live progress tracking and trigger the installation handoff instantly.

### Multi-User & Second Space Support
- **Dynamic Device Profile Discovery:** Automatically scans device user profiles (`Owner`, `Work`, `Dual Apps`, `Second Space`) with actual profile labels.
- **Interactive Profile Switcher:** Easily target actions in App Manager and App Stopper to specific user profiles.

### AMOLED Screen Protection
- **Subpixel Mesh Filters:** Protects OLED/AMOLED panels against burn-in using physically aligned micro-mesh patterns (Diamond PenTile, Dynamic Phase Inversion, Blue Subpixel Shield, Checker Matrix, Micro Dots).
- **Pixel Tint & White Attenuation:** Softens harsh white and high-energy blue subpixels with customizable color overlays (Amber, Red, Sepia, Dimmer, or custom hex).
- **Dynamic Shifting:** Continuous or periodic micro-movements to equalize diode wear.
- **Custom Viewports:** Apply protection across the entire screen, status bar, navigation bar, or a specific vertical area.

### Always-On Display (Beta)
- Lightweight lock screen overlay displaying time, date, battery status, and notifications.
- Built-in burn-in protection modes (Smooth Bounce, Random Jump, RGB Shift).
- Custom battery indicator styles with live charging speed (wattage / mA).
- Double-tap and power button wake integrations.

### Background App Stopper
- Automated background service to periodically or reactively stop selected background apps when launched.
- Quick Settings tile for instant batch force-stop or monitoring toggle.
- Search and category filters (User, System, Active) with safe exclusions for critical services.

### App Manager
- Package inspection sheet with UID, install paths, target SDK, and status flags.
- Fast batch enable/disable and freeze operations powered by privileged shell commands.

### Sensor Boost
- Unlocks sensor sampling rates (gyroscope, accelerometer) to their maximum hardware limits for lower input latency.
- Real-time sensor frequency monitor (Hz).

### Screen Resolution & DPI Tuner
- Adjust display resolution and density on the fly.
- Save and switch between custom profiles (e.g. Performance vs. Battery Saving).

### System Table Macros
- View and modify Android `System`, `Secure`, and `Global` settings tables.
- Create, run, export, and import multi-setting tweak macros via JSON.
- High-frequency loop intervals down to 500ms (500ms to 10 minutes).

### Screen Recorder
- Hardware-accelerated (HEVC/AVC) screen recorder with minimal overhead.
- Supports internal audio capture and customizable resolution, framerate, and bitrate (up to 1080p, 60 FPS).
- Includes Quick Settings tile for quick capture.

### Privacy Quick Settings Tiles
- **Camera Access Tile:** Instantly toggle global camera access on or off via privileged shell (`cmd sensor_privacy`).
- **Microphone Access Tile:** Instantly toggle global microphone access on or off via privileged shell (`cmd sensor_privacy`).
- **Persistent Local State:** Retains the toggle state seamlessly across sessions using local DataStore storage.

---

## Feature Requirements

| Feature | Requirement | Purpose |
| :--- | :--- | :--- |
| **Material 3 Theming** | Android 8.0+ *(Dynamic colors Android 12+)* | Visual customization & wallpaper palettes |
| **Always-On Display** | Android 8.0+ | Lock screen overlay with burn-in protection |
| **AMOLED Screen Protect** | Android 8.0+ | Subpixel anti-burn-in overlay & color tint |
| **Sensor Boost** | Android 8.0+ | Maximum hardware rate sensor sampling |
| **Screen Recorder** | Android 10+ | Internal audio & hardware screen capture |
| **In-App Releases & Updates** | Internet permission | GitHub API check & APK downloading |
| **Background App Stopper** | Standalone ADB or Shizuku | Automated background app freezing |
| **App Manager** | Standalone ADB or Shizuku | Package enable/disable & profile control |
| **Resolution & DPI Tuner** | Standalone ADB or Shizuku | Display scaling and density tweaks |
| **System Table Macros** | Standalone ADB or Shizuku | System/Secure/Global settings automation |
| **Package & APK Installer** | Standalone ADB or Shizuku | Privileged multi-format package installation |
| **Privacy Tiles (Camera & Mic)** | (Standalone ADB or Shizuku) + Android 12+ | Global hardware sensor privacy toggles |

---

## Privacy & Security

- **Strict Internet Permission Usage:** The `android.permission.INTERNET` permission is declared and used strictly for two transparent purposes:
  1. Communicating locally with the device's internal Wireless Debugging service on loopback (`localhost`).
  2. Querying the public GitHub Releases API (`api.github.com`) and downloading official update APKs when manually initiated by the user.
- **Zero Analytics or Telemetry:** No trackers, advertisements, telemetry SDKs, or background metric gathering. All app data and system settings remain strictly on your device.
- **Rootless Privileged Access:** Uses standard Android Wireless Debugging or user-authorized Shizuku APIs, avoiding dangerous root modifications.

---

## Google Play Protect Notice

Because SwiftSense interacts with system-level configuration tables and uses elevated debugging APIs, **Google Play Protect** may occasionally flag an advisory during installation. This is a false positive common to developer tools and system tweaking utilities. SwiftSense is completely open-source, contains zero trackers, and is safe to use.

---

## License

This project is open-source and licensed under the [GNU General Public License v3.0](LICENSE).
