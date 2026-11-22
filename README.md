# Tapkey

<p align="center">
  <img alt="Tapkey on Google Pixel 7" src="https://github.com/user-attachments/assets/de693379-c414-49b6-95ea-2e556eacf262" width="360">
</p>

A minimal Quick Settings companion to reduce wear on physical hardware buttons.

## Features

- **Lock Screen Tile:** Locks the screen.
- **Volume Panel Tile:** Opens the native volume slider directly on screen.
- **Accessibility Tile:** Disables all active Accessibility services in one tap and restores the same services on the next tap.
- **Clean UI:** Lightweight Material 3 interface, zero background services.

## Installation

1. Download the APK from [Releases](../../releases).  
   *(If Play Protect blocks install: temporarily toggle off "Scan apps with Play Protect" in Play Store settings).*
2. Go to **Settings > Apps > Tapkey > three dots (top right) > Allow restricted settings**.
3. Open the app, enable Accessibility (for the Lock Screen tile), and add the tiles to Quick Settings.
4. *(Optional, for the Accessibility tile)* Grant `WRITE_SECURE_SETTINGS` once via ADB:
   ```bash
   adb shell pm grant com.tapkey.app android.permission.WRITE_SECURE_SETTINGS
   ```

## Credits

Inspired by QuickTiles (big W).
