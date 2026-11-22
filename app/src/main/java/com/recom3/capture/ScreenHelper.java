package com.recom3.capture;

import android.graphics.Bitmap;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import java.io.*;
import java.lang.reflect.Method;

/** Shell-UID display bridge, local socket only. Started explicitly from an authorized PC. */
public final class ScreenHelper {
    public static final String SOCKET = "recom3.capture.screen.v1";
    public static Bitmap screenshot() throws Exception {
        Method method = Class.forName("android.view.Surface").getDeclaredMethod("screenshot", int.class, int.class);
        Bitmap image = (Bitmap)method.invoke(null, 428, 240);
        if (image == null || image.getWidth() < 2 || image.getHeight() < 2)
            throw new IOException("Display capture unavailable for this process identity");
        return image;
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--probe".equals(args[0])) {
            Bitmap image = screenshot(); FileOutputStream out = new FileOutputStream(args[1]);
            try { image.compress(Bitmap.CompressFormat.PNG, 100, out); } finally { out.close(); image.recycle(); }
            System.out.println("Screen helper probe completed"); return;
        }
        String key = new BufferedReader(new InputStreamReader(System.in, "UTF-8")).readLine();
        if (key == null || !key.matches("[a-f0-9]{32}")) throw new IOException("Missing private bootstrap key");
        if (args.length == 1 && "--stop".equals(args[0])) {
            LocalSocket client = new LocalSocket();
            try { client.connect(new android.net.LocalSocketAddress(SOCKET)); client.setSoTimeout(3000);
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                out.writeUTF(key); out.writeByte(2); out.flush();
                if (new DataInputStream(client.getInputStream()).readInt() != 0) throw new IOException("Stop failed");
            } finally { client.close(); }
            System.out.println("Display helper stopped"); return;
        }
        LocalServerSocket server = new LocalServerSocket(SOCKET);
        System.out.println("Recom3 display helper ready (shell UID, local socket)");
        while (true) {
            LocalSocket socket = server.accept();
            try {
                socket.setSoTimeout(3000);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                String supplied = in.readUTF();
                if (!java.security.MessageDigest.isEqual(key.getBytes("UTF-8"), supplied.getBytes("UTF-8"))) continue;
                int mode = in.readUnsignedByte();
                if (mode == 2) { out.writeInt(0); out.flush(); break; }
                if (mode != 0 && mode != 1) continue;
                Bitmap image = screenshot(); ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try { image.compress(mode == 1 ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, mode == 1 ? 100 : 75, bytes); }
                finally { image.recycle(); }
                byte[] payload = bytes.toByteArray(); out.writeInt(payload.length); out.write(payload); out.flush();
            } catch (Exception e) { System.err.println("Display request failed: " + e.getClass().getSimpleName()); }
            finally { try { socket.close(); } catch (IOException ignored) {} }
        }
        server.close();
    }
}
