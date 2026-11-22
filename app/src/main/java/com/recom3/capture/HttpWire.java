package com.recom3.capture;
import java.io.*;
import java.util.*;

/** Bounded HTTP/1.1 request reader; one request per connection, no chunked input. */
public final class HttpWire {
    public String method, target; public byte[] body;
    public final Map<String,String> headers = new HashMap<String,String>();
    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value;
        while ((value=in.read())!=-1) {
            if (value==10) return new String(out.toByteArray(),"US-ASCII").replace("\r", "");
            if (out.size()>=4096) throw new IOException("HTTP line too long"); out.write(value);
        }
        throw new EOFException();
    }
    public static HttpWire read(InputStream in) throws IOException {
        HttpWire request = new HttpWire();
        String[] first=line(in).split(" ");
        if(first.length!=3 || !first[2].equals("HTTP/1.1") || first[1].length()>1024) throw new IOException("Invalid request");
        request.method=first[0];request.target=first[1]; int total=0;
        while(true){String next=line(in);total+=next.length();if(total>8192)throw new IOException("Headers too large");if(next.length()==0)break;
            int colon=next.indexOf(':');if(colon<1)throw new IOException("Invalid header");String key=next.substring(0,colon).toLowerCase(Locale.US);
            if(request.headers.containsKey(key))throw new IOException("Duplicate header");request.headers.put(key,next.substring(colon+1).trim());}
        if(request.headers.containsKey("transfer-encoding"))throw new IOException("Chunked input unsupported");
        int length=0;try{if(request.headers.containsKey("content-length"))length=Integer.parseInt(request.headers.get("content-length"));}catch(NumberFormatException e){throw new IOException("Invalid length");}
        if(length<0||length>4096)throw new IOException("Body too large");request.body=new byte[length];
        int offset=0;while(offset<length){int n=in.read(request.body,offset,length-offset);if(n<0)throw new EOFException();offset+=n;}
        return request;
    }
    public static void respond(OutputStream out,int code,String type,byte[] data) throws IOException {
        String reason=code==200?"OK":code==401?"Unauthorized":code==429?"Too Many Requests":code==404?"Not Found":"Service Unavailable";
        out.write(("HTTP/1.1 "+code+" "+reason+"\r\nContent-Type: "+type+"\r\nContent-Length: "+data.length+"\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));out.write(data);out.flush();
    }
}
