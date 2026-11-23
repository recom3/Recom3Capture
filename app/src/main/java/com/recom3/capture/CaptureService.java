package com.recom3.capture;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

public final class CaptureService extends Service {
    public static volatile CaptureService current;
    public static final int PORT=18884;
    private ServerSocket server; private CameraFeed camera; private String helperKey, token;
    volatile String code, address="", lastError=""; private int failures; private long failureStart;
    private final Handler handler = new Handler();
    private final ExecutorService workers = new ThreadPoolExecutor(3,3,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(6));
    volatile String displayState="Checking screen access";
    volatile boolean displayReady;
    private volatile boolean stopping;
    private final AtomicBoolean activatingDisplay=new AtomicBoolean();
    private final ExecutorService displayWorker=Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wake;
    private WifiManager.WifiLock wifi;
    static String randomHex() {byte[] b=new byte[16];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(java.util.Locale.US,"%02x",v&255));return s.toString();}
    @Override public void onCreate(){
        super.onCreate(); current=this;
        try {
            File keyFile=new File(getFilesDir(),"helper.auth");
            if(keyFile.exists()){BufferedReader reader=new BufferedReader(new FileReader(keyFile));try{helperKey=reader.readLine();}finally{reader.close();}}
            if(helperKey==null||!helperKey.matches("[a-f0-9]{32}")){helperKey=randomHex();FileOutputStream out=openFileOutput("helper.auth",MODE_PRIVATE);try{out.write((helperKey+"\n").getBytes("UTF-8"));}finally{out.close();}}
            token=randomHex();code=String.format(java.util.Locale.US,"%08d",new SecureRandom().nextInt(100000000));
            JSONObject setup=new JSONObject();setup.put("code",code);setup.put("port",PORT);
            FileOutputStream out=openFileOutput("session.json",MODE_PRIVATE);try{out.write(setup.toString().getBytes("UTF-8"));}finally{out.close();}
            Notification notification=new Notification(android.R.drawable.presence_video_online,"Recom3 Capture active",System.currentTimeMillis());
            Intent open=new Intent(this,CaptureActivity.class);notification.setLatestEventInfo(this,"Recom3 Capture","Open to view connection or stop",PendingIntent.getActivity(this,0,open,0));
            startForeground(41,notification);
            wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"recom3:capture");wake.acquire();
            wifi=((WifiManager)getSystemService(WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"recom3:capture");wifi.acquire();
            camera=new CameraFeed();server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress(PORT));
            address=wifiAddress();
            new Thread(new Runnable(){public void run(){accept();}},"capture-http").start();
            handler.postDelayed(maintenance,5000);
            activateDisplay(getSharedPreferences("screen",MODE_PRIVATE).getBoolean("start_on_open",false));
        } catch(Exception e){lastError=e.getClass().getSimpleName();stopSelf();}
    }
    private final Runnable maintenance=new Runnable(){public void run(){if(camera!=null)camera.idle();address=wifiAddress();if(!displayReady)activateDisplay(false);handler.postDelayed(this,5000);}};
    private String wifiAddress(){int ip=((WifiManager)getSystemService(WIFI_SERVICE)).getConnectionInfo().getIpAddress();return (ip&255)+"."+((ip>>8)&255)+"."+((ip>>16)&255)+"."+((ip>>24)&255);}
    boolean displayStarting(){return activatingDisplay.get();}

    /** Serialized on its own worker so a root prompt cannot block HTTP/camera requests or the UI. */
    public void activateDisplay(final boolean allowExistingRoot) {
        if(stopping || !activatingDisplay.compareAndSet(false,true)) return;
        try {
            displayWorker.execute(new Runnable(){public void run(){
                java.lang.Process client=null;
                try {
                    if(probeDisplay()){displayState="Screen ready";return;}
                    if(!allowExistingRoot){displayState="Screen needs activation or PC setup";return;}
                    String su=null;
                    for(String path:new String[]{"/system/xbin/su","/system/bin/su"}){
                        if(new File(path).canExecute()){su=path;break;}
                    }
                    if(su==null){displayState="No system access: PC setup required";return;}
                    displayState="Waiting for root approval (30 seconds)";
                    String command=HelperLaunch.command(getPackageCodePath(),new File(getFilesDir(),"helper.auth").getAbsolutePath());
                    client=new ProcessBuilder(su,"-c",command).start();
                    client.getOutputStream().close();
                    // The fixed command redirects helper I/O, including its key, away from these pipes.
                    if(HelperLaunch.awaitExit(client,30000)!=0){
                        displayState="Root start failed: PC setup required";return;
                    }
                    long deadline=SystemClock.elapsedRealtime()+8000;
                    while(!stopping && SystemClock.elapsedRealtime()<deadline){
                        if(probeDisplay()){
                            if(stopping)return;
                            getSharedPreferences("screen",MODE_PRIVATE).edit().putBoolean("start_on_open",true).commit();
                            displayState="Screen ready; enabled on app launch";return;
                        }
                        Thread.sleep(250);
                    }
                    displayState="Screen unavailable: use PC setup";
                } catch(InterruptedException interrupted){
                    Thread.currentThread().interrupt();
                } catch(Exception failed){
                    displayState="Screen activation failed: use PC setup";
                } finally {
                    if(client!=null){
                        client.destroy();
                        try{client.getInputStream().close();}catch(IOException ignored){}
                        try{client.getErrorStream().close();}catch(IOException ignored){}
                    }
                    activatingDisplay.set(false);
                }
            }});
        } catch(RejectedExecutionException stopped){activatingDisplay.set(false);}
    }

    private boolean probeDisplay(){
        if(stopping)return false;
        try{screen(false);return true;}catch(Exception unavailable){return false;}
    }

    public byte[] screen(boolean png) throws Exception {
        try {
            byte[] bytes;
            if(checkCallingOrSelfPermission("android.permission.READ_FRAME_BUFFER")==PackageManager.PERMISSION_GRANTED){
                try{bytes=directScreen(png);}
                catch(Exception directUnavailable){bytes=helperScreen(png);}
            } else bytes=helperScreen(png);
            displayReady=true;
            if(!activatingDisplay.get())displayState="Screen ready";
            return bytes;
        } catch(Exception unavailable){displayReady=false;throw unavailable;}
    }

    private byte[] directScreen(boolean png) throws Exception {
        Bitmap image=ScreenHelper.screenshot();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try{
            if(!image.compress(png?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,png?100:75,output))
                throw new IOException("Display encoding failed");
        } finally{image.recycle();}
        byte[] bytes=output.toByteArray();
        if(bytes.length<1||bytes.length>1048576)throw new IOException("Invalid display frame");
        return bytes;
    }
    private byte[] helperScreen(boolean png) throws Exception {
        LocalSocket socket=new LocalSocket();
        try{socket.connect(new LocalSocketAddress(ScreenHelper.SOCKET));socket.setSoTimeout(5000);
            DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeUTF(helperKey);out.writeByte(png?1:0);out.flush();
            DataInputStream in=new DataInputStream(socket.getInputStream());int length=in.readInt();if(length<1||length>1048576)throw new IOException("Invalid display frame");byte[] data=new byte[length];in.readFully(data);return data;
        }finally{socket.close();}
    }
    public void delayedScreenshot(){deleteFile("last-screen.png");handler.postDelayed(new Runnable(){public void run(){workers.execute(new Runnable(){public void run(){try{byte[] data=screen(true);FileOutputStream out=openFileOutput("last-screen.png",MODE_PRIVATE);try{out.write(data);}finally{out.close();}lastError="Screenshot saved";}catch(Exception e){lastError="Display helper required";}}});}},5000);}
    private void accept(){while(server!=null&&!server.isClosed()){try{final Socket socket=server.accept();socket.setSoTimeout(10000);try{workers.execute(new Runnable(){public void run(){serve(socket);}});}catch(RejectedExecutionException e){socket.close();}}catch(IOException ignored){break;}}}
    private synchronized boolean pair(String supplied){long now=SystemClock.elapsedRealtime();if(now-failureStart>60000){failureStart=now;failures=0;}if(failures>=8)return false;if(!code.equals(supplied)){failures++;return false;}return true;}
    private void serve(Socket socket){
        try {
            HttpWire r=HttpWire.read(socket.getInputStream());byte[] data;String type="application/json";int status=200;
            if("/pair".equals(r.target)&&"POST".equals(r.method)){
                if(pair(new String(r.body,"UTF-8")))data=("{\"token\":\""+token+"\"}").getBytes("UTF-8");
                else {status=failures>=8?429:401;data="{\"error\":\"Pairing rejected\"}".getBytes("UTF-8");}
            } else if(!("Bearer "+token).equals(r.headers.get("authorization"))){status=401;data="{\"error\":\"Authentication required\"}".getBytes("UTF-8");}
            else if("GET".equals(r.method)&&"/status".equals(r.target)){
                boolean screen=false;try{screen(false);screen=true;}catch(Exception ignored){}
                data=("{\"screen\":"+screen+",\"camera\":"+(android.hardware.Camera.getNumberOfCameras()>0)+",\"protocol\":1}").getBytes("UTF-8");
            } else if(("GET".equals(r.method)&&r.target.startsWith("/frame?source="))||("POST".equals(r.method)&&r.target.startsWith("/photo?source="))){
                boolean photo=r.target.startsWith("/photo");String source=r.target.substring(r.target.indexOf('=')+1);
                if("screen".equals(source)){data=screen(photo);type=photo?"image/png":"image/jpeg";}
                else if("camera".equals(source)){data=camera.frame(photo);type="image/jpeg";}
                else throw new IOException("Unsupported source");
            } else if("GET".equals(r.method)&&"/last-screen".equals(r.target)){
                FileInputStream in=openFileInput("last-screen.png");ByteArrayOutputStream out=new ByteArrayOutputStream();try{byte[] b=new byte[8192];int n;while((n=in.read(b))>0){if(out.size()+n>1048576)throw new IOException("File too large");out.write(b,0,n);}}finally{in.close();}data=out.toByteArray();type="image/png";
            } else {status=404;data="{\"error\":\"Unknown endpoint\"}".getBytes("UTF-8");}
            HttpWire.respond(socket.getOutputStream(),status,type,data);
        }catch(Exception e){try{HttpWire.respond(socket.getOutputStream(),503,"application/json","{\"error\":\"Capture unavailable; check helper or camera ownership\"}".getBytes("UTF-8"));}catch(Exception ignored){}}
        finally{try{socket.close();}catch(IOException ignored){}}
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){return START_NOT_STICKY;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){stopping=true;displayWorker.shutdownNow();current=null;handler.removeCallbacksAndMessages(null);try{if(server!=null)server.close();}catch(IOException ignored){}workers.shutdownNow();if(camera!=null)camera.close();if(wifi!=null&&wifi.isHeld())wifi.release();if(wake!=null&&wake.isHeld())wake.release();stopForeground(true);super.onDestroy();}
}
