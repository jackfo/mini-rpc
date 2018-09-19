package com.cfs.mini.remoting.zookeeper.curator;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.remoting.exchange.suport.AbstractZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ChildListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.util.Collections;
import java.util.List;

public class CuratorZookeeperClient extends AbstractZookeeperClient<CuratorWatcher> {

    private final CuratorFramework client;

    public CuratorZookeeperClient(URL url) {
        super(url);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(url.getBackupAddress()) // 连接地址
                .retryPolicy(new RetryNTimes(1, 1000)) // 重试策略，1 次，间隔 1000 ms
                .connectionTimeoutMs(5000);
        client= builder.build();
        client.start();
    }

    public void createPersistent(String path) {
        try {
            client.create().forPath(path);
        } catch (KeeperException.NodeExistsException e) { // 忽略异常
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void createEphemeral(String path) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (KeeperException.NodeExistsException e) { // 忽略异常
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    public boolean checkExists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) { // 忽略异常
        }
        return false;
    }

    /**
     * 添加真正的监听器
     * */
    public List<String> addTargetChildListener(String path, CuratorWatcher listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public CuratorWatcher createTargetChildListener(String path, ChildListener listener) {
        return new CuratorWatcherImpl(listener);
    }


    private class CuratorWatcherImpl implements CuratorWatcher {

        private volatile ChildListener listener;

        public CuratorWatcherImpl(ChildListener listener) {
            this.listener = listener;
        }

        public void unwatch() {
            this.listener = null;
        }

        /**当节点发生改变,zookeeper会调用这个方法,最终唤醒所有的监听器*/
        @Override
        public void process(WatchedEvent event) throws Exception {
            if (listener != null) {
                String path = event.getPath() == null ? "" : event.getPath();
                listener.childChanged(path,
                        StringUtils.isNotEmpty(path)
                                ? client.getChildren().usingWatcher(this).forPath(path) // 重新发起连接，并传入最新的子节点列表
                                : Collections.<String>emptyList()); //
            }
        }
    }

}
