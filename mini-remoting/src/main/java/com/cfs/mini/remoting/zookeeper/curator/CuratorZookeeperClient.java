package com.cfs.mini.remoting.zookeeper.curator;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.exchange.suport.AbstractZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

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
}
