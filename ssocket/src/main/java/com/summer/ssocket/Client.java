package com.summer.ssocket;

import android.os.Handler;
import androidx.core.util.Consumer;
import com.alibaba.fastjson.JSONObject;
import java.nio.ByteBuffer;
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
         * 是否打印心跳
         */
        private boolean is_log_heartbeat = true;
        /**
         * 是否打印其他日志
         */
        private boolean is_log_message = true;
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
    private boolean is_not_heartbeat = true;
    private Handler timer_handler = new Handler();
    private Runnable heartbeat_time_out_run;
    private Runnable heartbeat_run;
    private Runnable reconnect_run;

    private int reconnect_count;
    private int __index__ = 0;
    private Code.SocketStatus status = Code.SocketStatus.CLOSE;

    /**
     * 连接状态：0未连接|1连接中|2已连接
     * @return
     */
    public Code.SocketStatus getStatus() {
        return status;
    }
    private boolean isSendData(){
        return  socket != null && status == Code.SocketStatus.CONNECTION;
    }

    public Options getOptions(){ return options; }

    public Client(String url){
        super();
        try{
            this.options = new Options();
            this.url = url  + (url.contains("?") ? "&" : "?") + "e=android&v=1.0.0";
            Logger.i(TAG,"创建客户端对象");
        }
        catch (Exception e){
            Logger.e(TAG,e,"创建客户端对象报错");
        }
    }

    private void removeTimer(Runnable task){
        try {
            if(task != null) timer_handler.removeCallbacks(task);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public Client connection(){
        if(status != Code.SocketStatus.CLOSE) return this;
        this.id = options.id;
        if(options.getProtosRequestJson() != null){
            Code.parseRequestJson(options.getProtosRequestJson());
        }
        if(options.getProtosResponseJson() != null){
            Code.parseResponseJson(options.getProtosResponseJson());
        }
        Logger.i(TAG,"开始发起连接", options);
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(url).build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    Logger.i(TAG,"连接成功");
                    removeTimer(heartbeat_time_out_run);
                    removeTimer(heartbeat_run);
                    removeTimer(reconnect_run);
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
                            if(id == "") id = data.getId();
                            shakehands(Code.SocketStatus.CONNECTION);
                            status = Code.SocketStatus.CONNECTION;
                            emit("shakehands", status);
                        }
                        else if(data.getAck() == Code.SocketStatus.CONNECTION.getValue()){
                            status = Code.SocketStatus.CONNECTION;
                            emit("shakehands", status);
                            emit(id != data.getId() ? "reconnection" : "connection", null);
                            send(Code.encode(Code.PackageType.HEARTBEAT));
                            Logger.i(TAG,"握手完成，开始心跳", status);
                        }
                    }
                    else if(Code.PackageType.HEARTBEAT.getValue() == aPackage.getType()){
                        long time = aPackage.getData();
                        emit("pong", time);
                        removeTimer(heartbeat_time_out_run);
                        heartbeat_run = new Runnable() {
                            @Override
                            public void run() {
                                send(Code.encode(Code.PackageType.HEARTBEAT));
                                emit("ping", null);
                                removeTimer(heartbeat_run);
                                heartbeat_time_out_run = new Runnable() {
                                    @Override
                                    public void run() {
                                        removeTimer(heartbeat_time_out_run);
                                        if(isSendData()){
                                            socket.close(4102, Code.StatusCode.get("4102"));
                                        }
                                    }
                                };
                                timer_handler.postDelayed(heartbeat_time_out_run, options.heartbeat_time_out);
                            }
                        };
                        timer_handler.postDelayed(heartbeat_run, options.heartbeat_interval);
                    }
                    else if(Code.PackageType.DATA.getValue() == aPackage.getType()){
                        Code.PackageData body = aPackage.getData();
                        if(body.getRequest_id() != 0){
                            emit(String.valueOf(body.getRequest_id()), body.getData());
                        }
                        else {
                            emit(body.getPath(), body.getData());
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

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
            if(status != Code.SocketStatus.CLOSE){
                status = Code.SocketStatus.CLOSE;
                socket = null;
                removeTimer(heartbeat_time_out_run);
                removeTimer(heartbeat_run);
                removeTimer(reconnect_run);
                if(--reconnect_count > 0){
                    reconnect_run = new Runnable() {
                        @Override
                        public void run() {
                            connection();
                            emit("reconnectioning", options.reconnect_count - reconnect_count);
                            status = Code.SocketStatus.RECONNECTION;
                            Logger.i(TAG,"开始重连", status);
                        }
                    };
                    timer_handler.postDelayed(reconnect_run, options.reconnect_interval);
                }
                emit("close", code);
                Logger.i(TAG,"连接关闭", status);
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

    public void request(String path, JSONObject data, Consumer callback){
        if(isSendData()){
            Code.PackageData.PackageDataBuilder packageData = Code.PackageData.builder().path(path).data(data);
            if(callback != null){
                int request_id = __index__++ > 9999999 ? (__index__ = 1) : __index__;
                once(String.valueOf(request_id), callback);
                packageData.request_id(request_id);
            }
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

    }

}
