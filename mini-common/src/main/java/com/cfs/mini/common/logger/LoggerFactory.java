package com.cfs.mini.common.logger;


import com.cfs.mini.common.logger.slf4j.Slf4jLoggerAdapter;
import com.cfs.mini.common.logger.support.FailsafeLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class LoggerFactory {


    private static volatile LoggerAdapter LOGGER_ADAPTER;

    private static FailsafeLogger failsafeLogger;

    private static final ConcurrentMap<String, FailsafeLogger> LOGGERS = new ConcurrentHashMap<String, FailsafeLogger>();



    static {
        setLoggerAdapter(new Slf4jLoggerAdapter());
    }

    private LoggerFactory() {
    }

    public static void setLoggerAdapter(LoggerAdapter loggerAdapter) {
        if (loggerAdapter != null) {
            // 获得 Logger 对象，并打印日志，提示设置后的 LoggerAdapter 实现类
            Logger logger = loggerAdapter.getLogger(LoggerFactory.class.getName());
            failsafeLogger =  new FailsafeLogger(logger);

        }
    }

    //TODO:目前只设置一种日志方式
    public static Logger getLogger(Class<?> key) {
        return failsafeLogger;
    }

    public static Logger getLogger(String key) {
        // 从缓存中，获得 Logger 对象
        FailsafeLogger logger = LOGGERS.get(key);
        // 不存在，则进行创建，并进行缓存
        if (logger == null) {
            LOGGERS.putIfAbsent(key, new FailsafeLogger(LOGGER_ADAPTER.getLogger(key)));
            logger = LOGGERS.get(key);
        }
        return logger;
    }



}
