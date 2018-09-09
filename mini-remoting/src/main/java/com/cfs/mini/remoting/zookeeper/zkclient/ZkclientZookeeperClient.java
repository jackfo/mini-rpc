package com.cfs.mini.remoting.zookeeper.zkclient;

import com.cfs.mini.common.URL;

import com.cfs.mini.remoting.exchange.suport.AbstractZookeeperClient;
import org.I0Itec.zkclient.IZkChildListener;

public class ZkclientZookeeperClient extends AbstractZookeeperClient<IZkChildListener> {


    private final ZkClientWrapper client;

    public ZkclientZookeeperClient(URL url) {
        super(url);
        // 创建 client 对象
        client = new ZkClientWrapper(url.getBackupAddress(), 30000);
        //添加相应监听事件
        client.start();
    }

    @Override
    protected void createPersistent(String path) {

    }

    @Override
    protected void createEphemeral(String path) {

    }

    @Override
    protected boolean checkExists(String path) {
        return false;
    }
}
