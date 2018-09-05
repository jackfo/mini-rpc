package com.cfs.mini.remoting.zookeeper.zkclient;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;

public class ZkclientZookeeperTransporter implements ZookeeperTransporter {
    @Override
    public ZookeeperClient connect(URL url) {
        return new ZkclientZookeeperClient(url);
    }
}
