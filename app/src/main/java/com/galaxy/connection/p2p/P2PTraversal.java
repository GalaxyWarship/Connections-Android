package com.galaxy.connection.p2p;

import android.support.annotation.NonNull;

import com.galaxy.connection.utils.P2PUtil;
import com.zenjoy.logger.ZenLogger;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import io.socket.emitter.Emitter;
import okio.Buffer;
import io.socket.client.Socket;

public class P2PTraversal {

    private static final String TAG = "P2PProtocol";
    public static final int CLOSE = 1;
    public static final int PACKAGE_MAX_SIZE = 1024;
    public static final int HEADER_SIZE = 8;
    private final P2PClient client;

    private PeerConnectionFactory factory;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private Map<String, Peer> peers;
    private MediaConstraints constraints = new MediaConstraints();
    private P2PListener listener;
    private volatile boolean connect;

    public P2PTraversal(P2PClient client, P2PListener listener) {
        this.client = client;
        this.listener = listener;
        factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
        for (String url : P2PUtil.iceServerUrlList) {
            iceServers.add(new PeerConnection.IceServer(url));
        }
//        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
//        iceServers.add(new PeerConnection.IceServer("stun:stunserver.org:3478"));
        peers = Collections.synchronizedMap(new HashMap<String, Peer>());
    }

    public void connect(String id) throws JSONException {
        doConnect();
//        sendMessage(id, "connect", null);
        JSONObject message = new JSONObject();
        message.put("to", id);
//        message.put("from", client.getClientId());
        client.getClient().emit("init", message);
    }

    public void bind() {
        doConnect();
    }

    public void sendTo(String id, byte[] data, int index, int len) {
        Peer peer = peers.get(id);
        peer.sendDataChannelMessage(data, index, len);
    }

    public void close(String id) {
        Peer removed = peers.remove(id);
        if (removed != null) {
            removed.reset();
//            removed.release();
        }
    }

    private void doConnect() {
        if (connect)
            return;

        connect = true;

        client.getClient().on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                ZenLogger.et("P2PProtocol", "messageListener call data : " + data);
                try {
                    String from = data.optString("from");
                    String type = data.getString("type");
                    JSONObject payload = null;
                    if (!type.equals("init")) {
                        payload = data.optJSONObject("payload");
                    }
                    switch (type) {
                        case "init":
                            onReceiveConnect(from);
                            break;
                        case "offer":
                            onReceiveOffer(from, payload);
                            break;
                        case "answer":
                            onReceiveAnswer(from, payload);
                            break;
                        case "candidate":
                            onReceiveCandidate(from, payload);
                            break;
                        default:
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void close() {
        factory.dispose();
        for (Peer peer : peers.values()) {
            if (peer != null) {
                peer.release();
                peer = null;
            }
        }
    }

    private void onReceiveConnect(String fromUid) {
        ZenLogger.et(TAG, "onReceiveInit fromUid:" + fromUid);
        Peer peer = getPeer(fromUid);
        peer.pc.createOffer(peer, constraints);
    }

    public void onReceiveOffer(String fromUid, JSONObject payload) {
        ZenLogger.et(TAG, "onReceiveOffer uid:" + fromUid + " data:" + payload);
        try {
            Peer peer = getPeer(fromUid);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, constraints);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onReceiveAnswer(String fromUid, JSONObject payload) {
        ZenLogger.et(TAG, "onReceiveAnswer uid:" + fromUid + " data:" + payload);
        try {
            Peer peer = getPeer(fromUid);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onReceiveCandidate(String fromUid, JSONObject payload) {
        ZenLogger.et(TAG, "onReceiveCandidate uid:" + fromUid + " data:" + payload);
        try {
            Peer peer = getPeer(fromUid);
            if (peer.pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                peer.pc.addIceCandidate(candidate);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);

        message.put("payload", payload);

//        message.put("from", client.getClientId());
        client.getClient().emit(Socket.EVENT_MESSAGE, message);
    }

    private Peer getPeer(String from) {
        Peer peer = peers.get(from);
        if (peer == null) {
            peer = new Peer(from);
            peers.put(from, peer);
        }
        return peer;
    }

    private class OutputStreamImpl extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        /**
         * mx type [padding] [data]
         *
         *
         *
         */

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {

            while (len > 0) {
                int l = Math.min(PACKAGE_MAX_SIZE, len);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(l + HEADER_SIZE);
                outputStream.write('m');//magic number
                outputStream.write('x');

                outputStream.write('d');//type
                outputStream.write('a');
                outputStream.write('t');
                outputStream.write('a');

                writeShort(outputStream, l);

                outputStream.write(b, off, l);//data

                byte[] bytes = outputStream.toByteArray();
//                peer.sendDataChannelMessage(bytes, 0, bytes.length);

                off += l;
                len -= l;
            }
        }

        @Override
        public void close() throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(HEADER_SIZE);
            outputStream.write('m');//magic number
            outputStream.write('x');

            outputStream.write('e');//type
            outputStream.write('n');
            outputStream.write('d');
            outputStream.write(0);
            writeShort(outputStream, 0);

            byte[] bytes = outputStream.toByteArray();
//            peer.sendDataChannelMessage(bytes, 0, bytes.length);
            super.close();
        }

        private void writeShort(OutputStream outputStream, int v) throws IOException {
            outputStream.write((v >>> 8) & 0xFF);
            outputStream.write((v >>> 0) & 0xFF);
        }
    }

    private class InputStreamImpl extends InputStream {

        private Buffer buffer;

        public InputStreamImpl() {
            buffer = new Buffer();
        }

        @Override
        public int read() throws IOException {
            if (buffer.size() > 0) {
                return buffer.readByte();
            }else {
//                long read = pipe.source().read(buffer, 1024 * 8);
//                if (read < 1)
//                    return -1;
            }

            return buffer.readByte();
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (buffer.size() > 0) {
                return buffer.read(b, off, len);
            } else {
//                long read = pipe.source().read(buffer, 1024 * 8);
//                if (read < 1)
//                    return -1;
            }

            return buffer.read(b, off, len);
        }
    }

    private class Peer implements SdpObserver, PeerConnection.Observer, DataChannel.Observer {
        PeerConnection pc;
        String id;
        DataChannel dc;
        boolean callbackNew;
        private volatile boolean released;
        private boolean reset;

        public Peer(String id) {
            ZenLogger.et(TAG, "new Peer: " + id);
            this.pc = factory.createPeerConnection(
                    iceServers, //ICE服务器列表
                    constraints, //MediaConstraints
                    this); //Context
            this.id = id;

            /*
            DataChannel.Init 可配参数说明：
            ordered：是否保证顺序传输；
            maxRetransmitTimeMs：重传允许的最长时间；
            maxRetransmits：重传允许的最大次数；
             */
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            dc = pc.createDataChannel("dataChannel", init);
        }

        public void sendDataChannelMessage(String message) {
            byte[] msg = message.getBytes();
            DataChannel.Buffer buffer = new DataChannel.Buffer(
                    ByteBuffer.wrap(msg),
                    false);
            dc.send(buffer);
        }

        private void sendDataChannelMessage(byte[] msg, int offset, int length) {
//            byte[] msg = message.getBytes();
            DataChannel.Buffer buffer = new DataChannel.Buffer(
                    ByteBuffer.wrap(msg, offset, length),
                    true);
            dc.send(buffer);
        }

        public void release() {
            ZenLogger.et(TAG, "release");
            if (released)
                return;

            released = true;

//            reset();

//            pc.dispose();
//            dc.dispose();
            dc.close();
        }

        public void reset() {
//            release();

            if (reset)
                return;

            reset = true;

            dc.unregisterObserver();
            pc.dispose();
//            pc.close();
        }

        //SdpObserver-------------------------------------------------------------------------------

        @Override
        public void onCreateSuccess(SessionDescription sdp) {
            ZenLogger.et(TAG, "onCreateSuccess: " + sdp.description);
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);
                sendMessage(id, sdp.type.canonicalForm(), payload);
                pc.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }

        //DataChannel.Observer----------------------------------------------------------------------

        @Override
        public void onBufferedAmountChange(long l) {

        }

        @Override
        public void onStateChange() {
            ZenLogger.et(TAG, "onDataChannel onStateChange:" + dc.state());

            if (dc.state() == DataChannel.State.OPEN && !callbackNew) {
                callbackNew = true;
                listener.onNewConnection(id);
            }

            if (dc.state() == DataChannel.State.CLOSED) {
                release();
            }
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            ZenLogger.et(TAG, "onDataChannel onMessage : " + buffer);
            ByteBuffer data = buffer.data;

            byte[] bytes = new byte[data.capacity()];
            data.get(bytes);

            try {
                listener.onDataGot(id, bytes, 0, bytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //PeerConnection.Observer-------------------------------------------------------------------

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            ZenLogger.et(TAG, "onIceConnectionChange : " + iceConnectionState.name());
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                Peer removed = peers.remove(id);
            } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
//                release();
                listener.onConnectionFailed(id);
            }else if (iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
//                Peer removed = peers.remove(id);
//                if (removed != null) {
//                    removed.release();
//                }
//                release();

                listener.onClosed(id);
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", candidate.sdpMLineIndex);
                payload.put("id", candidate.sdpMid);
                payload.put("candidate", candidate.sdp);
                sendMessage(id, "candidate", payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            ZenLogger.et(TAG, "onDataChannel label:" + dataChannel.label());
            dataChannel.registerObserver(this);
        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

    }
}
