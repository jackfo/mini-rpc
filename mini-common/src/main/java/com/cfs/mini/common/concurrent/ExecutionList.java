package com.cfs.mini.common.concurrent;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ExecutionList {

    static final Logger logger = LoggerFactory.getLogger(ExecutionList.class.getName());

    private static final Executor DEFAULT_EXECUTOR = new ThreadPoolExecutor(1, 10, 60000L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("mini-callback", true));

    public ExecutionList() {
    }


}
