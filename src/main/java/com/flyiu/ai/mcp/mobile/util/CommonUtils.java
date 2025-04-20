package com.flyiu.ai.mcp.mobile.util;

import java.util.concurrent.Callable;

public class CommonUtils {

    public static <T> T executeByCatch(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return null;
        }
    }

}
