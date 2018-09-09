package com.cfs.mini.registry.zookeeper;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.registry.support.FailbackRegistry;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;
import com.cfs.mini.rpc.core.RpcException;

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

    @Override
    protected void doRegister(URL url) {
        try {
            zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String toRootPath() {
        return root;
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }
}
