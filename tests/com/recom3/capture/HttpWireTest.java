package com.recom3.capture;
import java.io.*;

public final class HttpWireTest {
    private static int checks;
    private static void expect(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    private static HttpWire read(String input)throws Exception{return HttpWire.read(new ByteArrayInputStream(input.getBytes("US-ASCII")));}
    private static void reject(String input)throws Exception{try{read(input);throw new AssertionError("Malformed request accepted");}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        HttpWire r=read("POST /pair HTTP/1.1\r\nHost: local\r\nContent-Length: 8\r\n\r\n12345678");expect(r.method.equals("POST"));expect(r.target.equals("/pair"));expect(new String(r.body,"UTF-8").equals("12345678"));
        r=read("GET /status HTTP/1.1\r\nAuthorization: Bearer sample\r\n\r\n");expect(r.headers.get("authorization").equals("Bearer sample"));expect(r.body.length==0);
        reject("GET / HTTP/1.1\r\nAuthorization: first\r\nAuthorization: second\r\n\r\n");
        reject("POST /pair HTTP/1.1\r\nContent-Length: -1\r\n\r\n");
        reject("POST /pair HTTP/1.1\r\nContent-Length: 4097\r\n\r\n");
        reject("POST /pair HTTP/1.1\r\nContent-Length: invalid\r\n\r\n");
        reject("POST /pair HTTP/1.1\r\nContent-Length: 8\r\n\r\n123");
        reject("POST /pair HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n");
        reject("GET / HTTP/1.1\r\nnot-a-header\r\n\r\n");
        reject("GET / HTTP/1.0\r\n\r\n");
        StringBuilder oversized=new StringBuilder("GET /");for(int i=0;i<5000;i++)oversized.append('a');reject(oversized.toString());
        final byte[] bytes="POST /pair HTTP/1.1\r\nContent-Length: 8\r\n\r\n12345678".getBytes("US-ASCII");
        InputStream fragmented=new ByteArrayInputStream(bytes){@Override public synchronized int read(byte[] b,int off,int length){return super.read(b,off,Math.min(length,1));}};
        expect(new String(HttpWire.read(fragmented).body,"UTF-8").equals("12345678"));
        ByteArrayOutputStream out=new ByteArrayOutputStream();HttpWire.respond(out,401,"application/json","{}".getBytes("UTF-8"));String response=out.toString("US-ASCII");expect(response.startsWith("HTTP/1.1 401 Unauthorized\r\n"));expect(response.contains("Content-Length: 2\r\n"));expect(response.endsWith("\r\n\r\n{}"));
        System.out.println("HTTP bounds/framing checks passed: "+checks);
    }
}