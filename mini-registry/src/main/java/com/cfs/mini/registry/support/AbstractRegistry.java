package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.Registry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class AbstractRegistry implements Registry{


    private final Set<URL> registered = new ConcurrentHashSet<URL>();

    public  static final String module = AbstractRegistry.class.getName();

    public static Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    /**定义指定URL的监听器集合*/
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

    public AbstractRegistry(URL url) {
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url,module);
        }
        // 添加到 registered 集合
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {

    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {

        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }
        // 添加到 subscribed 集合
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }
}
