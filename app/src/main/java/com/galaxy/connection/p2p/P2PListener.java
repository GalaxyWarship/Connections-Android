package com.galaxy.connection.p2p;

import java.io.IOException;

public interface P2PListener {

    void onNewConnection(String id);

    void onClosed(String id);

    void onConnectionFailed(String id);

    void onDataGot(String id, byte[] bytes, int index, int length) throws IOException;
}
