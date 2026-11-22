# Implementation backlog

Updated 22 November 2026. Initial Jet/Pixel prototype implemented; Snow2 and unlocked-phone UI acceptance remain pending.

## Completed

- [x] Separate API 16 glasses APK and independent camera/display sources.
- [x] Demonstrate ordinary-app display denial and explicitly started shell-UID display helper.
- [x] Native ICE3 Capture entry with pairing, independent capabilities, live view and saved-media sharing/history.
- [x] Combine current Snow3 Bluetooth fixes with the existing Jet Vision/App Actions modifications in an isolated checkout.
- [x] Preserve previous installed ICE3 APK, verify matching signature and update the phone without clearing data.
- [x] Real Jet camera photograph, camera preview and screenshot of another foreground application.
- [x] Pixel-direct Wi-Fi reception and silent phone MP4 recording for both camera and display.
- [x] Inspect decoded phone video frames and complete-file decode.
- [x] API 16 build/signature checks, 18 HTTP bounds/framing assertions and 123 existing ICE3 regression tests.
- [x] Helper start/stop and private bootstrap scripts; no root, firmware changes or network ADB.

## Remaining acceptance

- [ ] Unlocked-phone manual UI: pairing, photo, live/stop, record/stop, source switching, saved-media viewer/share.
- [x] Glasses delayed screenshot and display recording while navigating Settings sections on the final build.
- [ ] Test Snow2 hardware independently, including display privileges, dimensions, Wi-Fi and no-camera behavior.
- [ ] Short-run failure tests and longer battery/thermal/network/coexistence tests before broader use.

## Later improvements

- [ ] Higher delivered fps and hardware H.264 streaming, if supported and measured.
- [ ] Recon OBJECT/FILE Bluetooth still-photo/control fallback.
- [ ] Encrypted transport compatible with API 16; per-frame capture timestamps and measured latency.
- [ ] Full-resolution glasses-local MediaRecorder clips, audio and background/locked-phone recording as separate capabilities.
- [ ] Installation/helper startup integration with Uplink after the prototype is accepted.

No GitHub repository creation or push is authorized by this work. Initial architecture proposals remain documented separately from implemented behavior.