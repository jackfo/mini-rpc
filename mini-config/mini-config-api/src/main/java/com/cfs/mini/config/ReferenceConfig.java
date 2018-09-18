package com.cfs.mini.config;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.bytecode.Wrapper;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.utils.ConfigUtils;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Protocol;
import com.cfs.mini.rpc.core.ProxyFactory;
import com.cfs.mini.rpc.core.cluster.Cluster;
import com.cfs.mini.rpc.core.cluster.directory.StaticDirectory;
import com.cfs.mini.rpc.core.cluster.support.AvailableCluster;
import com.cfs.mini.rpc.core.service.GenericService;
import com.cfs.mini.rpc.core.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static com.cfs.mini.common.utils.NetUtils.isInvalidLocalHost;

public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**判定该服务是否已经销毁*/
    private transient volatile boolean destroyed;

    /**Service引用对象*/
    private transient volatile T ref;

    /**检验是否已经初始化1*/
    private transient volatile boolean initialized;


    protected String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();


    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

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


    public String getInterface() {
        return interfaceName;
    }

    /**方法集合*/
    private List<MethodConfig> methods;

    private static final ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

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

        // 若未设置 `generic` 属性，使用 `ConsumerConfig.generic` 属性。
        if (getGeneric() == null && getConsumer() != null) {
            setGeneric(getConsumer().getGeneric());
        }

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
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;

        if (resolve == null || resolve.length() == 0) {
            // 默认先加载，`${user.home}/dubbo-resolve.properties` 文件 ，无需配置
            resolveFile = System.getProperty("mini.resolve.file");
            if (resolveFile == null || resolveFile.length() == 0) {
                File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                if (userResolveFile.exists()) {
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            // 存在 resolveFile ，则进行文件读取加载。
            if (resolveFile != null && resolveFile.length() > 0) {
                Properties properties = new Properties();
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(new File(resolveFile));
                    properties.load(fis);
                } catch (IOException e) {
                    throw new IllegalStateException("Unload " + resolveFile + ", cause: " + e.getMessage(), e);
                } finally {
                    try {
                        if (null != fis) fis.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
                resolve = properties.getProperty(interfaceName);
            }
        }

        // 设置直连提供者的 url
        if (resolve != null && resolve.length() > 0) {
            url = resolve;
            if (logger.isWarnEnabled()) {
                if (resolveFile != null && resolveFile.length() > 0) {
                    logger.warn("Using default mini resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                } else {
                    logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                }
            }
        }

        //TODO: 从 ConsumerConfig 对象中，读取 application、module、registries、monitor 配置对象。

        if (application != null) {
            if (registries == null) {
                registries = application.getRegistries();
            }

        }


        Map<String, String> map = new HashMap<String, String>();
        Map<Object, Object> attributes = new HashMap<Object, Object>();
        map.put(Constants.SIDE_KEY, Constants.CONSUMER_SIDE);
        map.put(Constants.MINI_VERSION_KEY, Version.getVersion());
        map.put(Constants.TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        if (ConfigUtils.getPid() > 0) {
            map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
        }
        if (!isGeneric()) {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put("revision", revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames(); // 获得方法数组
            if (methods.length == 0) {
                logger.warn("NO method found in service interface " + interfaceClass.getName());
                map.put("methods", Constants.ANY_VALUE);
            } else {
                map.put("methods", StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }

        map.put(Constants.INTERFACE_KEY, interfaceName);
        // 将各种配置对象，添加到 `map` 集合中。
        appendParameters(map, application);
        appendParameters(map, consumer, Constants.DEFAULT_KEY);
        appendParameters(map, this);
        String prefix = StringUtils.getServiceKey(map);
        if (methods != null && !methods.isEmpty()) {
            for (MethodConfig method : methods) {
                // 将 MethodConfig 对象，添加到 `map` 集合中。
                appendParameters(map, method, method.getName());
                // 当 配置了 `MethodConfig.retry = false` 时，强制禁用重试
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
            }
        }

        String hostToRegistry = ConfigUtils.getSystemProperty(Constants.MINI_IP_TO_REGISTRY);
        if (hostToRegistry == null || hostToRegistry.length() == 0) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + Constants.MINI_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(Constants.REGISTER_IP_KEY, hostToRegistry);

        ref = createProxy(map);

    }

    private void checkDefault(){
        if (consumer == null) {
            consumer = new ConsumerConfig();
        }
        appendProperties(consumer);
    }


    /**
     * 创建相应的代理对象
     * */
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


    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }




}
