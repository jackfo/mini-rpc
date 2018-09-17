package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;
import com.cfs.mini.rpc.core.cluster.LoadBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);


    protected final Directory<T> directory;

    protected final boolean availablecheck;

    /**
     * 粘滞连接,尽量使客户端向同一Invoker发起调用
     * */
    private volatile Invoker<T> stickyInvoker = null;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractClusterInvoker(Directory<T> directory) {
        this(directory, directory.getUrl());
    }

    public AbstractClusterInvoker(Directory<T> directory, URL url) {
        // 初始化 directory
        if (directory == null) {
            throw new IllegalArgumentException("service directory == null");
        }
        this.directory = directory;
        // sticky: invoker.isAvailable() should always be checked before using when availablecheck is true.
        // 初始化 availablecheck
        this.availablecheck = url.getParameter(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Constants.DEFAULT_CLUSTER_AVAILABLE_CHECK);
    }


    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
    }

    @Override
    public boolean isAvailable() {
        // 如有粘滞连接 Invoker ，基于它判断。
        Invoker<T> invoker = stickyInvoker; // 指向，避免并发
        if (invoker != null) {
            return invoker.isAvailable();
        }
        // 基于 Directory 判断
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            directory.destroy();
        }
    }

    /**
     * 负载均衡,选用相应的Invoker
     * */
    protected Invoker<T> select(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {

       if(invokers==null||invokers.isEmpty()){
           return null;
       }

       String methodName = invocation == null ? "" : invocation.getMethodName();

       /**
        * 获取相应methodName.sticky的值
        * */
       boolean sticky = invokers.get(0).getUrl().getMethodParameter(methodName, Constants.CLUSTER_STICKY_KEY, Constants.DEFAULT_CLUSTER_STICKY);

        {
            if (stickyInvoker != null && !invokers.contains(stickyInvoker)) {
                stickyInvoker = null;
            }
            /**
             * 如果具备粘滞特性,直接进行返回
             * */
            if (sticky && stickyInvoker != null && (selected == null || !selected.contains(stickyInvoker))) {
                // 若开启排除非可用的 Invoker 的特性，则校验 stickyInvoker 是否可用。若可用，则进行返回
                if (availablecheck && stickyInvoker.isAvailable()) {
                    return stickyInvoker;
                }
            }
        }

        Invoker<T> invoker = doselect(loadbalance, invocation, invokers, selected);

        if (sticky) {
            stickyInvoker = invoker;
        }
        return invoker;
    }

    /**
     * 选择相应的Invoker
     * 第一种:如果invoker只存在一个则直接返回
     * 第二种:如果invoker存在两个,则做一次轮训处理
     * 第三种:如果invoker大于3,则采用负载均衡
     *
     * */
    private Invoker<T> doselect(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {

        if(invokers==null||invokers.isEmpty()){
             return null;
        }

        if(invokers.size()==1){
            return invokers.get(0);
        }


        if(invokers.size()==2 && selected != null && !selected.isEmpty()){
            return selected.get(0)==invokers.get(0)?invokers.get(1):invokers.get(0);
        }

        Invoker<T> invoker = loadbalance.select(invokers, getUrl(), invocation);

        /**
         * 在选择了相应的invoker之后,还需要做一次判断,来看这个Invoker是否可以使用
         *
         * 第一种是如果这个invoker已经在选择的集合中需要重选
         * 第二种是当前invoker不可用时需要重选
         * */
        if((selected!=null&&selected.contains(invoker))||(!invoker.isAvailable()&&getUrl()!=null&&availablecheck)){

            try{

                Invoker<T> rinvoker = reselect(loadbalance, invocation, invokers, selected, availablecheck);
                if (rinvoker != null) {
                    invoker = rinvoker;
                } else {
                    // 【第五种】看下第一次选的位置，如果不是最后，选+1位置.
                    int index = invokers.indexOf(invoker);
                    try {
                        // Avoid collision
                        // 最后在避免碰撞
                        invoker = index < invokers.size() - 1 ? invokers.get(index + 1) : invoker;
                    } catch (Exception e) {
                        logger.warn(e.getMessage() + " may because invokers list dynamic change, ignore.", e);
                    }
                }

            }catch (Throwable t){
                logger.error("clustor relselect fail reason is :" + t.getMessage() + " if can not slove ,you can set cluster.availablecheck=false in url", t);
            }
        }
        return invoker;
    }

    /**
     * 重选invoker
     * */
    private Invoker<T> reselect(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected, boolean availablecheck) throws RpcException {

        List<Invoker<T>> reselectInvokers = new ArrayList<Invoker<T>>(invokers.size() > 1 ? (invokers.size() - 1) : invokers.size());

        /**
         * 将所有尚未被选中且可使用的添加到集合中去
         * 使用负载均衡,在这里面再选择一个
         * */
        if(availablecheck){
            for(Invoker<T> invoker:invokers){
                if(invoker.isAvailable()){
                    if(selected==null||!selected.contains(invoker)){
                        reselectInvokers.add(invoker);
                    }
                }
            }

            if (!reselectInvokers.isEmpty()) {
                return loadbalance.select(reselectInvokers, getUrl(), invocation);
            }
        } else { // do not check invoker.isAvailable()
            // 获得非选择过的 Invoker 集合
            for (Invoker<T> invoker : invokers) {
                if (selected == null || !selected.contains(invoker)) {
                    reselectInvokers.add(invoker);
                }
            }
            // 使用 Loadbalance ，选择一个 Invoker 对象。
            if (!reselectInvokers.isEmpty()) {
                return loadbalance.select(reselectInvokers, getUrl(), invocation);
            }
        }

        {
            // 获得选择过的，并且可用的 Invoker 集合
            if (selected != null) {
                for (Invoker<T> invoker : selected) {
                    if ((invoker.isAvailable()) // available first
                            && !reselectInvokers.contains(invoker)) {
                        reselectInvokers.add(invoker);
                    }
                }
            }
            // 使用 Loadbalance ，选择一个 Invoker 对象。
            if (!reselectInvokers.isEmpty()) {
                return loadbalance.select(reselectInvokers, getUrl(), invocation);
            }
        }

        return null;

    }

    /**
     *
     * */
    @Override
    public Result invoke(final Invocation invocation) throws RpcException {
        // 获得所有服务提供者 Invoker 集合
        List<Invoker<T>> invokers = list(invocation);

        LoadBalance loadBalance ;
        if(invokers!=null&&!invokers.isEmpty()){
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl().getMethodParameter(invocation.getMethodName(),Constants.LOADBALANCE_KEY,Constants.DEFAULT_LOADBALANCE));
        }else{
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
        }
        //todo：设置异步执行

        return doInvoke(invocation,invokers,loadBalance);
    }

    protected abstract Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException;

    protected List<Invoker<T>> list(Invocation invocation) throws RpcException {
        return directory.list(invocation);
    }


    protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation) {
        if (invokers == null || invokers.isEmpty()) {
            throw new RpcException("Failed to invoke the method "
                    + invocation.getMethodName() + " in the service " + getInterface().getName()
                    + ". No provider available for the service " + directory.getUrl().getServiceKey()
                    + " from registry " + directory.getUrl().getAddress()
                    + " on the consumer " + NetUtils.getLocalHost()
                    + " using the dubbo version " + Version.getVersion()
                    + ". Please check if the providers have been started and registered.");
        }
    }

    protected void checkWhetherDestroyed() {
        if (destroyed.get()) {
            throw new RpcException("Rpc cluster invoker for " + getInterface() + " on consumer " + NetUtils.getLocalHost()
                    + " use dubbo version " + Version.getVersion()
                    + " is now destroyed! Can not invoke any more.");
        }
    }

}
