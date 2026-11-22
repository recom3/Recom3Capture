# Hardware validation plan

**Not executed. Wait for the user to release the glasses.** This project preparation did not enumerate attached devices or run ADB.

## 1. Identify and isolate the targets

- Identify the intended Jet, Snow2 and phone and use serial-qualified commands for all later device operations.
- Record model, API level, firmware/build fingerprint, resolution and current foreground app privately; omit personal device identifiers from public reports.
- Identify the phone's installed ICE3 package/version/signing history and the correct Snow3/Ice3 source checkout. Review local changes before editing anything.
- Preserve existing working APKs and application data. Do not uninstall a companion to overcome a signature mismatch.
- Confirm that the device session currently in use is finished before installing or launching capture candidates.

## 2. Probe display capture first

- Check whether a normally installed debug APK can obtain a complete display frame. Test existing permission/interface access without altering system files or global permissions.
- Compare a normal UI, another third-party app and a moving/video surface. Check orientation, stride, color format, refresh, stale frames and overlay omissions.
- Verify whether returning to another app keeps the capture service/source alive.
- If the ordinary app cannot capture, document the exact denied operation. Evaluate an optional helper as a separate deployment choice; a PC ADB screenshot alone does not prove a standalone mobile feature.
- Repeat on Snow2 independently. Do not extrapolate Jet results from model names or source-tree similarity.

Acceptance: a verified frame of another foreground app, with a documented installation/privilege path. Otherwise keep display controls unavailable and state the limitation.

## 3. Reuse control and still-photo transport

- Pair/connect through the selected companion's existing Recon connection owner.
- Demonstrate a local Jet button photo and a phone-triggered photo; compare received bytes/hash and save/open the file on the phone.
- Demonstrate a phone-triggered screenshot and a delayed glasses-triggered screenshot of another app, if display capability passed.
- Cancel before capture and during transfer; inject duplicate/delayed commands. No repeated shutter, late capture or duplicate gallery item should result.
- Confirm no competing COMMAND/OBJECT/FILE sockets and no AI/cloud request for capture-only actions.

## 4. Measure transport rather than assume it

- Measure Bluetooth-only transfer time and sustainable payload throughput without cloud processing.
- Inspect the existing Wi-Fi/hotspot role and coexistence with other active apps before proposing changes.
- Validate phone/glasses endpoint reachability, legacy TLS interoperability and session authentication.
- Start with a 320 x 240 / 2 fps camera profile and native-display / 2 fps screen profile, only when actually supported.
- Record requested versus delivered fps, timestamp-based latency, dropped frames, CPU, memory, storage growth and connection stability. Use synchronized clocks or round-trip bounds for cross-device latency, not subtraction of unrelated monotonic clocks.
- Increase profiles only after stable runs. Use measured results to choose defaults.

## 5. Recording and recovery

- Screen: record a short cross-app interaction, stop from the phone, reopen the MP4 and check duration, frame order, gaps and orientation.
- Camera: record a short supported MediaRecorder clip, stop/finalize, transfer and reopen it on the phone. Confirm it is not confused with display video.
- Pull the connection, lock/unlock the phone, background the UI, return to another glasses app, stop the session and simulate insufficient storage.
- A disconnect stops live capture and does not restart automatically. Completed media remains available; incomplete files are clearly marked or removed according to documented policy.
- Confirm pressing Back and returning to a recorded app does not inadvertently stop display acquisition or record only the capture menu.

## 6. Coexistence and endurance

- Test ReconCamera/Jet Vision ownership conflicts, ICE3 synchronization, Snow2 remote controls and existing networking.
- Test Open Headunit/Android Auto coexistence as a separate case if required. Camera acquisition can change activity lifecycle and network usage.
- Confirm readable HUD state and stop controls; no global key interception should steal another app's controls.
- Run 5-minute and then 15-minute sessions if short runs succeed; record heat, battery change, frame rate and memory. Avoid claiming long-run reliability from a short clip.

## Deliverable evidence

Keep a matrix for each model with source, permissions/helper requirement, transport, measured profile, test duration and pass/fail. Supply a real sample photo, screenshot, camera clip and display clip only for paths that passed. Raw media/logs stay in ignored evidence directories and require review before sharing. Distinguish host tests, emulator checks, prior-project evidence and new hardware results.
