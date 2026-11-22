# Recom3Capture

Native Recon Jet capture prototype with an **ICE3 Capture** feature in the existing phone companion. The glasses APK is `com.recom3.capture` 0.1.0; the phone APK updates `com.recom3.snow3` to 1.0.3-capture.1 / code 4.

## Features

- Phone-triggered camera photographs (1280 x 960 on the tested Jet).
- Phone-triggered screenshots of another foreground glasses application.
- Camera and display live view over the existing private Wi-Fi/hotspot network.
- Silent H.264/MP4 recording on the phone from incoming camera or display frames.
- Phone capture history and sharing through a restricted FileProvider.
- A glasses-triggered screenshot with a five-second delay, leaving time to return to another app.

The first prototype uses authenticated local HTTP over Wi-Fi for both control and media. The earlier Bluetooth OBJECT/FILE proposal remains future work; this feature does not open or change Recon Bluetooth sockets and does not call a cloud provider.

See [validation results and limitations](docs/validation.md) for the exact tested paths. Source implementation is not proof that every feature or model has passed hardware validation.

## Compatibility and limits

- Glasses: API 16 framework APIs; tested hardware is the original Recon Jet, Android 4.1.2. Snow2 is an intended target and has not been connected for this project. Camera use is conditional on actual camera presence.
- Display capture needs the optional **shell-UID helper**, started explicitly from an authorized PC. A normal app cannot capture the display on the tested firmware. No root, platform signing, global permission changes, system APK replacement or network ADB are used.
- The helper runs locally on the glasses. After startup, capture uses Wi-Fi; a reboot requires starting the helper again from the PC. Starting it does not create a permanent privileged installation.
- Initial camera live frames are 320 x 240; display frames are 428 x 240. Full-resolution still photos are separate from preview frames. Actual fps depends on capture, link and phone processing; no 30 fps claim is made.
- Phone videos contain received preview frames, use phone monotonic arrival timing and are silent. They are not full-resolution native camera recordings. Recording is limited to two minutes / 128 MiB per clip, with a 256 MiB folder-size check before starting another capture and free-space checks. An in-progress clip can grow beyond that start threshold.
- Keep ICE3 Capture visible while using live view/recording. Leaving the screen stops and finalizes recording. Locked-phone/background recording and endurance are not supported acceptance claims.
- Traffic is authenticated but **not encrypted at the application layer**. Use a trusted private hotspot. A new session generates a new eight-digit pairing code and random 128-bit bearer token. Pairing attempts are rate limited. Capture pairing credentials are not built into either APK.
- The camera is released after approximately ten seconds without a camera request. The glasses network service stays visibly active until Stop Capture is selected; stop it when finished to release its wake/Wi-Fi locks.

## Build

Use JDK 8, Android SDK Platform 16 and Build Tools 27.0.3. Configure `ANDROID_HOME` or pass `-Sdk`. The glasses build uses SDK tools directly and does not download Gradle dependencies.

```powershell
.\run-checks.ps1
.\build.ps1 -Mode Apk
```

Development artifact: `dist/Recom3Capture-0.1.0-dev.apk`. The app is intentionally debuggable for the helper bootstrap and development probes. Do not distribute it as a production release. Its generated development keystore, build outputs and media are ignored.

ICE3 implementation is maintained in a separate linked checkout under `.local/ice3`, branch `codex/capture-phone` in the existing Snow3 repository. It combines current Bluetooth reply fixes and the existing Jet Vision/App Actions modifications. The original Snow3 checkouts are not switched or edited. An integration patch and checkout details are provided under `companion/`.

```powershell
# From the linked ICE3 checkout, using its existing JDK 8 / Gradle 4.1 toolchain:
.\gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest
```

Local delivery APK: `artifacts/ICE3-1.0.3-capture.1-debug.apk`. The previous installed ICE3 APK is backed up under `artifacts/fallback/`. Media, credentials and APKs are not committed.

## Install and use

Use explicit serials from `adb devices -l`; never select the first connected device automatically. Update the existing ICE3 package in place only after checking that the candidate signature matches. Do not uninstall it to work around a mismatch.

```powershell
adb -s JET_SERIAL install -r dist/Recom3Capture-0.1.0-dev.apk
adb -s PHONE_SERIAL install -r artifacts/ICE3-1.0.3-capture.1-debug.apk
.\tools\start-helper.ps1 -Serial JET_SERIAL
```

1. Keep the glasses and phone on the same private network. Existing hotspot settings need not change.
2. Open **Recom3 Capture** on the glasses. Note the glasses IPv4 address and pairing code.
3. Open **ICE3 Capture** on the phone (also linked from Snow3 Vision). Enter the address without a port and the eight-digit code; tap **Connect glasses**.
4. Select `screen` or `camera`, then capture a still, start live view or start recording. Stop recording before changing source. The phone stores completed media in its app-owned `Capture` folder; use **Saved captures** to view/share it.
5. To capture another app from the glasses, select **Screenshot in 5 seconds**, return to the desired app, then retrieve it on the phone with **Get delayed glasses screenshot**.
6. **Stop Capture** on the glasses stops the network service. The helper can be removed independently with `tools/stop-helper.ps1 -Serial JET_SERIAL`, or ends on reboot.

The helper bootstrap uses private app storage and a shell-owned temporary directory with mode 0700. Keys are not passed as command-line arguments or printed. The startup script handles the legacy device's PTY/SIGHUP behavior explicitly.

## RIF package and 64px catalog thumbnail

Local distribution files are `dist/Recom3Capture.rif` and `dist/thumbnail.png`. The RIF wraps the existing signed glasses APK; the thumbnail is a separate, exact **64 x 64 RGBA PNG** for the catalog. The camera/display capture icon is original SVG artwork, not a photograph or recorded frame.

![64px capture catalog thumbnail](assets/thumbnail.png)

Package with the existing trusted local RIF utility:

```powershell
# Reuse the current, verified development APK:
.\package.ps1 -RifTool 'C:\Tools\PrepRecom3Rif.jar' -SkipBuild
# Build the glasses APK first, then package it:
.\package.ps1 -RifTool 'C:\Tools\PrepRecom3Rif.jar'
# Wrap another already signed APK:
.\package.ps1 -RifTool 'C:\Tools\PrepRecom3Rif.jar' -SkipBuild -Apk '.\dist\Recom3Capture-0.1.0.apk'
```

The packaging script decodes a disposable copy and verifies that its SHA-256 matches the input APK. It checks the PNG signature and exact dimensions and writes `dist/SHA256SUMS.txt`. The external RIF utility is not redistributed. The delivered RIF contains the existing development-signed 0.1.0 APK and does not bundle the phone companion or start the display helper automatically. Existing installation and helper requirements still apply.

Editable icon artwork is `assets/capture.svg`; the generated PNG is checked in so packaging does not require an image renderer. To regenerate it, install ImageMagick and run `tools/generate-thumbnail.ps1`. APKs, RIF outputs, captured media and signing keys remain outside Git. This packaging step does not install or publish anything.

## Documents

- [Implementation protocol](docs/protocol.md)
- [Validation report](docs/validation.md)
- [Initial architecture and future options](docs/architecture.md)
- [Hardware test checklist](docs/hardware-validation.md)
- [Backlog](docs/backlog.md)

Official platform references: [MediaProjection API 21+](https://developer.android.com/reference/android/media/projection/MediaProjection) and [ADB screen capture tools](https://developer.android.com/tools/adb). This prototype implements neither MediaProjection nor the unavailable API 19 screenrecord tool on the glasses.
