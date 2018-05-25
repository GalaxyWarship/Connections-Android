package yang.webrtcdataclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvContent;
    private EditText editText;

    private StringBuilder sb = new StringBuilder();
    private RecyclerView recyclerView;
    private ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = (TextView) findViewById(R.id.tv_content);
        tvContent.setMovementMethod(new ScrollingMovementMethod());
        editText = (EditText) findViewById(R.id.edit);
        findViewById(R.id.btn_init).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this );
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        adapter = new ListAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void showSendMessage(String message) {
        sb.append("Send:").append(message).append("\n");
        tvContent.setText(sb.toString());
    }

    private void showReceiveMessage(String message) {
        sb.append("Receive:").append(message).append("\n");
        tvContent.setText(sb.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init:
//                rtcClient.sendInitMessage();
                showSendMessage("init");
                break;
            case R.id.btn_send:
                String message = editText.getText().toString();
//                rtcClient.sendDataMessageToAllPeer(message);
                showSendMessage(message);
                break;
            default:
                break;
        }
    }
}
