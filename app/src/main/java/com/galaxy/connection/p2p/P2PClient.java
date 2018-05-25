package com.galaxy.connection.p2p;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class P2PClient {

    private final String socketAddress;
    private Socket client;
    private String clientId;
    private volatile boolean connected;
    private volatile boolean started;

    public P2PClient(String socketAddress) {
        this.socketAddress = socketAddress;
    }

    public void connect() throws URISyntaxException {
        if (started)
            return;

        started = true;

        client = IO.socket(socketAddress);

        client.on("id", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                clientId = (String) args[0];
//                connected = true;
//                semaphore.release();
            }
        });

        client.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                connected = true;
            }
        });

        client.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                connected = false;
            }
        });

        client.connect();
//        if (!client.connected()) {
//            semaphore = new Semaphore(0);
//
//            semaphore.tryAcquire(15, TimeUnit.SECONDS);
//
//            if (!client.connected()) {
//                throw new TimeoutException();
//            }
//        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;

            clientId = null;
        }
    }

    public Socket getClient() {
        return client;
    }

    public String getClientId() {
        return clientId;
    }
}
