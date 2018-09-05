package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;

public class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

    private final URL url;

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }
}
