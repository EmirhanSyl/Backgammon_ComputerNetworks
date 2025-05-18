package com.blackflower.backgammon_computernetworks.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 *
 * @author emirs
 */
public final class ClientNetwork implements Closeable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile Consumer<LegacyMessage> onMsg;

    public ClientNetwork(String host,int port,Consumer<LegacyMessage> onMsg)
            throws IOException{
        this.socket = new Socket(host,port);
        this.in  = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.onMsg = onMsg;
        new Thread(this::listen,"net-listener").start();
    }

    private void listen(){
        try{
            String line;
            while((line=in.readLine())!=null){
                LegacyMessage lm = LegacyMessage.decode(line);
                System.out.println("← RECV from SRV: " + lm);      // DEBUG
                onMsg.accept(lm);
            }
        }catch(IOException e){
            onMsg.accept(new LegacyMessage("ERROR").put("message","Connection lost"));
        }
    }
    public void send(LegacyMessage m){ out.println(m.encode()); }
    
    public void setConsumer(Consumer<LegacyMessage> c) { // <-- yeni
        this.onMsg = c;
    }
    
    @Override public void close() throws IOException { socket.close(); }
}