package com.cfs.mini.registry.zookeeper;

import com.cfs.mini.common.URL;
import com.cfs.mini.registry.Registry;
import com.cfs.mini.registry.support.AbstractRegistryFactory;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;

/**
 * 获取相应的Zookeeper工厂
 * */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {


    private ZookeeperTransporter zookeeperTransporter;

    public void setZookeeperTransporter(ZookeeperTransporter zookeeperTransporter) {
        this.zookeeperTransporter = zookeeperTransporter;
    }

    @Override
    protected Registry createRegistry(URL url) {
        return new ZookeeperRegistry(url, zookeeperTransporter);
    }
}
