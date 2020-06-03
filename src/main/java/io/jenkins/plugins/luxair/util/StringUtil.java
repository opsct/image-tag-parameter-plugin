package io.jenkins.plugins.luxair.util;

public class StringUtil {
    public static boolean isNullOrEmpty(String param) {
        return !isNotNullOrEmpty(param);
    }

    public static boolean isNotNullOrEmpty(String param) {
        return param != null && !param.isEmpty();
    }
}
