package com.summer.ssocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.nio.ByteBuffer;

public class ProtosCode {


    private static final String TAG = "ProtosCode";

    /**字段类型 */
    private static class FIELD_TYPE {
        public static final String message = "message";
        public static final String required = "required";
        public static final String optional = "optional";
        public static final String repeated = "repeated";
    }

    /**数据类型 */
    private static enum DATA_TYPE {
        UINT8, UINT16, UINT32, UINT64,FLOAT,DOUBLE,STRING,MESSAGE
    }

    /**配置对象 */
    private JSONObject protos = new JSONObject();

    /**
     * 解析 protos JSON 配置对象
     * @param obj
     */
    private JSONObject parseObject(JSONObject obj){
        JSONObject proto = new JSONObject();
        JSONObject nestProtos = new JSONObject();
        JSONObject tags = new JSONObject();

        for(String name : obj.keySet()){

            final String[] params = name.split("\\s+");
            if(params.length >= 2){
                switch (params[0]){
                    case FIELD_TYPE.message :{
                        if(params.length != 2) continue;
                        nestProtos.put(params[1], parseObject(obj.getJSONObject(name)));
                        continue;
                    }
                    case FIELD_TYPE.required:
                    case FIELD_TYPE.optional:
                    case FIELD_TYPE.repeated: {
                        final Integer tag = obj.getInteger(name);
                        if(params.length != 3 && !tags.containsKey(tag)) continue;
                        proto.put(params[2], new JSONObject(){{
                            put("option", params[0]);
                            put("type", params[1]);
                            put("tag", tag);
                        }});
                        tags.put(String.valueOf(tag), params[2]);
                        continue;
                    }
                }
            }
        }
        proto.put("__messages", nestProtos);
        proto.put("__tags", tags);
        return proto;
    }

    /**
     * 解析 protos JSON 配置文件
     * @param config
     */
    public void parse(JSONObject config){
        for(String key : config.keySet()) protos.put(key, parseObject(config.getJSONObject(key)));
        Logger.i(TAG, "parse", JSONObject.toJSONString(config.keySet()), protos.toJSONString());
    }

    /**
     * 解析 protos JSON 配置文件
     * @param config
     */
    public void parse(String config){;
        parse(JSONObject.parseObject(config));
    }

    /**
     * 写入头部信息
     * @param buffer
     * @param tag
     * @param offset
     * @return
     */
    private int writeTag(ByteBuffer buffer, int tag, int offset){
        offset = ByteUtils.writeUInt8(buffer, (byte) tag, offset++);
        return offset;
    }

    /**
     * 读取字段标签
     * @param buffer
     * @param offset
     * @return
     */
    private int readTag(ByteBuffer buffer, int offset){
        int tag = ByteUtils.readUInt8(buffer, offset);
        return tag;
    }

    /**
     * 编码
     * @param protos_name
     * @param data
     * @return
     */
    public byte[] encode(String protos_name, JSONObject data){
        if(this.protos.containsKey(protos_name)){
            ByteBuffer buffer = ByteBuffer.allocate(JSONObject.toJSONBytes(data).length * 2);
            int length = write(protos.getJSONObject(protos_name), data, buffer);
            return ByteUtils.slice(buffer, 0, length).array();
        }

        return data.toJSONString().getBytes();
    }

    private int write(JSONObject protos, JSONObject data, ByteBuffer buffer){
        int offset = 0;
        for(String name : data.keySet()){
            if(protos.containsKey(name)){
                JSONObject proto = protos.getJSONObject(name);
                switch (proto.getString("option")){
                    case FIELD_TYPE.optional :
                    case FIELD_TYPE.required :{
                        offset = writeTag(buffer, proto.getIntValue("tag"), offset);
                        offset = writeBody(data.get(name), proto.getString("type"), buffer, offset, protos);
                        break;
                    }
                    case FIELD_TYPE.repeated :{
                        JSONArray array = data.getJSONArray(name);
                        offset = writeTag(buffer, proto.getIntValue("tag"), offset);
                        offset = ByteUtils.writeUInt32(buffer, array.size(), offset);
                        for(int i = 0, l = array.size(); i < l; i++) {
                            offset = writeBody(array.get(i), proto.getString("type"), buffer, offset, protos);
                        }
                        break;
                    }
                }
            }
        }

        return offset;
    }

    private int writeBody(Object value, String type, ByteBuffer buffer, int offset, JSONObject protos){
        switch (type){
            case "uint8" :{
                offset = ByteUtils.writeUInt8(buffer, Byte.valueOf(value.toString()), offset);
                break;
            }
            case "uint16" :{
                offset = ByteUtils.writeUInt16(buffer, Short.valueOf(value.toString()), offset);
                break;
            }
            case "uint32" :{
                offset = ByteUtils.writeUInt32(buffer, Integer.valueOf(value.toString()), offset);
                break;
            }
            case "uint64" :{
                offset = ByteUtils.writeUInt64(buffer, Long.valueOf(value.toString()), offset);
                break;
            }
            case "float" :{
                offset = ByteUtils.writeFloat(buffer, Float.valueOf(value.toString()), offset);
                break;
            }
            case "double" :{
                offset = ByteUtils.writeDouble(buffer, Double.valueOf(value.toString()), offset);
                break;
            }
            case "string" :{
                offset = ByteUtils.writeUInt32(buffer, value.toString().getBytes().length, offset);
                offset = ByteUtils.writeString(buffer, value.toString(), offset, value.toString().getBytes().length);
                break;
            }
            default: {
                JSONObject message = protos.getJSONObject("__messages").getJSONObject(type);
                if(message != null){
                    ByteBuffer tmpBuffer = ByteBuffer.allocate(JSONObject.toJSONString(value).getBytes().length * 2);
                    int length = write(message, (JSONObject) value, tmpBuffer);
                    offset = ByteUtils.writeUInt32(buffer, length, offset);
                    offset = ByteUtils.writeBytes(buffer, ByteUtils.slice(tmpBuffer, 0, length).array(), offset, length);
                }
                break;
            }
        }

        return offset;
    }

    /**
     * 解码
     * @param protos_name
     * @param buffer
     * @param t
     * @param <T>
     * @return
     */
    public <T> T decode(String protos_name, byte[] buffer, Class<T> t){
        if(protos.containsKey(protos_name)){
            JSONObject data = new JSONObject();
            read(protos.getJSONObject(protos_name), data, ByteBuffer.wrap(buffer), 0);
            return JSONObject.parseObject(data.toJSONString(), t);
        }
        return JSONObject.parseObject(new String(buffer), t);
    }

    /**
     *
     * @param protos
     * @param data
     * @param buffer
     * @param offset
     * @return
     */
    private int read(JSONObject protos, JSONObject data, ByteBuffer buffer, int offset){
        if(protos != null){
            while(offset < buffer.limit()){
                int tag = readTag(buffer, offset); offset += 1;
                String name = protos.getJSONObject("__tags").getString(tag + "");
                JSONObject proto = protos.getJSONObject(name);
                switch (proto.getString("option")){
                    case FIELD_TYPE.optional:
                    case FIELD_TYPE.required:{
                        ReadBody body = readBody(buffer, proto.getString("type"), offset, protos);
                        offset = body.offset;
                        data.put(name, body.value);
                        break;
                    }
                    case FIELD_TYPE.repeated:{
                        if(!data.containsKey(name)) data.put(name, new JSONArray());
                        int length = ByteUtils.readUInt32(buffer, offset); offset += 4;
                        for(int i = 0; i < length; i++){
                            ReadBody body = readBody(buffer, proto.getString("type"), offset, protos);
                            offset = body.offset;
                            data.getJSONArray(name).add(body.value);
                        }
                        break;
                    }
                }
            }
        }

        return offset;
    }

    private static class ReadBody {
        public Object value;
        public int offset;
    }

    /**
     *
     * @param buffer
     * @param type
     * @param offser
     * @param protos
     * @return
     */
    private ReadBody readBody(ByteBuffer buffer, String type, int offser, JSONObject protos ){
        ReadBody body = new ReadBody();
        switch (type){
            case "uint8":{
                body.value = ByteUtils.readUInt8(buffer, offser);
                body.offset = offser + 1;
                break;
            }
            case "uint16":{
                body.value = ByteUtils.readUInt16(buffer, offser);
                body.offset = offser + 2;
                break;
            }
            case "uint32":{
                body.value = ByteUtils.readUInt32(buffer, offser);
                body.offset = offser + 4;
                break;
            }
            case "uint64":{
                body.value = ByteUtils.readUInt64(buffer, offser);
                body.offset = offser + 8;
                break;
            }
            case "float":{
                body.value = ByteUtils.readFloat(buffer, offser);
                body.offset = offser + 4;
                break;
            }
            case "double":{
                body.value = ByteUtils.readDouble(buffer, offser);
                body.offset = offser + 8;
                break;
            }
            case "string":{
                int length = ByteUtils.readUInt32(buffer, offser); offser += 4;
                body.value = ByteUtils.readString(buffer, offser, length);
                body.offset = offser + length;
                break;
            }
            default:{
                JSONObject message = protos.getJSONObject("__messages").getJSONObject(type);
                if(message != null){
                    int length = ByteUtils.readUInt32(buffer, offser); offser += 4;
                    body.value = new JSONObject();
                    body.offset = offser + length;
                    read(message, (JSONObject) body.value, ByteUtils.slice(buffer, offser, length), 0);
                }
                break;
            }
        }

        return body;
    }
}
