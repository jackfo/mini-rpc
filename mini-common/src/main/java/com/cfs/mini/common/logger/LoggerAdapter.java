package com.cfs.mini.common.logger;


import com.cfs.mini.common.extension.SPI;

import java.io.File;

@SPI
public interface LoggerAdapter {

    Logger getLogger(Class<?> key);

    Logger getLogger(String key);


    //设置日志相应级别
    Level getLevel();

    void setLevel(Level level);


    //设置相应的文件
    File getFile();

    void setFile(File file);



}
