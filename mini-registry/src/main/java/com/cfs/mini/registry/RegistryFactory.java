package com.cfs.mini.registry;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;



@SPI("mini")
public interface RegistryFactory {

    @Adaptive({"protocol"})
    Registry getRegistry(URL url);
}
