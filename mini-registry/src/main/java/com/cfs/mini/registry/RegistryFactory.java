package com.cfs.mini.registry;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;



@SPI("mini")
public interface RegistryFactory {

    /**
     * 动态适配类会根据相关URL找到对应的实现类
     * */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);
}
