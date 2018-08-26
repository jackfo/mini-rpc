package com.mini.rpc.core.cluster;


import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;

@SPI
public interface ConfiguratorFactory {
    @Adaptive("protocol")
    Configurator getConfigurator(URL url);
}
