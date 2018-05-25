package com.galaxy.connection.p2p;

import android.support.annotation.NonNull;

import com.galaxy.connection.utils.P2PUtil;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okio.Buffer;
import okio.Pipe;

public class Socket {

    private final String target;
    private volatile Semaphore semaphore;
    private P2PManager p2pRouter;
    private SocketOutputStream outputStream;
    private SocketInputStream inputStream;
    private volatile boolean connected;
    private volatile boolean closed;

    public Socket(String target) {
        this.target = target;
        p2pRouter = P2PUtil.p2pManager;
    }

    public void connect() throws TimeoutException, SocketException, JSONException, URISyntaxException {
        p2pRouter.connect(this);
        semaphore = new Semaphore(0);
        try {
            if (!semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!connected) {
            throw new ConnectException();
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void close() {
        if (closed)
            return;

        closed = true;
        p2pRouter.close(this);
    }

    public boolean isClosed() {
        return closed;
    }

    void onConnected() {
        connected = true;

        outputStream = new SocketOutputStream();
        inputStream = new SocketInputStream();
        if (semaphore != null) {
            semaphore.release();
        }
    }

    void onConnectionFailed() {
        if (semaphore != null) {
            semaphore.release();
        }
        closeStream();

        close();
    }

    void onDisconnected() {
        if (semaphore != null) {
            semaphore.release();
        }
        closeStream();

        close();
    }

    private void closeStream() {
        try {
            inputStream.pipe.source().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            inputStream.pipe.sink().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void onDataGot(byte[] data, int index, int len) throws IOException {
        Buffer buffer = new Buffer();
        buffer.write(data, index, len);
        inputStream.pipe.sink().write(buffer, buffer.size());
    }

    String getTarget() {
        return target;
    }

    private class SocketOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            byte[] bytes = new byte[1];
            bytes[0] = (byte) b;
            write(bytes, 0, 1);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            p2pRouter.send(target, b, off, len);
        }
    }

    public static class SocketInputStream extends InputStream {
        private Buffer buffer;
        Pipe pipe;

        public SocketInputStream() {
            this.pipe = new Pipe(1024 * 1024 * 10);
            pipe.source().timeout().timeout(15, TimeUnit.SECONDS);
            buffer = new Buffer();
        }

        @Override
        public int read() throws IOException {
            if (buffer.size() > 0) {
                return buffer.readByte();
            }else {
                long read = pipe.source().read(buffer, 1024 * 8);
                if (read < 1)
                    return -1;
            }

            return buffer.readByte();
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (buffer.size() > 0) {
                return buffer.read(b, off, len);
            } else {
                long read = pipe.source().read(buffer, 1024 * 8);
                if (read < 1)
                    return -1;
            }

            return buffer.read(b, off, len);
        }

        public void timeout(int second) {
            pipe.source().timeout().timeout(second, TimeUnit.SECONDS);
        }

        public void resetTimeout() {
            pipe.source().timeout().timeout(15, TimeUnit.SECONDS);
        }
    }
}
