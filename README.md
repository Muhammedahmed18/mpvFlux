<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="MpvFlux icon">

# MpvFlux

**A fast, battery-friendly local video player for Android, built on mpv.**

Full ASS/SSA subtitle rendering, hardware decoding and a modern Material 3 design. Made with anime in mind.

[![Latest release](https://img.shields.io/github/v/release/Muhammedahmed18/mpvFlux?label=release)](https://github.com/Muhammedahmed18/mpvFlux/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Muhammedahmed18/mpvFlux/total)](https://github.com/Muhammedahmed18/mpvFlux/releases)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#-download)
[![License](https://img.shields.io/github/license/Muhammedahmed18/mpvFlux)](LICENSE)

[**Download**](https://github.com/Muhammedahmed18/mpvFlux/releases/latest) · [Features](#-features) · [Build](#-building-from-source) · [Report a bug](https://github.com/Muhammedahmed18/mpvFlux/issues)

</div>

---

## ✨ Features

### 🎬 Playback
- **Powered by mpv:** plays practically any local video format.
- **Hardware decoding first:** uses MediaCodec and falls back to software only when needed.
- **Picture-in-picture** and **background audio playback**.
- **Resume where you left off**, plus a recently played list and playlists.
- Frame-by-frame navigation, chapters, speed presets and a sleep timer.
- Advanced users can edit `mpv.conf` directly in the app.

### 💬 Subtitles
- **Full ASS/SSA rendering** through libass, with your typesetting, karaoke and signs exactly as the fansub intended.
- **Online subtitle search with hash matching:** identifies your file from its first and last 64 KB, so the subtitles you find are synced to that exact release.
- Custom fonts, styling, subtitle delay and secondary subtitles.

### 🔋 Battery and performance
- **No polling loops:** the player only reacts to real changes instead of checking every few milliseconds.
- **Video decoding pauses during background playback**, so audio-only listening doesn't waste power.
- **Smart media scanning:** an asynchronous scanner with caching keeps large libraries responsive, and full rescans are limited to once every 12 hours.
- **Lightweight thumbnail cache**, limited in size and cleaned up automatically.

### 🎨 Design
- **Material 3 Expressive** UI with dynamic (wallpaper) colours and an OLED black option.
- Customizable gestures for volume, brightness and seeking, plus a custom player control layout.
- Folder and file browser with copy, move, rename and delete.

### 🔒 Privacy
No ads, no trackers, no analytics. Fully open source.

---

## 📥 Download

Get the latest APK from the [**Releases page**](https://github.com/Muhammedahmed18/mpvFlux/releases/latest).

| File | For |
|---|---|
| `…-arm64-v8a-….apk` | Most phones (recommended) |
| `…-armeabi-v7a-….apk` | Older 32-bit phones |
| `…-x86_64-….apk` / `x86` | Emulators, Chromebooks |
| `…-universal-….apk` | Not sure? This one works everywhere |

**Requires Android 8.0 (API 26) or newer.**

> **Coming from mpvEx or MpvFlux 1.1.x?** Since 1.2.0, MpvFlux uses its own app ID
> (`app.marlboroadvance.mpvflux`), so it installs as a separate app. Your old watch
> history and settings don't carry over.

---

## 🛠️ Building from source

**You need:** Android Studio (latest stable) and JDK 17.

```bash
git clone https://github.com/Muhammedahmed18/mpvFlux.git
cd mpvFlux
./gradlew assembleStandardDebug
```

The APK ends up in `app/build/outputs/apk/standard/debug/`.

| Build flavour | Purpose |
|---|---|
| `standard` | GitHub releases (per-ABI + universal APKs) |
| `playstore` | Play Store build |
| `fdroid` | F-Droid build |

---

## 🤝 Contributing

Bug reports, ideas and pull requests are welcome.

- **Found a bug?** [Open an issue](https://github.com/Muhammedahmed18/mpvFlux/issues/new?template=bug_report.md) and include your phone model, Android version and, if possible, the video file type.
- **Have an idea?** [Suggest a feature](https://github.com/Muhammedahmed18/mpvFlux/issues/new?template=feature_request.md).
- **Sending a PR?** Keep it focused on one change and use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `perf:`, `ui:`, …).

---

## 🙏 Credits

MpvFlux is a fork of [**mpvEx**](https://github.com/marlboro-advance/mpvEx) and stands on the shoulders of:

- [mpv](https://mpv.io/) and [mpv-android](https://github.com/mpv-android/mpv-android): the playback engine
- [libass](https://github.com/libass/libass): ASS/SSA subtitle rendering

## 📄 License

Released under the [Apache License 2.0](LICENSE).
