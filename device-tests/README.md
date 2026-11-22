# Phone device verification

The source here is copied into the ICE3 **debug** source set. Its manifest fragment targets the existing `com.recom3.snow3` package. Release builds do not contain this instrumentation.

Build/install the ICE3 development APK first. With the glasses helper running and both devices on the same private network:

```powershell
.\tools\verify-phone.ps1 -GlassesSerial JET_SERIAL -PhoneSerial PHONE_SERIAL -HostAddress GLASSES_IPV4 -Source screen
.\tools\verify-phone.ps1 -GlassesSerial JET_SERIAL -PhoneSerial PHONE_SERIAL -HostAddress GLASSES_IPV4 -Source camera
```

The script provisions a temporary private test JSON through non-PTY phone ADB stdin, never putting the pairing code into shell arguments, and deletes it afterward. It then runs a ten-second actual Wi-Fi source/encoder check inside the target app. Expect explicit instrumentation `result=PASS`; the ADB process exit code alone is not a test assertion.

The test starts a visible ICE3 Capture foreground network session, captures a real still, encodes received preview frames through the production PhoneVideo class, reopens the MP4 and saves a decoded sample frame. The PC controls the test but is not a media relay. This does not validate normal UI controls, background recording or long-run reliability.

Running instrumentation may terminate the normal ICE3 app process. Use only when no other ICE3 operation is in progress. Media stays in the app-owned Capture directory and is not uploaded.