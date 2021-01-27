![npm version](https://img.shields.io/badge/npm-1.0.0-brightgreen)
 > 仿 Koa 中间件控制的 WebSocket 服务，安卓手机客户端程序，食用简单，上手容易, 支持 GZIP 解压缩和 ProtoBuffer 解压缩配置，觉得小弟写的还行的话，就给个[Star](https://github.com/mytwz/ssocket)⭐️吧~

## 使用说明

### 添加依赖
```Java
// build.gradle

annotationProcessor "org.projectlombok:lombok:1.18.2"
implementation "org.projectlombok:lombok:1.18.2"
implementation 'com.squareup.okhttp3:okhttp:3.5.0'
implementation 'com.alibaba:fastjson:1.2.73'
```

### 创建客户端对象
```Java
Client client = new Client("http://127.0.0.1:8080");
client.connection().on("close", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"连接关闭", o);
    }
});
client.on("open", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"连接打开, 开始握手, 此时还不能发送消息", o);
    }
});
client.on("connection", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"与服务端握手成功， 此时可以开始发送消息", o);
        client.request("test", new JSONObject() {{
                put("username", "小明");
        }}, new Consumer<JSONObject>() {
            @Override
            public void accept(JSONObject o) {
                Logger.i(TAG, "收到请求回调", o);
            }
        });
    }
});
client.on("shakehands", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"握手状态", o);
    }
});
client.on("reconnectioning", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"开始重连， 剩余重连次数", o);
    }
});
client.on("reconnection", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"重连成功", o);
    }
});
client.on("pong", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"心跳服务器回应，此时服务器当前时间是", o);
    }
});
client.on("user.user.login", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"客户端收到一个消息", o);
    }
});
```


### 配置 ProtoBuf 解压缩配置
```xml
<!-- 在 res/values/strings.xml 中添加配置 -->
<resources>
    <string name="protos_request_json">
        {
            \"test\":{
                \"required string username\": 0
            }
        }
    </string>
    <string name="protos_response_json">
        {
            \"user.user.login\":{
                \"required string username\": 0
            },

            \"test\":{
                \"required string username\": 0
            }
        }
    </string>
</resources>

```
```Java
client.getOptions().setProtosRequestJson(getResources().getString(R.string.protos_request_json)); // 获取 ProtoBuf 请求配置
client.getOptions().setProtosResponseJson(getResources().getString(R.string.protos_response_json)); // 获取 ProtoBuf 响应配置
```
