package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.registry.NotifyListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FailbackRegistry extends AbstractRegistry{

    /**
     * 失败取消订阅失败的监听器集合
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();


    /**
     * 失败发起订阅失败的监听器集合
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

    /**
     * 失败通知通知的 URL 集合
     */
    private final ConcurrentMap<URL, Map<NotifyListener, List<URL>>> failedNotified = new ConcurrentHashMap<URL, Map<NotifyListener, List<URL>>>();

    public FailbackRegistry(URL url) {


        super(url);

        //TODO:将重试机制添加到线程池
    }

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    @Override
    public void register(URL url) {

        if (destroyed.get()){
            return;
        }
        // 添加到 `registered` 变量
        super.register(url);

        doRegister(url);

    }

    protected abstract void doRegister(URL url);


    @Override
    public void subscribe(URL url, NotifyListener listener) {

       if(destroyed.get()){
           return;
       }

       /**
        *移除所有URL中listener为空的订阅
        * */
       super.subscribe(url,listener);

       removeFailedSubscribed(url, listener);

       try{

           doSubscribe(url,listener);
       }catch (Exception e){
           throw new RuntimeException("订阅失败");
       }
    }

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    /**
     * 移除出 `failedSubscribed` `failedUnsubscribed` `failedNotified`
     *
     * @param url URL
     * @param listener 监听器
     */
    private void removeFailedSubscribed(URL url, NotifyListener listener) {
        // 移除出 `failedSubscribed`
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        // 移除出 `failedUnsubscribed`
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        // 移除出 `failedNotified`
        Map<NotifyListener, List<URL>> notified = failedNotified.get(url);
        if (notified != null) {
            notified.remove(listener);
        }
    }


    @Override
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }

        // 通知监听器
        try {
            doNotify(url, listener, urls);
        } catch (Exception t) {
            // 将失败的通知记录到 `failedNotified`，定时重试
            // Record a failed registration request to a failed list, retry regularly
            Map<NotifyListener, List<URL>> listeners = failedNotified.get(url);
            if (listeners == null) {
                failedNotified.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, List<URL>>());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            logger.error("Failed to notify for subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    protected void doNotify(URL url, NotifyListener listener, List<URL> urls) {
        super.notify(url, listener, urls);
    }


    @Override
    public void unregister(URL url) {
        // 已销毁，跳过
        if (destroyed.get()){
            return;
        }
        // 移除出 `registered` 变量
        super.unregister(url);

        // 向注册中心发送取消注册请求
        try {
            // Sending a cancellation request to the server side
            doUnregister(url);
        } catch (Exception e) {
            logger.error("取消注册失败"+e.getMessage());
        }


    }

    protected abstract void doUnregister(URL url);
}
