# Implemented prototype protocol

Version 1, local Wi-Fi HTTP, port 18884. No Recon Bluetooth control/media adapter is implemented in this prototype. Existing ICE3 Bluetooth owners and cloud Vision messages are unchanged.

## Pairing and authentication

The explicitly started glasses service generates a random eight-digit pairing code and an independent 128-bit bearer token. `POST /pair` accepts the code as an eight-byte text body and returns a JSON token on success. Eight rejected attempts in a minute block further pairing until the window expires. Restarting the glasses service invalidates the old token/code.

All other requests require `Authorization: Bearer TOKEN`. Codes/tokens are not included in URLs. Cleartext HTTP is limited to a trusted private hotspot deployment; the current prototype does not implement TLS. The phone UI communicates that constraint before pairing.

## Endpoints

| Method and path | Result |
| --- | --- |
| `POST /pair` | `{ "token": "..." }`, or 401/429 |
| `GET /status` | Independent `screen` and `camera` booleans and `protocol: 1` |
| `GET /frame?source=screen` | Latest requested display JPEG, obtained through the local helper |
| `GET /frame?source=camera` | Camera preview JPEG, initially 320 x 240 |
| `POST /photo?source=screen` | PNG screenshot of the current foreground glasses display |
| `POST /photo?source=camera` | One full camera JPEG, followed by preview restart |
| `GET /last-screen` | The locally stored delayed screenshot, if one exists |

Responses use Content-Length, Cache-Control: no-store and Connection: close. Frames are individually requested; there is no MJPEG connection, chunked upload, H.264 network stream or automatic reconnection. A successfully received still is saved before the phone reports Saved. Completed videos are renamed from `.partial` to `.mp4` only after muxer finalization.

The phone requests live frames with a 200 ms delay after each completed iteration. Capture, transfer, decode and encode costs reduce the delivered rate. Camera JPEG caching limits compression to roughly every 400 ms, so repeated requests can receive the same camera frame. Recordings preserve phone arrival timing, not a claimed glasses-to-phone latency measurement.

## Bounds and concurrency

- HTTP request lines: at most 4096 bytes; target: at most 1024 characters.
- Headers: at most 8192 characters; duplicate headers, malformed Content-Length and Transfer-Encoding are rejected.
- Request body: at most 4096 bytes. The only meaningful body is the pairing code.
- Glasses HTTP workers: three, with six queued connections; connections time out.
- Local helper frame: at most 1 MiB. Phone response: at most 1.5 MiB before decode; dimensions: at most 2048 x 2048.
- One phone worker serializes photo/live/encoder work. The UI stops video before changing live source.
- No automatic photo retry, capture on reconnect, cloud call or Bluetooth socket recreation.

Malformed or unavailable capture requests return a sanitized failure. A non-null result from the hidden Surface API is not treated as proof of display access: the helper also requires nonzero dimensions. The ordinary-app probe on the tested Jet returned a zero-sized unusable bitmap.

## Local display helper

The helper can be started with the existing authorized ADB shell identity. Version 0.1.1 can also start it through existing root access after explicit in-app confirmation; normal apps cannot create this access. The launcher reads its key directly from the app-private file. Successful root activation can be remembered for subsequent app launches. An existing system-permission installation can instead capture directly. It binds an Android abstract local socket named `recom3.capture.screen.v1`, not a network port. Its independent 128-bit bootstrap key is read from stdin from a temporary mode-0700 shell-owned directory; it is not embedded in argv or an APK.

For each local socket connection the app sends Java DataOutputStream `writeUTF(key)` followed by one byte: 0 for JPEG, 1 for PNG, 2 for helper shutdown. Successful image responses contain a positive big-endian 32-bit length and payload. Shutdown replies with length zero. Input deadlines and a frame-size bound apply. The key is private to the app/bootstrap and retained across service restarts.

The helper uses the existing Surface screenshot implementation under shell or root privileges. It does not alter permissions, replace firmware, install a system UID app, expose an arbitrary shell executor or enable network ADB. It must be restarted after reboot through existing root access or the authorized PC fallback. No HTTP endpoint requests root activation.

## Future protocol work

Recon OBJECT/FILE still-image fallback, capability limits per model, per-frame capture metadata, synchronized latency measurements, encrypted transport, multiple clients and correlated request/cancel identifiers are future work. They are not represented as implemented by this version number.
