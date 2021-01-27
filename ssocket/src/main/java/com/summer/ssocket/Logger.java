package com.summer.ssocket;

import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;

public class Logger {

    public static void i(String tag, String name, Object ...args){
        Log.i(tag, String.format("%s: %s", name, Arrays.toString(args)));
    }

    public static void e(String tag, Throwable t, Object ...args){
        Log.e(tag, Arrays.toString(args), t);
    }
}
