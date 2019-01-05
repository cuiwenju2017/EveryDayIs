package com.example.everydayis.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 连接服务器读取输出流的类
 */
public class StreamUtils {

    public static String inputSteam2String(InputStream inputStream) {
        BufferedReader read = new BufferedReader(new InputStreamReader(inputStream));
        String data = null;
        try {
            data = read.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

}
