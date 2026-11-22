# Recom3Capture

Design preparation for a Recon Jet / Snow2 capture application and an ICE3 phone companion feature.

**Status: design only, 22 November 2026. No application code, APK or hardware validation has been produced for this project.** Hardware is reserved for another task; no ADB commands, installs or device configuration changes were performed during this preparation.

## Intended behavior

- Send a photograph from the Jet camera to the phone, triggered from the phone or glasses.
- Show the Jet camera live on the phone and record camera clips.
- Take a screenshot of the foreground glasses application on Jet and Snow2.
- Show the glasses display on the phone and save a video of activity across glasses applications.
- Offer capture, start, stop, connection state and a local phone gallery through ICE3.

These are planned features. Camera, display capture and transport support must be negotiated independently on every unit. Snow2 display capture is in scope; camera features must remain unavailable when no camera is reported.

## Proposed components

| Component | Responsibility |
| --- | --- |
| Glasses APK, proposed package `com.recom3.capture` | HUD controls, background session, camera source, display source adapters, capture and transport |
| ICE3 / Snow3 Capture feature | Existing Bluetooth connection owner, remote controls, preview, gallery, phone-side display-video encoding |
| Optional display helper | Only if ordinary application permissions cannot capture other applications; exact deployment depends on hardware findings |
| Shared capture contract | Versioned commands, capability report, acknowledgements, media metadata and errors |

The preferred phone integration extends the existing connection owner instead of opening a second set of competing Recon sockets. There are two local Snow3 checkouts with different history and remotes; select the intended checkout before implementation. Neither checkout was changed in this phase.

## Initial technical direction

Use the existing Recon OBJECT/FILE Bluetooth paths for control and still images. Prefer a separately negotiated Wi-Fi media session for sustained video. The existing Jet Vision reports establish single-photo transfer and remote capture as useful prior work; they do not establish live-video bandwidth.

On the old glasses firmware, display capture requires an explicit feasibility check. Android MediaProjection begins at API 21, and the standard screenrecord tool begins at API 19. Neither can be assumed available on the API 16 Jet baseline. The design includes a privileged/helper adapter and a separate PC-assisted diagnostic path; it does not claim that an ordinary APK can capture every other app.

No cloud service is required by the proposed capture feature. Initial audio scope is silent video; microphone capture is a later, separately tested capability.

## Preparation documents

- [Architecture and feasibility](docs/architecture.md)
- [Draft command and media contract](docs/protocol.md)
- [Hardware validation plan](docs/hardware-validation.md)
- [Implementation backlog and decisions](docs/backlog.md)

There is no build or installation procedure yet. After the user releases the devices, validate the display source and transport first, then implement and build the smallest supported feature set. A published repository and GitHub push have not been requested or performed.

## References

- [Android MediaProjection API, added at API 21](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [Android ADB screenshot and screen recording tools](https://developer.android.com/tools/adb)
- Local Jet Vision documentation and source, ReconCast framebuffer client, stock ReconCamera, Recon HUD connectivity code and archived ModLiveLiveServer were inspected; see architecture for specific relative locations and limitations.
