package com.cfs.mini.registry.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import com.cfs.mini.common.utils.UrlUtils;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.Registry;
import com.cfs.mini.rpc.core.RpcException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class AbstractRegistry implements Registry{

    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();

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

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((urls == null || urls.isEmpty())
                && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = new ArrayList<URL>();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }

        Map<String, List<URL>> categoryNotified = notified.get(url);

        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            // 当某个分类的数据为空时，会依然有 urls 。其中 `urls[0].protocol = empty` ，通过这样的方式，处理所有服务提供者为空的情况。
            categoryNotified.put(category, categoryList);

            //TODO:保存到文件
            // 通知监听器
            listener.notify(categoryList);
        }

    }
}
