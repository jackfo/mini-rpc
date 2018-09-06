package com.cfs.mini.common.utils;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;

public class ExecutorUtil {

    /**设置相应的线程名*/
    public static URL setThreadName(URL url, String defaultName) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, defaultName);
        name = new StringBuilder(32).append(name).append("-").append(url.getAddress()).toString();
        url = url.addParameter(Constants.THREAD_NAME_KEY, name);
        return url;
    }
}
