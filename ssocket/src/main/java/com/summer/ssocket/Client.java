package com.summer.ssocket;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import com.alibaba.fastjson.JSONObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Client extends Emitter {

    private static final String TAG = "Client";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Options {
        private String id = "";
        /**
         * 重连次数
         */
        private int reconnect_count = 20;
        /**
         * 重连间隔
         */
        private int reconnect_interval = 2 * 1000;
        /**
         * 心跳间隔时间
         */
        private int heartbeat_interval = 10 * 1000;
        /**
         * 心跳超时时间
         */
        private int heartbeat_time_out = 11 * 1000;
        /**ProtoBuf 解压缩配置 */
        private String protosRequestJson;
        /**ProtoBuf 解压缩配置 */
        private String protosResponseJson;
    }


    private String id = "";
    private Options options;
    private String url = null;
    private WebSocket socket;
    private boolean is_pong = true;
    private Map<Integer, Integer> requestIDDic = new HashMap<>();
    private Handler timer_handler = new Handler();
    private Runnable heartbeat_time_out_run = new Runnable() {
        @Override
        public void run() {
            if(!is_pong){
                if(isSendData()){
                    try{
                        socket.close(4102, Code.StatusCode.get("4102"));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    private Runnable heartbeat_run = new Runnable() {
        @Override
        public void run() {
            try{
                is_pong = false;
                send(Code.encode(Code.PackageType.HEARTBEAT));
                emit("ping", System.currentTimeMillis());
                timer_handler.postDelayed(heartbeat_time_out_run, options.heartbeat_time_out);
            }catch (Exception e){ e.printStackTrace(); }
        }
    };;
    private Runnable reconnect_run = new Runnable() {
        @Override
        public void run() {
            try{
                if(status == Code.SocketStatus.SHUTDOWN) return;
                connection();
                emit("reconnectioning", options.reconnect_count - reconnect_count);
                status = Code.SocketStatus.RECONNECTION;
                Logger.i(TAG,"开始重连", status);
            }catch (Exception e){ e.printStackTrace(); }
        }
    };;
    private Context app;
    private NetWorkStateReceiver netWorkStateReceiver;
    private int reconnect_count;
    private int __index__ = 0;
    private Code.SocketStatus status = Code.SocketStatus.CLOSE;

    public Code.SocketStatus getStatus() {
        return status;
    }
    private boolean isSendData(){
        return  socket != null && status == Code.SocketStatus.CONNECTION;
    }

    public Options getOptions(){ return options; }

    public Client(String url, Context app){
        super();
        try{
            this.status = Code.SocketStatus.INIT;
            this.app = app;
            this.options = new Options();
            this.url = url  + (url.contains("?") ? "&" : "?") + "e=android&v=1.0.0";
            this.netWorkStateReceiver = new NetWorkStateReceiver("www.baidu.com").onNetworkStatus(new Consumer<Boolean>(){
                @Override
                public void accept(Boolean isconn) {
                    if(!isconn && status == Code.SocketStatus.CONNECTION) {
                        status = Code.SocketStatus.SHUTDOWN;
                        close();
                    }
                    else if(status == Code.SocketStatus.CLOSE){
                        connection();
                    }
                }
            });
            reconnect_count = options.reconnect_count;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            app.registerReceiver(this.netWorkStateReceiver, intentFilter);
            Logger.i(TAG,"创建客户端对象");
        }
        catch (Exception e){
            Logger.e(TAG,e,"创建客户端对象报错");
        }
    }

    public Client connection(){
        try{
            if(status == Code.SocketStatus.OPENING ) return this;
            if(status == Code.SocketStatus.CLOSE || status == Code.SocketStatus.SHUTDOWN || status == Code.SocketStatus.INIT) {
            if(!"".equals(options.id)) this.id = options.id;
            if(options.getProtosRequestJson() != null){
                Code.parseRequestJson(options.getProtosRequestJson());
            }
            if(options.getProtosResponseJson() != null){
                Code.parseResponseJson(options.getProtosResponseJson());
            }
            Logger.i(TAG,"开始发起连接", options);
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(url).build();
            status = Code.SocketStatus.OPENING;
            is_pong = true;
            socket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    super.onOpen(webSocket, response);
                    try {
                        Logger.i(TAG,"连接成功");
                        emit("open", null);
                        status = Code.SocketStatus.OPEN;
                        reconnect_count = options.reconnect_count;
                        shakehands(Code.SocketStatus.SHAKING_HANDS);
                        emit("shakehands", status = Code.SocketStatus.SHAKING_HANDS);
                        Logger.i(TAG,"开始握手", status);
                    }
                    catch (Exception e){
                        Logger.e(TAG,e,"连接报错");
                    }
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    super.onClosed(webSocket, code, reason);
                    handleClose(code);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    super.onFailure(webSocket, t, response);
                    handleClose(0);
                    emit("error", t);
                    Logger.e(TAG,t, "Socket 异常");
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    super.onMessage(webSocket, bytes);
                    try{
                        Code.Package aPackage = Code.decode(bytes.toByteArray());
                        Logger.i(TAG,"接收到消息", aPackage);
                        if(Code.PackageType.SHAKEHANDS.getValue() == aPackage.getType()){
                            Code.ShakehandsPackageData data = aPackage.getData();
                            if(data.getAck() == Code.SocketStatus.HANDSHAKE.getValue()){
                                if("".equals(id)) id = data.getId();
                                shakehands(Code.SocketStatus.CONNECTION);
                                status = Code.SocketStatus.CONNECTION;
                                emit("shakehands", status);
                            }
                            else if(data.getAck() == Code.SocketStatus.CONNECTION.getValue()){
                                status = Code.SocketStatus.CONNECTION;
                                emit("shakehands", status);
                                emit(id.equals(data.getId()) ? "connection" : "reconnection", null);
                                send(Code.encode(Code.PackageType.HEARTBEAT));
                                Logger.i(TAG,"握手完成，开始心跳", status);
                            }
                        }
                        else if(Code.PackageType.HEARTBEAT.getValue() == aPackage.getType()){
                            long time = aPackage.getData();
                            emit("pong", time);
                            is_pong = true;
                            timer_handler.postDelayed(heartbeat_run, options.heartbeat_interval);
                        }
                        else if(Code.PackageType.DATA.getValue() == aPackage.getType()){
                            Code.PackageData body = aPackage.getData();
                            if(body.getRequest_id() != 0){
                                emit(String.valueOf(body.getRequest_id()), body);
                            }
                            else {
                                emit(body.getPath(), body);
                            }
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

            };
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return this;
    }

    private void shakehands(Code.SocketStatus ack){
        if(socket != null && status == Code.SocketStatus.CLOSE) return;
        socket.send(ByteString.of(ByteBuffer.wrap(Code.encode(
                Code.PackageType.SHAKEHANDS,
                Code.ShakehandsPackageData.builder()
                        .id(id)
                        .ack(ack.getValue())
                        .build()
                )
        )));
    }

    private void handleClose(int code){
        try {
            if(status == Code.SocketStatus.CLOSE) return;
            emit("close", code);
            Logger.i(TAG,"连接关闭", status);
            for(Integer event : requestIDDic.keySet()){
                emit(event.toString(), Code.PackageData.builder().status(code).msg("client close").build());
            }
            if(status != Code.SocketStatus.SHUTDOWN){
                status = Code.SocketStatus.CLOSE;
                socket = null;
                if(--reconnect_count > 0){
                    timer_handler.postDelayed(reconnect_run, options.reconnect_interval);
                }
            }
        }
        catch (Exception e){
            Logger.e(TAG,e, "重连报错");
        }
    }

    private void send(Code.PackageData data){
        if(data.getPath() != null){
            if(isSendData()){
                Logger.i(TAG,"发送数据", data);
                send(Code.encode( Code.PackageType.DATA,data ));
            }
        }
    }

    private void send(byte[] data){
        if(isSendData()) {
            socket.send(ByteString.of(ByteBuffer.wrap(data)));
        }
    }

    public void request(String path, JSONObject data, final Consumer callback){
        if(isSendData()){
            Code.PackageData.PackageDataBuilder packageData = Code.PackageData.builder().path(path).data(data);
            final int request_id = __index__++ > 9999999 ? (__index__ = 1) : __index__;
            if(callback != null){
                once(String.valueOf(request_id), new Consumer() {
                    @Override
                    public void accept(Object o) {
                        requestIDDic.remove(request_id);
                        callback.accept(o);
                    }
                });
            }
            packageData.request_id(request_id);
            try {
                send(packageData.build());
            }
            catch (Exception e){
                status = Code.SocketStatus.CLOSE;
                emit("close", 4101);
                e.printStackTrace();
            }
        }
    }

    public void request(String path, JSONObject data){
        request(path, data, null);
    }

    public void close(){
        if(socket != null && status == Code.SocketStatus.CONNECTION){
            status = Code.SocketStatus.SHUTDOWN;
            handleClose(4020);
            socket.close(4020, Code.StatusCode.get(4020));
        }
    }

    public void onDestroy(){
        this.app.unregisterReceiver(this.netWorkStateReceiver);
    }
}
