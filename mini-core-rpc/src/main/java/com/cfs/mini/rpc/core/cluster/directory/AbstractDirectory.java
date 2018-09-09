package com.cfs.mini.rpc.core.cluster.directory;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.rpc.core.cluster.Directory;
import com.cfs.mini.rpc.core.cluster.Router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Directory 抽象实现类，实现了公用的路由规则的逻辑
 * */
public abstract class AbstractDirectory<T> implements Directory<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    /**是否已经销毁*/
    private volatile boolean destroyed = false;

    /**注册中心URL*/
    private final URL url;

    /**消费者URL*/
    private volatile URL consumerUrl;

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, List<Router> routers) {
        this(url, url, routers);
    }

    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
        this.consumerUrl = consumerUrl;
        // 设置 Router 数组
        setRouters(routers);
    }




    protected void setRouters(List<Router> routers) {
       //添加相应的路由选择
    }
}
