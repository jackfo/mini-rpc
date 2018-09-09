package com.cfs.mini.config;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Protocol;
import com.cfs.mini.rpc.core.cluster.Cluster;
import com.cfs.mini.rpc.core.service.GenericService;
import com.cfs.mini.rpc.core.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**判定该服务是否已经销毁*/
    private transient volatile boolean destroyed;

    /**Service引用对象*/
    private transient volatile T ref;

    /**检验是否已经初始化1*/
    private transient volatile boolean initialized;


    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();


    /**接口名称*/
    private String interfaceName;

    private ConsumerConfig consumer;

    private Class<?> interfaceClass;

    private String url;

    /***服务引用的集合*/
    private final List<URL> urls = new ArrayList<URL>();

    private static final Protocol refprotocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private transient volatile Invoker<?> invoker;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**方法集合*/
    private List<MethodConfig> methods;


    public synchronized T get() {
        // 已销毁，不可获得
        if (destroyed) {
            throw new IllegalStateException("Already destroyed!");
        }
        // 初始化
        if (ref == null) {
            init();
        }
        return ref;
    }


    private void init(){

        if(initialized){
            return;
        }
        initialized = true;

        if(interfaceName==null||interfaceName.length()==0){
            throw new IllegalStateException("<mini:rereference> interface not allow null!");
        }

        //检验相应ConsumerConfig文件配置

        checkDefault();

        appendProperties(this);

        //TODO:设置generic属性

        if (ProtocolUtils.isGeneric(getGeneric())) {
            interfaceClass = GenericService.class;
            // 普通接口的实现
        } else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            // 校验接口和方法
            checkInterfaceAndMethods(interfaceClass, methods);
        }

        Map<String, String> map = new HashMap<String, String>();

        ref = createProxy(map);

    }

    private void checkDefault(){
        if (consumer == null) {
            consumer = new ConsumerConfig();
        }
        appendProperties(consumer);
    }


    private T createProxy(Map<String, String> map) {

        //创建相应的URL
        URL tmpUrl = new URL("temp", "localhost", 0, map);

        /**检测是否是本地引用*/
        final boolean isJvmRefer;

        /**
         * 如果inJvm这个属性存在,直接以这个属性为准
         * */
        if(isInjvm() == null){
            /**设置默认属性为false*/
            isJvmRefer = false;
        }else {
            isJvmRefer = isInjvm();
        }


        /**采用本地引用,则进行直接调用*/
        if(isJvmRefer){
            URL url = new URL(Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0, interfaceClass.getName()).addParameters(map);
            invoker = refprotocol.refer(interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());
            }
        /**正常流程 一般采用远程调用*/
        }else{

            if(url!=null&&url.length()>0){
                //通过正则进行相应的URL拆分
                String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(url);

                //循环数组,添加到URL中去
                if(us!=null&&us.length>0){
                    for(String u:us){
                        URL url = URL.valueOf(u);

                        //设置默认路径
                        if (url.getPath() == null || url.getPath().length() == 0) {
                            url = url.setPath(interfaceName);
                        }

                        // 注册中心的地址，带上服务引用的配置参数
                        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                            urls.add(url.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                            // 服务提供者的地址
                        } else {
                            throw new RuntimeException("注册协议不是registry");
                            //urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }

                /**根据注册中心进行获取*/
            }else{
                // 加载注册中心 URL 数组
                List<URL> us = loadRegistries(false);
                if (us != null && !us.isEmpty()) {
                    for (URL u : us) {
                        // 注册中心的地址，带上服务引用的配置参数,注册中心，带上服务引用的配置参数
                        urls.add(u.addParameterAndEncoded(Constants.REFER_KEY, StringUtils.toQueryString(map)));
                    }
                }

                if(urls.isEmpty()){
                    throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <mini:registry address=\"...\" /> to your spring config.");
                }
            }

            /**开始进行相关引用服务*/
            if(urls.size()==1){
                invoker = refprotocol.refer(interfaceClass, urls.get(0));
            }else{
                // 循环 `urls` ，引用服务，返回 Invoker 对象
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                for (URL url : urls) {
                    // 引用服务
                    invokers.add(refprotocol.refer(interfaceClass, url));
                    // 使用最后一个注册中心的 URL
                    if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                        registryURL = url;
                    }
                }
                // 有注册中心
                if (registryURL != null) { // registry url is available
                    // 对有注册中心的 Cluster 只用 AvailableCluster
                    URL u = registryURL.addParameter(Constants.CLUSTER_KEY, AvailableCluster.NAME);
                    invoker = cluster.join(new StaticDirectory(u, invokers));
                    // 无注册中心，全部都是服务直连
                } else { // not a registry url
                    invoker = cluster.join(new StaticDirectory(invokers));
                }
            }
        }



        //TODO:检查invoker是否可以用

        // 创建 Service 代理对象
        // create service proxy
        return (T) proxyFactory.getProxy(invoker);

    }




}
