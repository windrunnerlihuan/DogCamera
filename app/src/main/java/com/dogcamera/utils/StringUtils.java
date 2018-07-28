package com.dogcamera.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StringUtils {

    public static String exception2String(Exception exception) {
        if (exception == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            exception.printStackTrace(new PrintStream(baos));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return baos.toString();
    }
}
