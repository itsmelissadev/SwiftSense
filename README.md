# SwiftSense

SwiftSense is an **open-source, privacy-focused** optimization toolkit designed to enhance Android
performance, minimize system latency, and provide advanced system tools.

---

### Key Features

* **AMOLED Screen Protect:** Prevents screen burn-in and limits static pixels using a subtle overlay
  mesh. Highly customizable with various patterns (Dots, Grid, Pixel Shift), adjustable opacity, and
  dynamic shift cycles to extend display lifespan.
* **Sensor Optimization:** Boosts sensor sampling rates (Gyroscope, Accelerometer, etc.) to their
  hardware limits. Includes a **Live Frequency Monitor** to track real-time sensor speeds in Hz.
* **Advanced App Manager:** Easily enable or disable system and user applications via **Shizuku**.
  Perfect for freezing resource-heavy apps before gaming.
* **Gaming Screen Recorder:** A low-latency, high-performance recording engine. Optimized for games
  with hardware-accelerated encoding (HEVC/AVC), customizable quality (up to 1080p, 60 FPS, 15 Mbps),
  and support for internal audio. Includes a Quick Settings Tile for instant access.
* **Screen Resolution Tuner:** Modify your device's display resolution and DPI. Create and save *
  *Resolution Plans** for different scenarios (e.g., Gaming vs. Battery Saving).
* **Cache Cleaner:** Reclaim storage by clearing system and app caches via Shizuku.
* **Background Killer:** Free up RAM and reduce CPU usage by force-stopping background
  applications.
* **System Table Macros:** View and modify Android's `System`, `Secure`, and `Global` settings
  tables. Create and **Edit Macros** to apply multiple system tweaks with a single tap. Features *
  *Import/Export** for sharing JSON macro configurations.
* **Total Privacy:** SwiftSense **never** connects to the internet. All data, app lists, and
  optimizations stay strictly on your device.

---

### Technical Details & Requirements

| Feature                | Requirement  | Purpose                       |
|:-----------------------|:-------------|:------------------------------|
| **AMOLED Protect**     | Android 8.0+ | Prevent screen burn-in        |
| **Sensor Boost**       | Android 8.0+ | Low-latency data processing   |
| **App & System Tools** | **Shizuku**  | Advanced system-level control |
| **Resolution/Tables**  | **Shizuku**  | Display and System tweaks     |
| **Cache/Killer**       | **Shizuku**  | Storage and RAM optimization  |
| **Screen Recorder**   | Android 10+  | Gaming-optimized recording    |

---

### Why SwiftSense?

SwiftSense is built for power users who want to squeeze every bit of performance out of their
hardware. Whether you're a competitive mobile gamer or someone looking to breathe new life into an
older device, SwiftSense provides a clean, effective, and secure interface for deep system
optimization.

---

### Security & Privacy

* **Shizuku Integration:** Performs system-level tasks (like disabling apps, changing resolution, or
  clearing cache)
  through a secure, user-authorized bridge.
* **No Internet Access:** The app does not request internet permission. Your data never leaves the
  device.
* **No Analytics:** We don't track you. Period.

---

### Installation Note

Since SwiftSense performs deep system optimizations, **Google Play Protect** may occasionally flag
the installation. This is a common false positive for tools requiring Shizuku or advanced
permissions. SwiftSense is fully open-source and safe to use.

---

### License

This project is licensed under the **GNU GPLv3**. As an open-source project, any derivative works must also be shared under the same license terms. See the `LICENSE` file for more details.
