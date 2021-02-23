package com.summer.ssocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.util.Consumer;
import java.net.InetAddress;
import android.os.AsyncTask;

public class NetWorkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetWorkStateReceiver";

    private Consumer<Boolean> listener;
    private String domian;
    NetWorkStateReceiver(String domian){
        Logger.i(TAG, "NetWorkStateReceiver", "开始监听网络变化", domian);
        this.domian = domian;
    }

    public NetWorkStateReceiver onNetworkStatus(Consumer<Boolean> listener){
        this.listener = listener;
        return this;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        new PingTask().execute();
    }

    private class PingTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            boolean isConnected = false;
            try {
                InetAddress address = InetAddress.getByName(domian);
                Logger.i(TAG, "ping", address.getHostName(), address.getHostAddress());
                isConnected = address.isReachable(3000);
            } catch (Exception e) { }
            listener.accept(isConnected);
            Logger.i(TAG, "onReceive", "网络发生变化", isConnected);
            return null;
        }
    }

}
