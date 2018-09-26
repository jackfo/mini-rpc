package com.cfs.mini.common.threadpool;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;

import java.util.concurrent.Executor;

@SPI("fixed")
public interface ThreadPool {

    @Adaptive({Constants.THREADPOOL_KEY})
    Executor getExecutor(URL url);

}
