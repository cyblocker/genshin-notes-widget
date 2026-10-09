# Genshin Impact Real-Time Notes - Android Glance AppWidget

A native Android desktop widget built with **Jetpack Compose Glance** (`androidx.glance:glance-appwidget`) that displays real-time Genshin Impact notes with the high information density of iOS shortcuts.

[![Build Android APK](https://github.com/cyblocker/genshin-notes-widget/actions/workflows/build.yml/badge.svg)](https://github.com/cyblocker/genshin-notes-widget/actions/workflows/build.yml)

### 📥 Download APK
Pre-built APKs are generated automatically by GitHub Actions on every commit and release:
- Go to the **[Releases](https://github.com/cyblocker/genshin-notes-widget/releases)** page for official versioned releases.
- Or check the latest successful workflow run under **[Actions](https://github.com/cyblocker/genshin-notes-widget/actions/workflows/build.yml)** to download `genshin-notes-widget-release-apk` directly from Artifacts.

---

## Features

- **Jetpack Compose Glance UI**: Modern, sleek dark theme with high data density.
- **Full Daily Note Support**: Calls the complete `dailyNote` endpoint directly (not the stripped `getWidgetData`).
  - **Original Resin**: Live count / max (200), progress bar, and dynamic time-to-full countdown (`Xh Ym`).
  - **Commissions**: Tasks completed vs total (e.g. `4/4`), with distinct indicator for claimed extra rewards (`Claimed ★`).
  - **Trounce Domains (Boss Discount)**: Remaining weekly discount runs (e.g. `3/3`).
  - **Expeditions**: Completed count and dynamic lowest countdown among ongoing expeditions.
  - **Parametric Transformer**: Displays "Ready" or remaining cooldown countdown (`Xd Xh`).
  - **Realm Currency**: Current vs maximum Serenitea Pot coin capacity.
- **Local Linear Extrapolation**: Extrapolates resin (+1 per 8 minutes / 480s) and expedition/transformer cooldowns locally between sync intervals without aggressive battery-draining polling.
- **Interactive Glance Refresh**: One-tap refresh button on the widget (`ActionCallback`) triggers immediate sync.
- **Jetpack WorkManager**: Background synchronization on a 30-minute interval with network constraints.
- **DataStore Storage**: Securely stores UID, Server, and Cookie.
- **Work Profile Bypass**: Targeted for seamless ADB deployment on enterprise Work Profile restricted devices.

---

## Endpoints & Servers

- **HoYoLAB (Global):**
  `https://bbs-api-os.hoyolab.com/game_record/app/genshin/api/dailyNote?role_id={UID}&server={SERVER}`
  - Servers: `os_asia` (Asia), `os_usa` (America), `os_euro` (Europe), `os_cht` (TW/HK/MO)
- **Miyoushe (CN):**
  `https://bbs-api.miyoushe.com/game_record/app/genshin/api/dailyNote?role_id={UID}&server={SERVER}`
  - Servers: `cn_gf01` (Official), `cn_qd01` (Bilibili)
  - Includes standard dynamic secret `DS` MD5 calculation header.

---

## Build & Deployment

### 1. Build APK
From the project root:
```bash
./gradlew assembleDebug
# or for release:
./gradlew assembleRelease
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk` (or `release/app-release.apk`)

### 2. Deploy to Android (Bypassing Work Profile Restrictions)
To bypass enterprise MDM / Work Profile restrictions on corporate enrolled devices, install directly into User 0 (Primary Personal Profile):
```bash
adb install -r --user 0 app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration Guide

1. **Extract Cookie**:
   - Log into [hoyolab.com](https://www.hoyolab.com) or [miyoushe.com](https://www.miyoushe.com) in your desktop browser.
   - Press `F12` (Developer Tools) -> `Application` tab -> `Cookies`.
   - Copy `ltuid_v2` (or `ltuid`) and `ltoken_v2` (or `ltoken`).
   - Format: `ltuid_v2=XXXX; ltoken_v2=YYYY;`
2. **Privacy Setting**:
   - In HoYoLAB / Miyoushe Privacy Settings, ensure **"Real-Time Notes"** is set to **Public** for your character record.
3. **App Setup**:
   - Open **Genshin Notes** on your phone.
   - Input your **UID**, select your **Server**, and paste the **Cookie**.
   - Tap **Test API** to verify credentials.
   - Tap **Save & Sync**.
4. **Add Desktop Widget**:
   - Long press on your home screen -> Widgets -> **Genshin Notes** -> Add **Genshin Real-Time Notes**.
