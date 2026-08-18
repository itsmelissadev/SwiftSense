# SwiftSense

SwiftSense is an offline, privacy-first Android utility designed for fine-grained system tuning, AMOLED display protection, and hardware-level optimizations without root (via Shizuku).

---

## Features

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
- Fast batch enable/disable and freeze operations powered by Shizuku without artificial execution delays.

### Sensor Boost
- Unlocks sensor sampling rates (gyroscope, accelerometer) to their maximum hardware limits for lower input latency.
- Real-time sensor frequency monitor (Hz).

### Screen Resolution & DPI Tuner
- Adjust display resolution and density on the fly.
- Save and switch between custom profiles (e.g. Performance vs. Battery Saving).

### System Table Macros
- View and modify Android `System`, `Secure`, and `Global` settings tables.
- Create, run, export, and import multi-setting tweak macros via JSON.

### Screen Recorder
- Hardware-accelerated (HEVC/AVC) screen recorder with minimal overhead.
- Supports internal audio capture and customizable resolution, framerate, and bitrate (up to 1080p, 60 FPS).
- Includes Quick Settings tile for quick capture.

---

## Feature Requirements

| Feature | Requirement | Purpose |
| :--- | :--- | :--- |
| **Always-On Display** | Android 8.0+ | Lock screen overlay |
| **AMOLED Screen Protect** | Android 8.0+ | Anti-burn-in overlay |
| **Sensor Boost** | Android 8.0+ | High-rate sensor sampling |
| **Background App Stopper** | Shizuku | Automated background app freezing |
| **App Manager** | Shizuku | Package enable/disable control |
| **Resolution & DPI Tuner** | Shizuku | Display scaling and density tweaks |
| **System Table Macros** | Shizuku | System/Secure/Global settings editor |
| **Screen Recorder** | Android 10+ | Internal audio & screen capture |

---

## Privacy & Security

- **No Internet Permission:** SwiftSense does not declare `android.permission.INTERNET`. All data remains strictly on your device.
- **No Analytics or Telemetry:** No tracking, telemetry, or remote logging.
- **Rootless System Access:** Operates securely through user-authorized [Shizuku](https://shizuku.rikka.app/) APIs.

---

## Google Play Protect Notice

Because SwiftSense interacts with system-level settings and utilizes Shizuku, **Google Play Protect** may occasionally display a warning during installation. This is a common false positive for advanced system utility applications. SwiftSense is fully open-source, contains no trackers or network capabilities, and is safe to use.

---

## License

This project is open-source and licensed under the [GNU General Public License v3.0](LICENSE).
