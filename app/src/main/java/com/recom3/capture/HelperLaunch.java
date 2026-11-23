package com.recom3.capture;

import java.io.IOException;

/** Fixed helper command: paths only, never bootstrap-key bytes or caller-supplied shell code. */
final class HelperLaunch {
    private HelperLaunch() {}

    static String command(String apk, String keyFile) throws IOException {
        safePath(apk);
        safePath(keyFile);
        return "trap '' HUP; (export CLASSPATH=" + apk
                + "; exec /system/bin/app_process /system/bin com.recom3.capture.ScreenHelper < "
                + keyFile + " > /dev/null 2>&1) < /dev/null > /dev/null 2>&1 &";
    }

    private static void safePath(String path) throws IOException {
        if (path == null || !path.matches("/[A-Za-z0-9/._-]+")
                || path.contains("/../") || path.endsWith("/.."))
            throw new IOException("Unsupported helper path");
    }

    /** API 16-compatible bounded wait; releases this client process on every exit path. */
    static int awaitExit(Process process, long timeoutMillis) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1000000L;
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                try { return process.exitValue(); }
                catch (IllegalThreadStateException pending) {
                    if (System.nanoTime() >= deadline) throw new IOException("Root approval timed out");
                    Thread.sleep(25);
                }
            }
        } finally { process.destroy(); }
    }
}
