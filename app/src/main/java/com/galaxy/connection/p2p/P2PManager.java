package com.galaxy.connection.p2p;

import com.zenjoy.logger.ZenLogger;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class P2PManager implements P2PListener {
    private final P2PClient p2PClient;

    Map<String, Socket> sockets;
    ServerSocket serverSocket;
    P2PTraversal traversal;

    public P2PManager(String server) {
        sockets = Collections.synchronizedMap(new HashMap<String, Socket>());
        p2PClient = new P2PClient(server);
        traversal = new P2PTraversal(p2PClient, this);
    }

    @Override
    public void onNewConnection(String from) {
        ZenLogger.e("onNewConnection %s", from);
        Socket p2PSocket = matchSocket(from);
        if (p2PSocket != null) {
            p2PSocket.onConnected();
            return;
        }

        if (serverSocket == null) {
            throw new RuntimeException();
        }

        Socket newSocket = new Socket(from);
        sockets.put(from, newSocket);

        newSocket.onConnected();

        serverSocket.onNewConnection(newSocket);
    }

    @Override
    public void onClosed(String id) {
        ZenLogger.e("onDisconnected %s", id);
        Socket p2PSocket = matchSocket(id);
        if (p2PSocket != null) {
            p2PSocket.onDisconnected();
        }
    }

    @Override
    public void onConnectionFailed(String id) {
        ZenLogger.e("onConnectionFailed %s", id);
        Socket p2PSocket = matchSocket(id);
        if (p2PSocket != null) {
            p2PSocket.onConnectionFailed();
        }
    }

    @Override
    public void onDataGot(String from, byte[] data, int index, int len) throws IOException {
        ZenLogger.e("onDataGot %s", from);
        Socket p2PSocket = matchSocket(from);
        if (p2PSocket != null) {
            p2PSocket.onDataGot(data, index, len);
        }
    }

    void onAccept(ServerSocket socket) throws URISyntaxException {
        this.serverSocket = socket;
        p2PClient.connect();
        traversal.bind();
    }

    void send(String to, byte[] data, int index, int len) {
        ZenLogger.e("send %s", to);
        traversal.sendTo(to, data, index, len);
    }

    void connect(Socket socket) throws JSONException, URISyntaxException, SocketException {
        ZenLogger.e("connect %s", socket.getTarget());
        if (sockets.get(socket.getTarget()) != null) {
            throw new SocketException("connection exist.");
        }

        sockets.put(socket.getTarget(), socket);
        p2PClient.connect();
        traversal.connect(socket.getTarget());
    }

    void close(Socket socket) {
        Socket socket1 = sockets.get(socket.getTarget());
        if (socket1 != socket) {
            ZenLogger.w("close socket error");
            return;
        }

        Socket removed = sockets.remove(socket.getTarget());
        if (removed != null) {
            traversal.close(socket.getTarget());
        }
    }

    private Socket matchSocket(String id) {
        return sockets.get(id);
    }
}
