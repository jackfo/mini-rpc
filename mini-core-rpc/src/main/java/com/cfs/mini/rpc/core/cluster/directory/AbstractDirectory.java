package com.cfs.mini.rpc.core.cluster.directory;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;
import com.cfs.mini.rpc.core.cluster.Router;
import com.cfs.mini.rpc.core.cluster.RouterFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Directory 抽象实现类，实现了公用的路由规则的逻辑
 * */
public abstract class AbstractDirectory<T> implements Directory<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    /**是否已经销毁*/
    private volatile boolean destroyed = false;

    /**注册中心URL*/
    private final URL url;

    /**消费者URL*/
    private volatile URL consumerUrl;

    /**路由数组*/
    private volatile List<Router> routers;

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, List<Router> routers) {
        this(url, url, routers);
    }

    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
        this.consumerUrl = consumerUrl;
        // 设置 Router 数组
        setRouters(routers);
    }


    public URL getConsumerUrl() {
        return consumerUrl;
    }


    protected void setRouters(List<Router> routers) {
        // copy list // 复制 routers ，因为下面要修改
        routers = routers == null ? new ArrayList<Router>() : new ArrayList<Router>(routers);
        // append url router
        // 拼接 `url` 中，配置的路由规则
        String routerkey = url.getParameter(Constants.ROUTER_KEY);
        if (routerkey != null && routerkey.length() > 0) {
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routerkey);
            routers.add(routerFactory.getRouter(url));
        }
        // append mock invoker selector
        //routers.add(new MockInvokersSelector());
        // 排序
        Collections.sort(routers);
        // 赋值给属性
        this.routers = routers;
    }

    public boolean isDestroyed() {
        return destroyed;
    }


    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;

    /**
     * 根据invocation找到所有的invoker
     * */
    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if(destroyed){
            throw new RpcException("Directory already destroyed .url:"+getUrl());
        }

        List<Invoker<T>> invokers = doList(invocation);

        List<Router> localRouters = this.routers;

        if(localRouters!=null&&!localRouters.isEmpty()){
            for (Router router:localRouters){
                try {
                    if (router.getUrl() == null || router.getUrl().getParameter(Constants.RUNTIME_KEY, false)) {
                        invokers = router.route(invokers, getConsumerUrl(), invocation);
                    }
                }catch (Throwable t){
                    logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                }
            }
        }
        return invokers;
    }


    @Override
    public URL getUrl() {
        return url;
    }



    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }



    @Override
    public void destroy() {
        destroyed = true;
    }
}
