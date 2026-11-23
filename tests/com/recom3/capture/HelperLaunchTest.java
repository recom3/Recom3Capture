package com.recom3.capture;

import java.io.*;

public final class HelperLaunchTest {
    private static int checks;
    private static void expect(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    private static void reject(String apk,String key)throws Exception{
        try{HelperLaunch.command(apk,key);throw new AssertionError("Unsafe path accepted");}
        catch(IOException expected){checks++;}
    }
    private static final class Child extends Process {
        final boolean pending;final int exit;boolean destroyed;
        Child(boolean pending,int exit){this.pending=pending;this.exit=exit;}
        public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
        public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}
        public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}
        public int waitFor(){return exit;}
        public int exitValue(){if(pending)throw new IllegalThreadStateException();return exit;}
        public void destroy(){destroyed=true;}
    }
    public static void main(String[] args)throws Exception{
        String apk="/data/app/com.recom3.capture-1.apk";
        String key="/data/data/com.recom3.capture/files/helper.auth";
        String command=HelperLaunch.command(apk,key);
        expect(command.contains("com.recom3.capture.ScreenHelper < "+key));
        expect(command.contains("export CLASSPATH="+apk));
        expect(command.contains("trap '' HUP"));
        expect(command.endsWith("> /dev/null 2>&1 &"));
        expect(!command.contains("setenforce")&&!command.contains("chmod")&&!command.contains("adb"));
        reject("/data/app/capture.apk;id",key);
        reject("/data/app/$(id).apk",key);
        reject("/data/app/`id`.apk",key);
        reject("/data/app/capture'bad.apk",key);
        reject("/data/app/capture apk",key);
        reject("/data/app/capture.apk\nid",key);
        reject("relative.apk",key);
        reject("/data/../capture.apk",key);
        reject(apk,key+";id");
        reject(apk,null);
        Child completed=new Child(false,0);
        expect(HelperLaunch.awaitExit(completed,100)==0);
        expect(completed.destroyed);
        Child denied=new Child(false,1);
        expect(HelperLaunch.awaitExit(denied,100)==1);
        expect(denied.destroyed);
        Child timedOut=new Child(true,0);
        try{HelperLaunch.awaitExit(timedOut,20);throw new AssertionError("Unbounded root wait");}
        catch(IOException expected){checks++;}
        expect(timedOut.destroyed);
        Child interrupted=new Child(true,0);
        Thread.currentThread().interrupt();
        try{HelperLaunch.awaitExit(interrupted,10000);throw new AssertionError("Ignored cancellation");}
        catch(InterruptedException expected){checks++;}
        finally{Thread.interrupted();}
        expect(interrupted.destroyed);
        System.out.println("Helper launch boundary/timeout checks passed: "+checks);
    }
}
