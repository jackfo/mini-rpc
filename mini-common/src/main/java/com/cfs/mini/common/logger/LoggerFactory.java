package com.cfs.mini.common.logger;


import com.cfs.mini.common.logger.slf4j.Slf4jLoggerAdapter;
import com.cfs.mini.common.logger.support.FailsafeLogger;




public class LoggerFactory {


    private static volatile LoggerAdapter LOGGER_ADAPTER;

    private static FailsafeLogger failsafeLogger;


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



}
