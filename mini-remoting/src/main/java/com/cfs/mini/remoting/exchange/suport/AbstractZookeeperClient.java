package com.cfs.mini.remoting.exchange.suport;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;

public abstract class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

    private final URL url;

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }

    @Override
    public void create(String path, boolean ephemeral) {
        // 循环创建父路径
        int i = path.lastIndexOf('/');
        if (i > 0) {
            String parentPath = path.substring(0, i);
            if (!checkExists(parentPath)) {
                create(parentPath, false);
            }
        }
        // 创建临时节点
        if (ephemeral) {
            createEphemeral(path);
            // 创建持久节点
        } else {
            createPersistent(path);
        }
    }

    protected abstract void createPersistent(String path);

    protected abstract void createEphemeral(String path);

    protected abstract boolean checkExists(String path);
}
