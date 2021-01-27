package com.summer.ssocket_lib;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.alibaba.fastjson.JSONObject;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.summer.ssocket.Client;
import com.summer.ssocket.Logger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "发送测试事件", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                if(client != null){

                    client.request("test", new JSONObject() {{
                        put("username", "小明");
                    }}, new Consumer<JSONObject>() {
                        @Override
                        public void accept(JSONObject o) {
                            Logger.i(TAG, "收到请求回调", o);
                        }
                    });
                }
            }
        });

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            Log.i("MainActivity", "有网络权限");
            createSocket();
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.INTERNET }, 1);
            Log.i("MainActivity", "没有网络权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1 :{
                Log.i("MainActivity", "获取网络权限");
                createSocket();
                break;
            }
            default: break;
        }
    }

    private void createSocket(){
        client = new Client("http://10.9.16.34:8080");
        client.getOptions().setProtosRequestJson(getResources().getString(R.string.protos_request_json));
        client.getOptions().setProtosResponseJson(getResources().getString(R.string.protos_response_json));
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
