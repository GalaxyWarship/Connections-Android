package com.galaxy.connection.p2p;

import com.galaxy.connection.utils.P2PUtil;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ServerSocket {

    private final P2PManager p2pRouter;
    private final Semaphore semaphore;
    private final List<Socket> sockets;

    public ServerSocket() {
        p2pRouter = P2PUtil.p2pManager;
        semaphore = new Semaphore(0);
        sockets = new LinkedList<>();
    }

    public Socket accept() throws InterruptedException, URISyntaxException {
        p2pRouter.onAccept(this);
        semaphore.acquire();

        synchronized (sockets) {
            return sockets.remove(0);
        }
    }

    void onNewConnection(Socket socket) {
        synchronized (sockets) {
            this.sockets.add(socket);
            semaphore.release();
        }
    }
}
