package yang.webrtcdataclient;

import android.app.Application;

import org.webrtc.PeerConnectionFactory;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(this)
                .createInitializationOptions());
    }
}
