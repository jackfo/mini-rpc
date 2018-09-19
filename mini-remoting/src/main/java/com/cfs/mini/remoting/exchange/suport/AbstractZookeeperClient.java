package com.cfs.mini.remoting.exchange.suport;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.zookeeper.ChildListener;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractZookeeperClient<TargetChildListener> implements ZookeeperClient {

    private final URL url;

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }

    private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners = new ConcurrentHashMap<String, ConcurrentMap<ChildListener, TargetChildListener>>();

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

    @Override
    public List<String> addChildListener(String path, final ChildListener listener) {
        // 获得路径下的监听器数组
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (listeners == null) {
            childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, TargetChildListener>());
            listeners = childListeners.get(path);
        }

        // 获得是否已经有该监听器
        TargetChildListener targetListener = listeners.get(listener);

        // 监听器不存在，进行创建
        if (targetListener == null) {
            listeners.putIfAbsent(listener, createTargetChildListener(path, listener));
            targetListener = listeners.get(listener);
        }

        // 向 Zookeeper ，真正发起订阅
        return addTargetChildListener(path, targetListener);

    }

    protected abstract void createPersistent(String path);

    protected abstract void createEphemeral(String path);

    protected abstract boolean checkExists(String path);

    protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);


    protected abstract List<String> addTargetChildListener(String path, TargetChildListener listener);
}
