package com.recom3.capturetests;
import android.app.Instrumentation;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import org.json.*;

/** Real device test: HTTP over the existing hotspot and the production phone MP4 encoder. */
public final class CaptureInstrumentation extends Instrumentation {
    private Bundle args;private String endpoint,token;
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    private byte[] request(String method,String path,byte[] body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint+path).openConnection();
        try{c.setConnectTimeout(5000);c.setReadTimeout(12000);c.setRequestMethod(method);if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
            if(body!=null){c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);OutputStream out=c.getOutputStream();out.write(body);out.close();}
            if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0){if(out.size()+n>1572864)throw new IOException("Oversized media");out.write(b,0,n);}in.close();return out.toByteArray();
        }finally{c.disconnect();}
    }
    @Override public void onStart(){Bundle result=new Bundle();Intent session=new Intent().setClassName(getTargetContext(),"com.recom3.snow3.capture.CaptureSessionService");
        try{
            getTargetContext().startForegroundService(session);Thread.sleep(700);
            FileInputStream configInput=getTargetContext().openFileInput("capture-test.json");ByteArrayOutputStream configBytes=new ByteArrayOutputStream();try{byte[] chunk=new byte[1024];int n;while((n=configInput.read(chunk))>0){if(configBytes.size()+n>4096)throw new IOException("Invalid test configuration");configBytes.write(chunk,0,n);}}finally{configInput.close();}JSONObject config=new JSONObject(configBytes.toString("UTF-8"));endpoint="http://"+config.getString("host")+":18884";
            JSONObject auth=new JSONObject(new String(request("POST","/pair",config.getString("code").getBytes("UTF-8")),"UTF-8"));token=auth.getString("token");
            JSONObject caps=new JSONObject(new String(request("GET","/status",null),"UTF-8"));
            String source=args.getString("source","screen");if(!caps.getBoolean(source))throw new IOException("Source unavailable");
            File folder=new File(getTargetContext().getExternalFilesDir(null),"Capture");folder.mkdirs();
            byte[] still=request("POST","/photo?source="+source,new byte[0]);File photo=new File(folder,"validation-"+source+(source.equals("screen")?".png":".jpg"));FileOutputStream out=new FileOutputStream(photo);out.write(still);out.close();
            Class<?> type=getTargetContext().getClassLoader().loadClass("com.recom3.snow3.capture.PhoneVideo");Constructor<?> constructor=type.getDeclaredConstructor(File.class,Bitmap.class);constructor.setAccessible(true);
            Method frame=type.getDeclaredMethod("frame",Bitmap.class),finish=type.getDeclaredMethod("finish");frame.setAccessible(true);finish.setAccessible(true);
            Object encoder=null;int count=0;long start=SystemClock.elapsedRealtime();
            while(SystemClock.elapsedRealtime()-start<10000){byte[] bytes=request("GET","/frame?source="+source,null);Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap==null)throw new IOException("Cannot decode frame");
                if(encoder==null)encoder=constructor.newInstance(folder,bitmap);frame.invoke(encoder,bitmap);bitmap.recycle();count++;Thread.sleep(200);}
            File clip=(File)finish.invoke(encoder);MediaMetadataRetriever metadata=new MediaMetadataRetriever();metadata.setDataSource(clip.getPath());
            result.putString("source",source);result.putString("photo",photo.getPath());result.putString("clip",clip.getPath());result.putString("width",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));result.putString("height",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));result.putString("durationMs",metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            result.putInt("receivedFrames",count);result.putLong("elapsedMs",SystemClock.elapsedRealtime()-start);
            Bitmap sample=metadata.getFrameAtTime(3000000);if(sample==null)throw new IOException("Recorded MP4 has no decodable frame");FileOutputStream sampleOut=new FileOutputStream(new File(folder,"validation-"+source+"-video-frame.jpg"));sample.compress(Bitmap.CompressFormat.JPEG,90,sampleOut);sampleOut.close();sample.recycle();metadata.release();
            result.putString("result","PASS");finish(-1,result);
        }catch(Throwable e){Throwable reason=e instanceof InvocationTargetException?((InvocationTargetException)e).getCause():e;result.putString("result","FAIL");result.putString("error",reason.getClass().getSimpleName()+": "+reason.getMessage());finish(0,result);}
        finally{getTargetContext().stopService(session);}
    }
}