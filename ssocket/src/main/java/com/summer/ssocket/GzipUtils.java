package com.summer.ssocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipUtils {

    public static final int GZIP_HEADER_FORMAT = 0x1f8b;

    /**
     * GZIP 压缩
     * @param data
     * @return
     */
    public static byte[] gzip(byte[] data){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(data);
            gzip.close();
            byte[] encode = baos.toByteArray();
            baos.flush();
            baos.close();
            return  encode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * GZIP 解压
     * @param data
     * @return
     */
    public static byte[] ungzip(byte[] data){
        try {
            if(!check(data)) return data;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int n = 0;
            while ((n = gzip.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, n);
            }
            gzip.close();
            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检查二进制是否是 GZIP 格式的数据
     * @param data
     * @return
     */
    public static boolean check(byte[] data){
        return data != null && data.length > 2 && ByteBuffer.wrap(new byte[]{ data[0], data[1] }).getShort() == GZIP_HEADER_FORMAT;
    }
}
