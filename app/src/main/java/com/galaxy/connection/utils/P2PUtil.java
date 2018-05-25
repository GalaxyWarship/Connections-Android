package com.galaxy.connection.utils;

import android.app.Application;

import com.galaxy.connection.p2p.P2PManager;

import org.webrtc.PeerConnectionFactory;

import java.util.LinkedList;
import java.util.List;

public class P2PUtil {
    public static P2PManager p2pManager;
    public static List<String> iceServerUrlList = new LinkedList<>();

    public static class P2PBuilder {

        private Application application;
        private String messageServer;

        public P2PBuilder(Application application) {
            this.application = application;
        }

        public P2PBuilder messageServer(String messageServer) {
            this.messageServer = messageServer;
            return this;
        }

        public P2PBuilder iceServerUrl(String url) {
            iceServerUrlList.add(url);
            return this;
        }

        public void build() {
            PeerConnectionFactory.initialize(PeerConnectionFactory
                    .InitializationOptions
                    .builder(application)
                    .createInitializationOptions());

            p2pManager = new P2PManager(messageServer);
        }
    }
}
