package com.cfs.mini.registry.zookeeper;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.utils.UrlUtils;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.support.FailbackRegistry;
import com.cfs.mini.remoting.zookeeper.ChildListener;
import com.cfs.mini.remoting.zookeeper.ZookeeperClient;
import com.cfs.mini.remoting.zookeeper.ZookeeperTransporter;
import com.cfs.mini.rpc.core.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ZookeeperRegistry extends FailbackRegistry {

    private final static String DEFAULT_ROOT = "mini";

    private final ZookeeperClient zkClient;

    private final String root;

    /**
     * 监听器集合
     */
    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildListener>>();

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {

        super(url);

        /**现在是去直接连接zookeeper了*/
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }

        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);

        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }

        this.root = group;

        zkClient = zookeeperTransporter.connect(url);
        // 添加 StateListener 对象。该监听器，在重连时，调用恢复方法。

        //TODO:重连机制没有添加
    }

    @Override
    protected void doRegister(URL url) {
        try {
            zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        try {
            //处理所有Service层发起的订阅
            if(Constants.ANY_VALUE.equals(url.getServiceInterface())){
                throw new RpcException("这部分暂时未处理");
            }else{
                // 子节点数据数组
                List<URL> urls = new ArrayList<URL>();
                for (String path : toCategoriesPath(url)) {
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    if (listeners == null) { // 不存在，进行创建
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }

                    /**获取URL对应的监听节点,最终目的是节点发生改变,唤醒NotifyListener*/
                    ChildListener zkListener = listeners.get(listener);
                    if (zkListener == null) { // 不存在 ChildListener 对象，进行创建 ChildListener 对象
                        listeners.putIfAbsent(listener, new ChildListener() {
                            public void childChanged(String parentPath, List<String> currentChilds) {
                                ZookeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }

                    /**创建一个持久性节点*/
                    zkClient.create(path, false);

                    /**添加一个监听器*/
                    List<String> children = zkClient.addChildListener(path, zkListener);

                    if (children != null) {
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }

                }
                //做首次唤醒操作,因为这个时候不管zookeeper是否发生监听节点的变化,都需要统计所有节点数据
                notify(url, listener, urls);
            }

        }catch (Throwable e){
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }

    }

    /**
     * 分类路径分组
     * */
    private String[] toCategoriesPath(URL url) {
        String[] categories;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) { // * 时，
            categories = new String[]{Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY,
                    Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY};
        } else {
            categories = url.getParameter(Constants.CATEGORY_KEY, new String[]{Constants.DEFAULT_CATEGORY});
        }
        // 获得分类路径数组
        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String toRootPath() {
        return root;
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }


    /**
     * 获得 providers 中，和 consumer 匹配的 URL 数组
     *
     * 若不存在匹配，则创建 `empty://` 的 URL返回。通过这样的方式，可以处理类似服务提供者为空的情况。
     *
     * @param consumer 用于匹配 URL
     * @param path 被匹配的 URL 的字符串
     * @param providers 匹配的 URL 数组
     * @return 匹配的 URL 数组
     */
    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        // 获得 providers 中，和 consumer 匹配的 URL 数组
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        // 若不存在匹配，则创建 `empty://` 的 URL返回
        if (urls == null || urls.isEmpty()) {
            int i = path.lastIndexOf('/');
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

    /**
     * 获得 providers 中，和 consumer 匹配的 URL 数组
     *
     * @param consumer 用于匹配 URL
     * @param providers 被匹配的 URL 的字符串
     * @return 匹配的 URL 数组
     */
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && !providers.isEmpty()) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) { // 是 url
                    URL url = URL.valueOf(provider); // 将字符串转化成 URL
                    if (UrlUtils.isMatch(consumer, url)) { // 匹配
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }


}
