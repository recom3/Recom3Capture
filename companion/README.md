# ICE3 integration

The existing phone repository is preserved separately. Local branch `codex/capture-phone`, commit **30c7797** (feature merge 4611a82), combines master `b685785` with Jet Vision/App Actions `5fa0677` and adds the native Capture feature. Both current Bluetooth fixes and prior Vision features are included.

The linked checkout is under `.local/ice3` in the Recom3Capture project. It uses the existing Snow3 repository's Git database; its files, credentials and build outputs are not vendored into this repository. The original checkout remains on its original branch.

`ice3-capture.patch` contains the complete source/documentation difference from `b685785`, including Vision dependencies needed for the merged build. It is exported as raw bytes to preserve the original repository's line endings. A reverse-application check against the final checkout passed.

To reproduce in an isolated Snow3 clone at `b685785`, with your own ignored SDK properties:

```powershell
git switch -c codex/capture-phone
git apply --check PATH_TO_ICE3_CAPTURE_PATCH
git apply PATH_TO_ICE3_CAPTURE_PATCH
.\gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest
```

Use the existing working signing identity before updating a phone. Do not uninstall ICE3 on a signature mismatch. SDK/dependency caches and local credentials are not supplied by the patch. Configure provider credentials separately only if using the existing Vision feature; Capture itself has no cloud dependency.

Native feature source is under `app/src/main/java/com/recom3/snow3/capture`. Debug-only device verification is under `app/src/debug`, with source also retained in the parent project's `device-tests` directory.

The companion branch remains local; publishing this integration patch does not push a Snow3 branch.