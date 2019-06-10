package tcpclienttest.com.tcpclient;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Handler mHandler;

    private TCPClient socket;

    private String ip = "192.168.150.57";
    private int port = 60124;
    private int delay = 0;

    private PowerManager.WakeLock wakeLock;

    private EditText et;
    private EditText editPort;
    private EditText editDelay;
    private Button btn;
    private Button sendBtn;
    private Button resetBtn;
    private TextView tv;
    private View touchView;

    private int index = 0;
    private ArrayList<String> actionArrayList = new ArrayList<>();

    private boolean isStart = false;

    private Timer timer = null;

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        wakeLock = null;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.PARTIAL_WAKE_LOCK, "TCPClient");

        et = (EditText) findViewById(R.id.editText01);
        editPort = (EditText) findViewById(R.id.editText02);
        editDelay = (EditText) findViewById(R.id.editText03);
        btn = (Button) findViewById(R.id.button01);
        sendBtn = (Button) findViewById(R.id.button02);
        sendBtn.setVisibility(View.INVISIBLE);
        resetBtn = (Button) findViewById(R.id.button03);
        resetBtn.setVisibility(View.INVISIBLE);
        tv = (TextView) findViewById(R.id.textView01);
        touchView = (View) findViewById(R.id.touchLayout);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case TCPClient.MessageTypeClass.SIMSOCK_DATA:
                        setConnectedMessage("Connected Server");
                        break;

                    case TCPClient.MessageTypeClass.SIMSOCK_CONNECTED:
                        setConnectedMessage("Connected Server");
                        String isConnect = (String) inputMessage.obj;
                        Log.d("_LOG_", "isConncet: " + isConnect);
                        if (isConnect == "1") {
                            setConnectedMessage("Connected Server");
                        }
                        else {
                            setConnectedMessage("Connected Fail");
                            socket.setPaused();
                            socket.interrupt();
                            btn.setText("CONNECT");
                            isStart = false;
                            socket.stopTimer();
                            sendBtn.setVisibility(View.INVISIBLE);
                            resetBtn.setVisibility(View.INVISIBLE);
                            actionArrayList.clear();
                        }
                        break;

                    case TCPClient.MessageTypeClass.SIMSOCK_DISCONNECTED:
                        setConnectedMessage("Disconnected");
                        break;
                }
            }
        };

        btn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (isStart) {
                    if (socket != null) {
                        socket.setPaused();
                        socket.interrupt();
                        btn.setText("CONNECT");
                        isStart = false;
                        socket.stopTimer();
                        sendBtn.setVisibility(View.INVISIBLE);
                        resetBtn.setVisibility(View.INVISIBLE);
                        actionArrayList.clear();
                    }
                } else {
                    if (!et.getText().toString().isEmpty())
                        ip = et.getText().toString();
                    if (!editPort.getText().toString().isEmpty())
                        port = Integer.parseInt(editPort.getText().toString());
                    if (!editDelay.getText().toString().isEmpty())
                        delay = Integer.parseInt(editDelay.getText().toString());
                    Log.d("__LOG_", "ip: " + ip + " / port: " + port + " / delay: " + delay);
                    socket = new TCPClient(ip, port, mHandler, MainActivity.this);
                    socket.start();
                    btn.setText("DISCONNECT");
                    isStart = true;
                    sendBtn.setVisibility(View.VISIBLE);
                    resetBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        sendBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStart) {
                    if (actionArrayList != null) {
                        delay = Integer.parseInt(editDelay.getText().toString());
                        if (delay > 0) {
                            TimerTask tt = new TimerTask() {
                                @Override
                                public void run() {
                                    if (index < actionArrayList.size()) {
                                        socket.outPrintln(actionArrayList.get(index++));
                                    }
                                    else {
                                        index = 0;
                                        stopTimer();
                                    }
                                }
                            };
                            if (timer == null) {
                                timer = new Timer();
                                timer.schedule(tt, 0, delay);
                            }
                        } else {
                            for (int i=0 ; i<actionArrayList.size() ; i++) {
                                socket.outPrintln(actionArrayList.get(i));
                            }
                        }
                    }
                }
            }
        });

        resetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionArrayList != null) {
                    actionArrayList.clear();
                }
            }
        });

        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN :
                        if (socket != null) {
                            actionArrayList.add(Integer.toString(MotionEvent.ACTION_DOWN) + "/" + Float.toString(event.getX()) + "/" + Float.toString(event.getY()));
                            if (socket.isConnected())
                                socket.sendTouchAction(MotionEvent.ACTION_DOWN, event.getX(), event.getY());
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE :
                        if (socket != null) {
                            actionArrayList.add(Integer.toString(MotionEvent.ACTION_MOVE) + "/" + Float.toString(event.getX()) + "/" + Float.toString(event.getY()));
                            if (socket.isConnected())
                                socket.sendTouchAction(MotionEvent.ACTION_MOVE, event.getX(), event.getY());
                        }
                        return true;
                    case MotionEvent.ACTION_UP :
                        if (socket != null) {
                            actionArrayList.add(Integer.toString(MotionEvent.ACTION_UP) + "/" + Float.toString(event.getX()) + "/" + Float.toString(event.getY()));
                            if (socket.isConnected())
                                socket.sendTouchAction(MotionEvent.ACTION_UP, event.getX(), event.getY());
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setTouchEventBuff(String action) {

    }

    public void onWakeLock() {
        try {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConnectedMessage(String str) {
        tv.setText(str);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}