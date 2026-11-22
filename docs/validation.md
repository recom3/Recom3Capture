# Prototype validation

Date: 22 November 2026. Original Recon Jet / Android 4.1.2 / API 16 and Pixel 11 / Android 17. Device serials, network names and pairing credentials are omitted.

## Verified so far

| Check | Observed result |
| --- | --- |
| Ordinary APK framebuffer access | Denied; app UID lacks graphics membership |
| Ordinary APK hidden Surface screenshot | Unusable zero-sized result / zero-byte PNG; non-null bitmap alone was misleading |
| Shell-UID Surface helper | Real 428 x 240 display capture obtained without root or system permission changes |
| Capture of another foreground app | Passed: Android Settings screenshot obtained while the capture service was backgrounded |
| Offscreen legacy Camera preview | Passed on Jet: real 320 x 240 JPEG from the service |
| Full-resolution Camera still | Passed on Jet: real 1280 x 960 JPEG |
| Glasses delayed screenshot | Passed: selected the actual HUD button, returned to Settings, then retrieved its new PNG after the delay |
| Helper stop/start | Passed through the authenticated local socket; restarted helper remained present after the ADB command ended |
| Glasses APK build/signature | API 16 bootclasspath build passed; v1 signing verified explicitly for API 16 |
| HTTP bounds/framing host checks | 18 assertions passed |
| Combined ICE3 regression | 123 tests, zero failures/errors |
| ICE3 update-in-place | Installed 1.0.3-capture.1 / code 4 after preserving the previous APK and matching signing certificates; no uninstall or data clearing |

Source acquisition was first checked through an explicit USB ADB forward on the development PC. The subsequent tests below ran inside ICE3 on the Pixel and opened the glasses Wi-Fi address directly. The PC sent development/test commands over USB; it did not relay photo or video bytes between the devices.

## Phone-direct acceptance

| Pixel-direct test | Observed result |
| --- | --- |
| Screen still and MP4 | PASS: actual screenshot received, 26 received frames, 428 x 240, MP4 duration 10,022 ms |
| Camera still and MP4 | PASS: actual full camera JPEG received, 32 received preview frames, 320 x 240 MP4, duration 9,799 ms |
| Display navigation recording | PASS on final build: 27 received frames, 428 x 240, 9,959 ms MP4 while opening Display, returning to Settings and opening Date/Time |
| Phone MP4 decoding | Both files reopened with MediaMetadataRetriever; decoded frame at 3 s inspected visually; complete files also decoded with FFmpeg |

These are short backend acceptance runs using the production HTTP sources and phone encoder. The frame counts include any repeated cached camera frames. Delivered view is approximately 2-3 frames/s in these runs; this is not smooth 30 fps video, a latency benchmark or an endurance result.

After the user unlocked the Pixel, the normal native UI was tested through serial-qualified ADB taps: pairing, source selection, full camera photo, screenshot, live start/stop, screen/camera record start/stop, saved-media selection and opening an MP4 in Files by Google. The Android share chooser opened with the selected MP4 and was cancelled; actual transfer through an external application was not tested. The UI reported successful saves for the screenshot, camera JPEG and both short MP4 recordings. No lock credential was entered or security setting changed.

The successful checks use instrumentation bundled only in the ICE3 debug source set, from the checked-in `device-tests` source. The two standalone verification APK install sessions were identified by shell ownership, exact APK size and creation timestamps and explicitly abandoned, so the supported test route updates the already authorized development ICE3 package and preserves normal platform verification. Backend verification is not a manual UI test. Temporary stills are named `validation-screen` / `validation-camera`; resulting MP4s remain local.

## Evidence

Ignored local `evidence/` contains helper/display captures, full Jet camera JPEG, camera preview, device-test result text and UI dumps. Phone validation media lives in ICE3's app-owned Capture directory. Do not publish room photographs, device identifiers, pairing codes or raw UI dumps without review.

The probe bootstrap initially encountered the legacy device's PTY echo/EOF behavior. The affected temporary helper key was invalidated, the old bootstrap file removed, and transfer changed to a private file inside a shell-owned mode-0700 directory. Current scripts do not send keys through an echoing PTY or print them.

## Remaining limits

Snow2 was not connected and is not validated. Long sessions, heat/battery measurements, camera/audio concurrency, Android Auto coexistence, Bluetooth-only delivery, locked/background phone recording, microphone/internal audio, encrypted transport and full-resolution glasses-local camera clips are not demonstrated. The first release remains a development prototype.

ICE3 feature source is saved locally at merge commit 4611a82 on codex/capture-phone; documentation was clarified in 3c02ac5. Its patch round-trip check passed. At the end of those device tests, no source had been pushed.

Final cleanup removed the development-PC media forward and temporary probe/install files. The installed Pixel APK hash matches the local delivery APK exactly. Both app screens were opened for the user; the unlocked-phone normal UI review subsequently passed as described above.

Publication preparation: the user-created recom3/Recom3Capture repository was verified empty with default branch main, and linked as origin. The existing Windows OpenSSH transport authenticates successfully; the separately selected Recom3 key did not authenticate. No push has been performed. Latest companion documentation commit: 30c7797.
