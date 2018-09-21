package com.cfs.mini.remoting.zookeeper;


import java.util.List;

/**
 * zookeeper客户端
 * */
public interface ZookeeperClient {

    void create(String path, boolean ephemeral);

    List<String> addChildListener(String path, ChildListener listener);

    /**
     * 删除节点
     * */
    void delete(String path);
}
