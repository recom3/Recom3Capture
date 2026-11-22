# Draft capture contract

Design version 0.1. This document proposes semantics; there is no implemented parser or interoperable endpoint yet.

## Control transport

Use Recon OBJECT for small UTF-8 JSON commands and status inside its existing envelope. Use a dedicated capture intent/action namespace. Do not invent replacement Recon service UUIDs or alter the original framing. Still-image bytes use Recon FILE with the existing GZIP handling, as in the separate Jet Vision implementation.

Proposed action namespace: `com.recom3.capture.CONTROL`, `com.recom3.capture.EVENT` and `com.recom3.capture.MEDIA`. Exact registration must be checked against both firmware and the selected phone checkout.

Every command carries `protocolVersion`, `sessionId`, `requestId` and `command`. Request IDs are random UUIDs. A session is explicitly opened by the user; reconnect establishes a new session and invalidates outstanding capture work. Do not treat a sender string in a legacy broadcast as cryptographic authentication.

Example request:

```json
{
  "protocolVersion": 1,
  "sessionId": "11111111-1111-4111-8111-111111111111",
  "requestId": "22222222-2222-4222-8222-222222222222",
  "command": "capture.once",
  "source": "screen",
  "delayMs": 3000
}
```

The IDs above are synthetic examples. A command acknowledgement means accepted or rejected, not captured, transferred or saved.

## Commands

| Command | Meaning |
| --- | --- |
| `capabilities.get` | Query model/API, independently probed sources, formats, transports and limits |
| `capture.once` | One camera photo or screen image, with optional bounded delay |
| `live.start` / `live.stop` | Start/stop the selected source and negotiated live transport |
| `record.start` / `record.stop` | Start/stop a recording with explicit recording location and source |
| `request.cancel` | Cancel one pending request, including delayed capture |
| `session.close` | Release capture resources owned by this feature |

A display recording requests live frames and phone-side recording. A camera recording may use glasses-local MediaRecorder with transfer after finalization. The acknowledgement must identify the actual location; do not imply a phone recording exists merely because the glasses started recording locally.

## Capabilities and state

Report separate statuses for `cameraPhoto`, `cameraLive`, `cameraRecord`, `screenStill`, `screenLive`, `screenRecord` and each transport. Use `available`, `unavailable` or `unprobed`, with a reason and actual supported limits. Supported source formats and transport availability are separate facts; an action is enabled only when its whole path is usable.

Proposed lifecycle:

`DISCONNECTED -> CONNECTED -> PROBING -> READY -> STARTING -> ACTIVE -> STOPPING -> READY`

Cancellation/failure emits a correlated terminal event and releases owned resources. Source, live-preview and recording state remain explicit even if represented within one session. A failed start must not show ACTIVE; a command write must not show delivery confirmed. Late events from a closed session are ignored.

## Events and receipts

Emit correlated `accepted`, `rejected`, `capture.completed`, `media.received`, `media.saved`, `live.started`, `live.stopped`, `record.started`, `record.completed`, `cancelled` and `error` as applicable.

For still media, metadata includes the source, request ID, media ID, MIME type, byte count, dimensions, rotation, monotonic capture timestamp and SHA-256 of uncompressed file bytes. Verify content/size/dimensions/hash after receipt. `media.received` follows validation; `media.saved` follows durable local completion. No duplicate capture or duplicate gallery entry is created on redelivery. Do not delete the only saved copy based merely on an accepted command.

Keep a bounded recent-request cache and cancellation tombstones. A cancel arriving before its media suppresses later processing. Expired requests are not replayed after reconnection.

## Provisional limits

These are implementation proposals, not hardware specifications:

- One pending still capture and one active media source per session.
- Control JSON no larger than 8 KiB, nesting depth bounded, known command/field types only.
- Maximum still JPEG/PNG: 1 MiB; maximum FILE envelope: 2 MiB, with decompression bound and existing framing checks.
- Delay: 0-5 seconds. One-shot deadline: 30 seconds including the requested delay; expiry does not cause a retry.
- At most one queued live frame; discard an obsolete frame rather than accumulating latency.
- Initial live JPEG frame limit: 512 KiB, with dimensions capped by negotiation before allocation.
- Live inactivity timeout, recording duration/size limits and clip-transfer chunks must be set after hardware measurements, before implementation is accepted.

A large or unusually complex screenshot may exceed the still-image limit. Negotiate/downscale explicitly or return SIZE_LIMIT; do not truncate it or label it successful.

## Live-media framing requirements

Use a separate versioned media stream, not a stream of fake FILE photo requests. Require a bounded binary header containing session identity, source, frame sequence, payload length, dimensions/format, orientation and monotonic timestamp. Read partial headers/payloads correctly; reject oversized frames before allocating memory. Validate the session before accepting payloads. Negotiate endpoint authentication/encryption independently from this metadata contract.

The final byte layout, TLS configuration and transport bootstrap are deliberately not frozen before API 16 interoperability and phone-network tests. Video payloads and still-image FILE GZIP framing must not be mixed accidentally.

## Errors

At minimum distinguish NOT_CONNECTED, BUSY, CAMERA_BUSY, CAMERA_UNAVAILABLE, SCREEN_CAPTURE_UNAVAILABLE, HELPER_REQUIRED, TRANSPORT_UNAVAILABLE, UNSUPPORTED_PROFILE, TIMEOUT, SIZE_LIMIT, STORAGE_FULL, CANCELLED and INTERNAL_ERROR. Show actionable text and retain only sanitized diagnostics. Never report a requested capability as working simply because a fallback path can capture the app's own UI.
