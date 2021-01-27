package com.summer.ssocket;

import androidx.core.util.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Emitter {
    private Map<String, List<Consumer>> _callback = null;

    public Emitter(){
        _callback = new HashMap<>();
    }

    public void on(String event, Consumer callback){
        if(!this._callback.containsKey(event)){
            this._callback.put(event, new ArrayList<Consumer>());
        }
        this._callback.get(event).add(callback);
    }

    public void offAll(){ this._callback = new HashMap<>(); }

    public void off(String event){
        this._callback.remove(event);
    }

    public void off(String event, Consumer callback){
        if(this._callback.containsKey(event)){
            if(this._callback.get(event).contains(callback)) this._callback.get(event).remove(callback);
        }
    }

    public void once(final String event, final Consumer callback){

        Consumer fn = new Consumer() {
            @Override
            public void accept(Object o) {
                off(event);
                callback.accept(o);
            }
        };

        this.on(event, fn);
    }

    public void emit(String event, Object args){
        if(this._callback.containsKey(event)){
           for(Consumer callback : this._callback.get(event)){
               callback.accept(args);
           }
        }
    }
}
