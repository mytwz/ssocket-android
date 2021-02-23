package com.summer.ssocket;


import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 二进制操作
 */
public class ByteUtils {

    private static final String TAG = "ByteUtils";
    private static final int UINT_MAX = 0xFFFFFFFF;

    private ByteUtils(){}

    public static int writeUInt8(ByteBuffer buffer, byte value, int offset){
        buffer.put(ByteBuffer.allocate(1).put(value).array());
        return offset+= 1;
    }
    public static int writeUInt16(ByteBuffer buffer, short value, int offset){
        buffer.put(ByteBuffer.allocate(2).putShort( value).array());
        return offset+= 2;
    }
    public static int writeUInt32(ByteBuffer buffer, int value, int offset){
        buffer.put(ByteBuffer.allocate(4).putInt(value).array());
        return offset+= 4;
    }
    public static int writeUInt64(ByteBuffer buffer, long value, int offset){
        buffer.put(ByteBuffer.allocate(8).putLong(value).array());
        return offset+= 8;
    }
    public static int writeFloat(ByteBuffer buffer, float value, int offset){
        buffer.put(ByteBuffer.allocate(4).putFloat(value).array());
        return offset+= 4;
    }
    public static int writeDouble(ByteBuffer buffer, double value, int offset){
        buffer.put(ByteBuffer.allocate(8).putDouble(value).array());
        return offset+= 8;
    }
    public static int writeString(ByteBuffer buffer, String value, int offset, int length){
        buffer.put(value.getBytes(), 0, length);
        return offset += length;
    }
    public static int writeBytes(ByteBuffer buffer, byte[] value, int offset, int length){
        buffer.put(value, 0, length);
        return offset += length;
    }

    public static int readUInt8(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 1).get();
    }
    public static int readUInt16(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 2).getShort();
    }
    public static int readUInt32(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 4).getInt();
    }
    public static long readUInt64(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 8).getLong();
    }
    public static float readFloat(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 4).getFloat();
    }
    public static double readDouble(ByteBuffer buffer, int offset){
        return slice(buffer, offset, 8).getDouble();
    }
    public static String readString(ByteBuffer buffer, int offset, int length){
        return new String(slice(buffer, offset, length).array());
    }

    public static ByteBuffer slice(ByteBuffer buffer, int offset, int length){
        byte[] bytes = new byte[length];
        buffer.position(offset);
        buffer.get(bytes, 0, length);
        return ByteBuffer.wrap(bytes);
    }


}
