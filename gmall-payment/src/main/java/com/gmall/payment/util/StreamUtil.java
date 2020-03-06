package com.gmall.payment.util;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayOutputStream;

public class StreamUtil {
    public static String inputStream2String(ServletInputStream inputStream, String encode) {
        int buffer_size = 1024;
        String result = null;
        try {
            if (inputStream != null) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                byte[] tempBytes = new byte[buffer_size];
                int count = -1;
                while ((count = inputStream.read(tempBytes, 0, buffer_size)) != -1) {
                    outStream.write(tempBytes, 0, count);
                }
                tempBytes = null;
                outStream.flush();
                result = new String(outStream.toByteArray(), encode);
            }
        } catch (Exception e) {
            result = null;
        }
        return result;
    }
}
