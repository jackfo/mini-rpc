package com.cfs.mini.remoting.zookeeper.zkclient;

import com.cfs.mini.common.concurrent.ListenableFutureTask;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ZkClientWrapper {

    private Logger logger = LoggerFactory.getLogger(ZkClientWrapper.class);

    private ZkClient client;

    private long timeout;

    private ListenableFutureTask<ZkClient> listenableFutureTask;

    private volatile boolean started = false;

    public ZkClientWrapper(final String serverAddr,long timeout){
        this.timeout = timeout;

        /**
         *  采用ListenableFutureTask这种future这种模式主要有两个原因
         *  1.能后在执行完毕后执行一些监听线程
         *  2.能够获取所有执行超时的zkClient
         */
        listenableFutureTask = ListenableFutureTask.create(new Callable<ZkClient>() {
            @Override
            public ZkClient call() throws Exception {
                return new ZkClient(serverAddr, Integer.MAX_VALUE); // 连接超时设置为无限，在 {@link #start()} 方法中，通过 listenableFutureTask ，实现超时。
            }
        });
    }

    public void start() {
        if (!started) {
            Thread connectThread = new Thread(listenableFutureTask);
            connectThread.setName("Mini-Zkclient-Connector");
            connectThread.setDaemon(true);
            connectThread.start();
            // 连接。若超时，打印错误日志，不会抛出异常。
            try {
                client = listenableFutureTask.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Throwable t) {
                logger.error("Timeout! zookeeper server can not be connected in : " + timeout + "ms!", t);
            }
            started = true;
        } else {
            logger.warn("Zkclient has already been started!");
        }
    }


    public List<String> subscribeChildChanges(String path, final IZkChildListener listener) {
        if(client==null){
            throw new RuntimeException("client is null");
        }
        return client.subscribeChildChanges(path, listener);
    }


    public void delete(String path) {
        client.delete(path);
    }

}
