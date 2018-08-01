package com.dogcamera.utils;

import java.nio.ShortBuffer;

public class ByteUtils {

    public static void mixByteSpecLen(ShortBuffer src1, ShortBuffer src2, int len, ShortBuffer dst){
        for (int i = 0; i < len; i++) {
            int a = src1.get();
            int b = src2.get();
            short result;
            if (a < 0 && b < 0) {
                int i1 = a + b - a  * b / (-32768);
                if (i1 < -32768) {
                    result = -32768;
                }else{
                    result = (short) i1;
                }
            } else if (a > 0 && b > 0) {
                int i1 = a + b - a  * b  / 32767;
                if (i1 > 32767) {
                    result = 32767;
                }else{
                    result = (short) i1;
                }
            } else {
                int i1 = a + b;
                if (i1 > 32767) {
                    result = 32767;
                } else if (i1 < -32768) {
                    result = -32768;
                } else {
                    result = (short) i1;
                }
            }
            dst.put(result);
        }

    }
}
