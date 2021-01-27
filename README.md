![npm version](https://img.shields.io/badge/npm-1.0.0-brightgreen)
 > 仿 Koa 中间件控制的 WebSocket 服务，安卓手机客户端程序，食用简单，上手容易, 支持 GZIP 解压缩和 ProtoBuffer 解压缩配置，觉得小弟写的还行的话，就给个[Star](https://github.com/mytwz/ssocket-android)⭐️吧~

## 使用说明

### [点击安装服务端程序](https://github.com/mytwz/ssocket)
```javascript
npm i -s ssocket
```


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
```

### 发起连接
```Java
client.connection()
```
### 绑定基础事件
```Java
client.on("close", new Consumer() {
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
client.on("ping", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"向服务器发送心跳，此时客户端当前时间是", System.currentTimeMillis());
    }
});
```

### 监听路由事件
```Java
client.on("user.user.login", new Consumer() {
    @Override
    public void accept(Object o) {
        Logger.i(TAG,"客户端收到一个消息", o);
    }
});
```

### 发送请求
```Java
client.request("test", new JSONObject() {{
        put("username", "小明");
}}, new Consumer<JSONObject>() {
    @Override
    public void accept(JSONObject o) {
        Logger.i(TAG, "收到请求回调", o);
    }
});
```

### 基础配置
```Java
client.getOptions().setHeartbeat_interval(1000 * 10);   // 设置心跳间隔时间
client.getOptions().setHeartbeat_time_out(1000 * 11);   // 设置心跳超时时间
client.getOptions().setReconnect_count(20);             // 设置重连次数
client.getOptions().setReconnect_interval(1000 * 2);    // 设置重连间隔
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

### 运行样例
```text
2021-01-27 09:27:55.973 4646-4730/com.summer.ssocket_lib I/Client: 连接成功: []
2021-01-27 09:27:55.973 4646-4730/com.summer.ssocket_lib I/MainActivity: 连接打开, 开始握手, 此时还不能发送消息: [null]
2021-01-27 09:27:55.983 4646-4730/com.summer.ssocket_lib I/MainActivity: 握手状态: [SHAKING_HANDS]
2021-01-27 09:27:55.983 4646-4730/com.summer.ssocket_lib I/Client: 开始握手: [SHAKING_HANDS]
2021-01-27 09:27:55.995 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"ack":2,"id":"AABHjAANZUcAAAACAACFJg=="},"type":0}]
2021-01-27 09:27:55.996 4646-4730/com.summer.ssocket_lib I/MainActivity: 握手状态: [CONNECTION]
2021-01-27 09:27:55.999 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"ack":3,"id":"AABHjAANZUcAAAACAACFJg=="},"type":0}]
2021-01-27 09:27:56.000 4646-4730/com.summer.ssocket_lib I/MainActivity: 握手状态: [CONNECTION]
2021-01-27 09:27:56.000 4646-4730/com.summer.ssocket_lib I/MainActivity: 重连成功: [null]
2021-01-27 09:27:56.000 4646-4730/com.summer.ssocket_lib I/Client: 握手完成，开始心跳: [CONNECTION]
2021-01-27 09:27:56.002 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":1611710877928,"type":1}]
2021-01-27 09:27:56.002 4646-4730/com.summer.ssocket_lib I/MainActivity: 心跳服务器回应，此时服务器当前时间是: [1611710877928]
2021-01-27 09:28:00.461 4646-4646/com.summer.ssocket_lib I/Client: 发送数据: [{"data":{"username":"小明"},"path":"test","request_id":1,"status":0}]
2021-01-27 09:28:00.501 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"data":{"username":"登录成功，欢迎小明"},"msg":"ok","path":"user.user.login","request_id":0,"status":200},"type":2}]
2021-01-27 09:28:00.501 4646-4730/com.summer.ssocket_lib I/MainActivity: 客户端收到一个消息: [{"username":"登录成功，欢迎小明"}]
2021-01-27 09:28:00.528 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"data":{"username":"登录成功，欢迎小明"},"msg":"ok","path":"user.user.login","request_id":0,"status":200},"type":2}]
2021-01-27 09:28:00.528 4646-4730/com.summer.ssocket_lib I/MainActivity: 客户端收到一个消息: [{"username":"登录成功，欢迎小明"}]
2021-01-27 09:28:00.533 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"data":{"username":"登录成功，欢迎小明"},"msg":"ok","path":"test","request_id":1,"status":200},"type":2}]
2021-01-27 09:28:00.533 4646-4730/com.summer.ssocket_lib I/MainActivity: 收到请求回调: [{"username":"登录成功，欢迎小明"}]
2021-01-27 09:28:00.538 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":{"data":{"username":"登录成功，欢迎小明"},"msg":"ok","path":"user.user.login","request_id":0,"status":200},"type":2}]
2021-01-27 09:28:00.538 4646-4730/com.summer.ssocket_lib I/MainActivity: 客户端收到一个消息: [{"username":"登录成功，欢迎小明"}]
2021-01-27 09:28:06.009 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":1611710887935,"type":1}]
2021-01-27 09:28:06.010 4646-4730/com.summer.ssocket_lib I/MainActivity: 心跳服务器回应，此时服务器当前时间是: [1611710887935]
2021-01-27 09:28:16.023 4646-4730/com.summer.ssocket_lib I/Client: 接收到消息: [{"data":1611710897949,"type":1}]
2021-01-27 09:28:16.023 4646-4730/com.summer.ssocket_lib I/MainActivity: 心跳服务器回应，此时服务器当前时间是: [1611710897949]

```