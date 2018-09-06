package com.cfs.mini.remoting.zookeeper.curator;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;

public class CuratorZookeeperTransporter implements ZookeeperTransporter {

    public ZookeeperClient connect(URL url) {
        return new CuratorZookeeperClient(url);
    }
}
