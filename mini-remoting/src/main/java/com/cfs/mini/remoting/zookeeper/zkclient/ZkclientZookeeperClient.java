package com.cfs.mini.remoting.zookeeper.zkclient;

import com.cfs.mini.common.URL;

import com.cfs.mini.remoting.exchange.suport.AbstractZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ChildListener;
import org.I0Itec.zkclient.IZkChildListener;

import java.util.List;

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

    @Override
    protected IZkChildListener createTargetChildListener(String path, ChildListener listener) {
        return new IZkChildListener() {
            public void handleChildChange(String parentPath, List<String> currentChilds)
                    throws Exception {
                listener.childChanged(parentPath, currentChilds);
            }
        };
    }

    @Override
    protected List<String> addTargetChildListener(String path, IZkChildListener iZkChildListener) {
        return client.subscribeChildChanges(path, iZkChildListener);
    }
}
