package com.summer.ssocket;


import com.alibaba.fastjson.JSONObject;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * 用于传输数据的编码解压缩操作
 */
public class Code {

    private static final String TAG = "Code";

    private static final ProtosCode RequestProtos = new ProtosCode();
    private static final ProtosCode ResponseProtos = new ProtosCode();

    /**
     * 包类型
     */
    public enum PackageType {
        SHAKEHANDS(0),
        HEARTBEAT(1),
        DATA(2);

        private int value;
        public int getValue(){ return this.value; }
        PackageType(int value) {
            this.value = value;
        }
    }

    public enum SocketStatus {
        /**打开 */
        OPEN(0),
        /**正在握手 */
        SHAKING_HANDS(1),
        /**握手完毕 */
        HANDSHAKE(2),
        /**连接 */
        CONNECTION(3),
        /**关闭 */
        CLOSE(4),
        /**重连 */
        RECONNECTION(5);

        private int value;
        public int getValue(){ return this.value; }
        SocketStatus(int value) {
            this.value = value;
        }
    }

    public static void parseRequestJson(JSONObject config){
        RequestProtos.parse(config);
    }

    public static void parseResponseJson(JSONObject config){
        ResponseProtos.parse(config);
    }

    public static void parseRequestJson(String config){
        RequestProtos.parse(config);
    }

    public static void parseResponseJson(String config){
        ResponseProtos.parse(config);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShakehandsPackageData {
        private String id;
        private int ack;

        public String toString(){ return JSONObject.toJSONString(this); }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PackageData {
        private String path;
        private int request_id;
        private int status;
        private String msg;
        private JSONObject data;

        public String toString(){ return JSONObject.toJSONString(this); }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Package {
        private int type;
        private Object data;

        public <T> T getData(){ return (T) data; }

        public String toString(){ return JSONObject.toJSONString(this); }
    }

    /**
     * 消息封包
     * - +------+----------------------------------+------+
     * - | head | This data exists when type == 0  | body |
     * - +------+------------+---------------------+------+
     * - | type | id length  | id                  | ack  |
     * - +------+------------+---------------------+------+
     * - | 1B   | 4B         | --                  | 1B   |
     * - +------+------------+---------------------+------+
     * - +------+----------------------------------+------+
     * - | head | This data exists when type == 1  | body |
     * - +------+----------------------------------+------+
     * - | type | body length                      | time |
     * - +------+----------------------------------+------+
     * - | 1B   | 0B                               | 8B   |
     * - +------+----------------------------------+------+
     * - +------+---------------------------------------------------+------+
     * - | head | This data exists when type == 2                   | body |
     * - +------+------------+---------------+--------+-------------+------+
     * - | type | request_id | path length   | path   | body length | body |
     * - +------+------------+---------------+--------+-------------+------+
     * - | 1B   | 4B         | 4B            | 4B     | --          | 4B   |
     * - +------+------------+---------------+--------+-------------+------+
     * @param type 类型：0握手|1心跳|2数据
     * @param package_data
     * @return
     */
    public static byte[] encode(PackageType type, PackageData package_data){
        ByteBuffer buffer = ByteBuffer.allocate(package_data.toString().getBytes().length * 2);
        int index = 0;
        ByteUtils.writeUInt8(buffer, (byte)type.getValue(), index); index += 1;
        if(type == PackageType.DATA){
            byte[] _data = ResponseProtos.encode(package_data.getPath(), package_data.getData());
            if(_data.length > 128){
                _data = GzipUtils.gzip(_data);
            }
            byte[] _path = package_data.getPath().getBytes();
            ByteUtils.writeUInt32(buffer, package_data.getRequest_id(), index += 4);
            ByteUtils.writeUInt32(buffer, _path.length, index);  index+= 4;
            ByteUtils.writeBytes(buffer, _path, index, _path.length); index += _path.length;
            ByteUtils.writeUInt32(buffer, _data.length, index); index += 4;
            ByteUtils.writeBytes(buffer, _data, index, _data.length); index += _data.length;
        }
        return ByteUtils.slice(buffer, 0, index).array();
    }

    public static byte[] encode(PackageType type){
        ByteBuffer buffer = ByteBuffer.allocate(10);
        int index = 0;
        ByteUtils.writeUInt8(buffer, (byte)type.getValue(), index); index += 1;
        if(type == PackageType.HEARTBEAT){
            ByteUtils.writeDouble(buffer, (double) System.currentTimeMillis(), index); index += 8;
        }
        return ByteUtils.slice(buffer, 0, index).array();
    }

    public static byte[] encode(PackageType type, ShakehandsPackageData package_data){
        ByteBuffer buffer = ByteBuffer.allocate(package_data.toString().getBytes().length * 2);
        int index = 0;
        ByteUtils.writeUInt8(buffer, (byte)type.getValue(), index); index += 1;
        if(type == PackageType.SHAKEHANDS){
            byte[] _id = package_data.getId().getBytes();
            ByteUtils.writeUInt32(buffer, _id.length, index); index += 4;
            ByteUtils.writeBytes(buffer, _id, index, _id.length); index += _id.length;
            ByteUtils.writeUInt8(buffer, (byte)package_data.getAck(), index); index +=1;
        }
        return ByteUtils.slice(buffer, 0, index).array();
    }

    public static Package decode(byte[] _buffer){
        int index = 0;
        ByteBuffer buffer = ByteBuffer.wrap(_buffer);
        int type = ByteUtils.readUInt8(buffer, index++);
        if(type == PackageType.DATA.getValue()){
            int request_id = ByteUtils.readUInt32(buffer, index); index +=4;
            int path_length = ByteUtils.readUInt32(buffer, index); index += 4;
            String path = ByteUtils.readString(buffer, index, path_length); index += path_length;
            int status = ByteUtils.readUInt32(buffer, index); index += 4;
            int msg_length = ByteUtils.readUInt32(buffer, index); index += 4;
            String msg = ByteUtils.readString(buffer, index, msg_length); index += msg_length;
            int data_length = ByteUtils.readUInt32(buffer, index); index += 4;
            byte[] data = GzipUtils.ungzip(ByteUtils.slice(buffer, index, data_length).array());
            JSONObject _data = ResponseProtos.decode(path, data, JSONObject.class);
            return Package.builder()
                    .type(type)
                    .data(PackageData.builder()
                            .request_id(request_id)
                            .path(path)
                            .status(status)
                            .msg(msg)
                            .data(_data).build()
                    ).build();
        }
        else if(type == PackageType.HEARTBEAT.getValue()){
            long data = (long) ByteUtils.readDouble(buffer, index);
            return Package.builder().type(type).data(data).build();
        }
        else if(type == PackageType.SHAKEHANDS.getValue()){
            int id_length = ByteUtils.readUInt32(buffer, index); index += 4;
            String id = ByteUtils.readString(buffer, index, id_length); index += id_length;
            int ack = ByteUtils.readUInt8(buffer, index);
            return Package.builder()
                    .type(type)
                    .data(ShakehandsPackageData.builder()
                            .id(id)
                            .ack(ack).build()
                    ).build();
        }
        return null;
    }

    public static final Map<Integer, String> StatusCode = new HashMap<Integer, String>(){{
        put(4100, "client ping timeout");
        put(4102, "server ping timeout");
        put(4101, "connection close");
        put(200, "ok");
    }};

    public static void expandStatusCode(Integer code, String msg){
        StatusCode.put(code, msg);
    }
}
