package com.cfs.mini.remoting.zookeeper;


/**
 * zookeeper客户端
 * */
public interface ZookeeperClient {

    void create(String path, boolean ephemeral);
}
