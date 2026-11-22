# Implementation backlog

Prepared 22 November 2026. Planning is complete for this phase; implementation and device work are pending.

## Stage 0: prepared offline

- [x] Confirm a new application name/directory does not already exist.
- [x] Inspect Jet photo/control references, phone project identities and display-capture precedents.
- [x] Record the API/permission constraints and separate camera from display behavior.
- [x] Draft component boundaries, control semantics, media requirements and hardware acceptance gates.
- [x] Keep glasses, current applications, existing companion checkouts and network settings untouched.

## Stage 1: feasibility after hardware release

- [ ] Choose the active ICE3/Snow3 checkout and preserve existing local work.
- [ ] Probe ordinary-app display access on Jet and Snow2.
- [ ] Decide whether an optional helper is necessary and how it is started/stopped after reboot.
- [ ] Measure Bluetooth and Wi-Fi capacity and check concurrent app/network requirements.
- [ ] Freeze the smallest supported capability matrix before implementing the UI.

## Stage 2: first useful build

- [ ] Separate API 16 glasses APK with directional HUD controls and negotiated capability state.
- [ ] ICE3 Capture entry using its existing Bluetooth service and explicit foreground session.
- [ ] Jet photo capture, transfer, receipt and local phone gallery.
- [ ] Phone-triggered screenshot and delayed glasses screenshot when the display gate passes.
- [ ] Meaningful host tests for framing bounds, split/coalesced delivery, duplicate/cancel ordering and state transitions.
- [ ] APK/build checks and physical still-image acceptance on each supported model.

## Stage 3: live view and recording

- [ ] Implement negotiated/authenticated media framing with bounded queues and timeouts.
- [ ] Camera live view with supported dimensions and measured defaults.
- [ ] Display live view and phone-side display-video encoder.
- [ ] Camera MediaRecorder clips and completed-file transfer.
- [ ] Stop/failure/storage recovery and playable-file verification.
- [ ] Physical performance, background lifecycle, companion regression and endurance checks.

## Decisions still requiring evidence

- Whether phone-only display capture is possible without a privileged helper.
- Which Snow3/Ice3 checkout should receive the feature; label, package and repository names differ.
- Which Wi-Fi network role coexists with current glasses apps and the phone.
- Whether JPEG live view is sufficient or an API 16 hardware/native H.264 path is worth the additional complexity.
- Exact supported profiles, recording limits and defaults for each physical unit.
- Whether a later standalone companion is useful; the current preference is an ICE3 feature to avoid competing Recon connections.

No clarification is required before these offline design documents can be reviewed. Do not publish, push, install candidates or touch the occupied glasses as part of this preparation.
