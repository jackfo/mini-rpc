package com.cfs.mini.remoting.exchange.suport;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.TimeoutException;
import com.cfs.mini.remoting.exchange.Request;
import com.cfs.mini.remoting.exchange.Response;
import com.cfs.mini.remoting.exchange.ResponseFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultFuture implements ResponseFuture {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    /**通道集合*/
    private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<Long, Channel>();

    /**请求标编号*/
    private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<Long, DefaultFuture>();

    /**创建时间*/
    private final long start = System.currentTimeMillis();

    /**响应*/
    private volatile Response response;

    private final Lock lock = new ReentrantLock();

    private final Condition done = lock.newCondition();

    private final int timeout;
    /**请求编号*/
    private final long id;
    /**通道*/
    private final Channel channel;
    /**请求*/
    private final Request request;

    /**发送请求时间*/
    private volatile long sent;

    public DefaultFuture(Channel channel, Request request, int timeout) {
        this.channel = channel;
        this.request = request;
        this.id = request.getId();
        this.timeout = timeout > 0 ? timeout : channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        // put into waiting map.
        FUTURES.put(id, this);
        CHANNELS.put(id, channel);
    }


    static {
        Thread th = new Thread(new RemotingInvocationTimeoutScan(), "DubboResponseTimeoutScanTimer");
        th.setDaemon(true);
        th.start();
    }

    /**
     * 接受响应
     * */
    public static void received(Channel channel, Response response) {
        try {
            // 移除 FUTURES
            DefaultFuture future = FUTURES.remove(response.getId());
            // 接收结果
            if (future != null) {
                future.doReceived(response);
            } else {
                logger.warn("The timeout response finally returned at "
                        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                        + ", response " + response
                        + (channel == null ? "" : ", channel: " + channel.getLocalAddress()
                        + " -> " + channel.getRemoteAddress()));
            }
            // 移除 CHANNELS
        } finally {
            CHANNELS.remove(response.getId());
        }
    }

    private void doReceived(Response res) {
        // 锁定
        lock.lock();
        try {
            // 设置结果
            response = res;
            // 通知，唤醒等待
            if (done != null) {
                done.signal();
            }
        } finally {
            // 释放锁定
            lock.unlock();
        }
        //TODO:回调机制
    }

    public void cancel() {
        Response errorResult = new Response(id);
        errorResult.setErrorMessage("request future has been canceled.");
        response = errorResult;
        FUTURES.remove(id);
        CHANNELS.remove(id);
    }

    @Override
    public Object get() throws RemotingException {
        return get(timeout);
    }

    @Override
    public Object get(int timeout) throws RemotingException {
        if (timeout <= 0) {
            timeout = Constants.DEFAULT_TIMEOUT;
        }
        // 若未完成，等待
        if (!isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                // 等待完成或超时
                while (!isDone()) {
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            // 未完成，抛出超时异常 TimeoutException
            if (!isDone()) {
                throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
            }
        }
        // 返回响应
        return returnFromResponse();
    }


    private Object returnFromResponse() throws RemotingException {
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null");
        }
        // 正常，返回结果
        if (res.getStatus() == Response.OK) {
            return res.getResult();
        }
        // 超时，抛出 TimeoutException 异常
        if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
            throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
        }
        // 其他，抛出 RemotingException 异常
        throw new RemotingException(channel, res.getErrorMessage());
    }


    @Override
    public boolean isDone() {
        return response != null;
    }

    private long getStartTimestamp() {
        return start;
    }

    private int getTimeout() {
        return timeout;
    }

    private long getId() {
        return id;
    }

    private boolean isSent() {
        return sent > 0;
    }

    private Channel getChannel() {
        return channel;
    }

    /**
     * 后台扫描超时任务
     * */
    private static class RemotingInvocationTimeoutScan implements Runnable {
        public void run() {
            while (true){
                try{
                    for(DefaultFuture future : FUTURES.values()){
                        if(future==null||future.isDone()){
                            if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
                                // 创建超时 Response
                                // create exception response.
                                Response timeoutResponse = new Response(future.getId());
                                // set timeout status.
                                timeoutResponse.setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
                                timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
                                // 响应结果
                                // handle response.
                                DefaultFuture.received(future.getChannel(), timeoutResponse);
                            }
                        }
                    }
                }catch (Throwable e) {
                    logger.error("Exception when scan the timeout invocation of remoting.", e);
                }
            }
        }
    }

    private String getTimeoutMessage(boolean scan) {
        long nowTimestamp = System.currentTimeMillis();
        // 阶段
        return (sent > 0 ? "Waiting server-side response timeout" : "Sending request timeout in client-side")
                // 触发
                + (scan ? " by scan timer" : "") + ". start time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + ","
                // 剩余时间
                + (sent > 0 ? " client elapsed: " + (sent - start)
                + " ms, server elapsed: " + (nowTimestamp - sent)
                : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: "
                + timeout + " ms, request: " + request + ", " +
                // 连接的服务器
                "channel: " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress();
    }
}
