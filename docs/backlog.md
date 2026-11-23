# Implementation backlog

Updated 22 November 2026. Initial Jet/Pixel prototype implemented and normal unlocked-phone UI acceptance passed; Snow2 hardware remains pending.

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

- [x] Unlocked-phone normal UI: pairing, photo, live/stop, record/stop, source switching, saved-media selection, external viewer and share chooser. External share delivery is not tested.
- [x] Glasses delayed screenshot and display recording while navigating Settings sections on the final build.
- [ ] Test Snow2 hardware independently, including display privileges, dimensions, Wi-Fi and no-camera behavior.
- [ ] Short-run failure tests and longer battery/thermal/network/coexistence tests before broader use.

## Later improvements

- [ ] Higher delivered fps and hardware H.264 streaming, if supported and measured.
- [ ] Recon OBJECT/FILE Bluetooth still-photo/control fallback.
- [ ] Encrypted transport compatible with API 16; per-frame capture timestamps and measured latency.
- [ ] Full-resolution glasses-local MediaRecorder clips, audio and background/locked-phone recording as separate capabilities.
- [ ] Installation/helper startup integration with Uplink after the prototype is accepted.

The 0.1.1 helper activation and launcher icon are proposed in a separate pull request. Physical verification of system/root activation remains pending. Initial architecture proposals remain documented separately from implemented behavior.
## Version 0.1.1

- [x] Add Capture launcher icons at API 16-compatible densities.
- [x] Expose screen readiness and activation with existing system/root access.
- [x] Remember successful root activation for app launches, with a disable control.
- [x] Preserve the authorized PC bootstrap and HTTP/local-socket protocols.
- [ ] Validate UI, root approval/denial, restart and cancellation on actual glasses.
- [ ] Establish a supported permanent privilege installation for ordinary unprivileged glasses; a button alone cannot provide it.
