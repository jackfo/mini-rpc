package com.cfs.mini.registry.zookeeper;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.registry.support.FailbackRegistry;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;

public class ZookeeperRegistry extends FailbackRegistry {

    private final static String DEFAULT_ROOT = "mini";

    private final ZookeeperClient zkClient;

    private final String root;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {

        super(url);

        /**现在是去直接连接zookeeper了*/
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }

        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);

        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }

        this.root = group;

        zkClient = zookeeperTransporter.connect(url);
        // 添加 StateListener 对象。该监听器，在重连时，调用恢复方法。

        //TODO:重连机制没有添加
    }
}
