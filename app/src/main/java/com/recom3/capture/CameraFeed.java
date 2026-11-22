package com.recom3.capture;

import android.graphics.*;
import android.hardware.Camera;
import android.os.*;
import java.io.*;
import java.util.concurrent.*;

final class CameraFeed {
    private final HandlerThread thread = new HandlerThread("capture-camera");
    private final Handler handler;
    private Camera camera; private SurfaceTexture texture;
    private volatile byte[] latest; private volatile String error;
    private long encodedAt;
    volatile long lastAccess;
    CameraFeed() { thread.start(); handler = new Handler(thread.getLooper()); }
    private void open() throws Exception {
        if (camera != null) return;
        if (Camera.getNumberOfCameras() == 0) throw new IOException("CAMERA_UNAVAILABLE");
        camera = Camera.open();
        try {
            Camera.Parameters p = camera.getParameters();
            Camera.Size chosen = null;
            for (Camera.Size size : p.getSupportedPreviewSizes())
                if (chosen == null || Math.abs(size.width*size.height-320*240) < Math.abs(chosen.width*chosen.height-320*240)) chosen = size;
            p.setPreviewSize(chosen.width, chosen.height); p.setPreviewFormat(ImageFormat.NV21);
            Camera.Size picture = null;
            for (Camera.Size size : p.getSupportedPictureSizes())
                if (picture == null || Math.abs(size.width*size.height-1280*960) < Math.abs(picture.width*picture.height-1280*960)) picture = size;
            p.setPictureSize(picture.width, picture.height); p.setJpegQuality(90); camera.setParameters(p);
            texture = new SurfaceTexture(0); camera.setPreviewTexture(texture);
            final int width = chosen.width, height = chosen.height;
            camera.setPreviewCallback(new Camera.PreviewCallback() { public void onPreviewFrame(byte[] data, Camera owner) {
                if (data == null || SystemClock.elapsedRealtime()-encodedAt < 400) return;
                encodedAt = SystemClock.elapsedRealtime();
                try { ByteArrayOutputStream out = new ByteArrayOutputStream();
                    new YuvImage(data, ImageFormat.NV21, width, height, null).compressToJpeg(new Rect(0,0,width,height),65,out);
                    latest = out.toByteArray();
                } catch (RuntimeException e) { error="Camera frame encoding failed"; }
            }});
            camera.startPreview(); error = null;
        } catch (Exception e) { release(); throw e; }
    }
    byte[] frame(final boolean photograph) throws Exception {
        lastAccess = SystemClock.elapsedRealtime();
        final CountDownLatch done = new CountDownLatch(1);
        final byte[][] result = new byte[1][]; final String[] failure = new String[1];
        handler.post(new Runnable() { public void run() {
            try {
                open();
                if (!photograph) { done.countDown(); return; }
                handler.postDelayed(new Runnable() { public void run() {
                    try { camera.takePicture(null,null,new Camera.PictureCallback() { public void onPictureTaken(byte[] data, Camera owner) {
                        result[0] = data;
                        try { owner.startPreview(); } catch (RuntimeException e) { release(); }
                        done.countDown();
                    }}); } catch (Exception e) { failure[0]="CAMERA_BUSY"; done.countDown(); }
                }}, 500);
            } catch (Exception e) { failure[0]="CAMERA_BUSY: " + e.getClass().getSimpleName(); done.countDown(); }
        }});
        if (!done.await(8, TimeUnit.SECONDS)) throw new IOException("Camera timeout");
        if (failure[0] != null) throw new IOException(failure[0]);
        if (photograph) { if(result[0]==null)throw new IOException("Camera returned no JPEG"); return result[0]; }
        long deadline = SystemClock.elapsedRealtime()+3000;
        while (latest == null && error == null && SystemClock.elapsedRealtime()<deadline) Thread.sleep(30);
        if (latest == null) throw new IOException(error == null ? "Camera preview unavailable" : error);
        return latest;
    }
    void idle() { if(SystemClock.elapsedRealtime()-lastAccess>10000) handler.post(new Runnable(){public void run(){release();}}); }
    void close() { handler.post(new Runnable(){public void run(){release();thread.quit();}}); }
    private void release() {
        if(camera!=null){try{camera.setPreviewCallback(null);camera.stopPreview();}catch(Exception ignored){}camera.release();camera=null;}
        if(texture!=null){texture.release();texture=null;} latest=null;
    }
}
