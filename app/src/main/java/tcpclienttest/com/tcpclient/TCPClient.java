package tcpclienttest.com.tcpclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TCPClient extends Thread {
    private Socket  mSocket;

    private BufferedReader buffRecv;
    private BufferedWriter buffSend;

    private String  mAddr = "";
    private int     mPort = 0;
    private boolean mConnected = false;
    private Handler mHandler = null;
    
    private Timer timer = null;

    public MainActivity mainActivity;

    static class MessageTypeClass {
        public static final int SIMSOCK_CONNECTED = 0;
        public static final int SIMSOCK_DATA = 1;
        public static final int SIMSOCK_DISCONNECTED = 2;
    };
    public enum MessageType { SIMSOCK_CONNECTED, SIMSOCK_DATA, SIMSOCK_DISCONNECTED };

    public TCPClient(String addr, int port, Handler handler, MainActivity mainActivity)
    {
        mAddr = addr;
        mPort = port;
        mHandler = handler;
        this.mainActivity = mainActivity;
    }

    private void makeMessage(MessageType what, Object obj)
    {
        Message msg = Message.obtain();
        msg.what = what.ordinal();
        msg.obj  = obj;
        mHandler.sendMessage(msg);
    }

    private boolean connect (String addr, int port) {
        try {
            InetSocketAddress socketAddress  = new InetSocketAddress (InetAddress.getByName(addr), port);
            mSocket = new Socket();
            mSocket.connect(socketAddress, 3000);
            mSocket.setKeepAlive(true);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        } finally {
            mainActivity.onWakeLock();
        }
        return true;
    }

    @Override
    public void run() {
        if(! connect(mAddr, mPort)) {
            mainActivity.onWakeLock();
            Log.d("_LOG_", "Connect Fail");
            makeMessage(MessageType.SIMSOCK_CONNECTED, "0");
            mConnected = false;
            return; // connect failed
        } else if(mSocket == null) {
            Log.d("_LOG_", "Connect Fail");
            makeMessage(MessageType.SIMSOCK_CONNECTED, "0");
            mConnected = false;
            return;
        } else {
            Log.d("_LOG_", "Connected Server");
            makeMessage(MessageType.SIMSOCK_CONNECTED, "1");
        }

        try {
            buffRecv = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            buffSend = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mConnected = true;
    }

    synchronized public boolean isConnected(){
        return mConnected;
    }

    public void setPaused() {
        try {
            if (buffRecv != null) {
                buffRecv.close();
            }
            if (buffSend != null) {
                buffSend.close();
            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        makeMessage(MessageType.SIMSOCK_DISCONNECTED, "");
        mConnected = false;
    }

    public void outPrintln(String str) {

        final String strAction = str;

        final AsyncTask<String, Void, Void> asyncTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                PrintWriter out = new PrintWriter(buffSend, true);
                out.println(strAction);
                return null;
            }
        };
        try {
            asyncTask.execute(strAction);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void sendTouchAction(int action, float x, float y) {  //실시간 터치 이벤트 전달
        final String strAction = String.valueOf(action) + "/" + String.valueOf(x) + "/" + String.valueOf(y);
        Log.d("__LOG_", "[sendTouchAction] strAction: " + strAction);

        final AsyncTask<String, Void, Void> asyncTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                PrintWriter out = new PrintWriter(buffSend, true);
                out.println(strAction);
                return null;
            }
        };
        try {
            asyncTask.execute(strAction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}