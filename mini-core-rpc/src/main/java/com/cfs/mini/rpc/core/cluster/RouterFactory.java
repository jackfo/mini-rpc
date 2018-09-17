package com.cfs.mini.rpc.core.cluster;


import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;

@SPI
public interface RouterFactory {

    /**
     * Create router.
     *
     * 创建 Router 对象
     *
     * @param url
     * @return router
     */
    @Adaptive("protocol")
    Router getRouter(URL url);

}
