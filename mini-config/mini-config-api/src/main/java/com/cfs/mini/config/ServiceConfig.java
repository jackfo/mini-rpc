package com.cfs.mini.config;

import com.cfs.mini.common.utils.NamedThreadFactory;
import com.cfs.mini.common.utils.StringUtils;
import com.mini.rpc.core.service.GenericService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServiceConfig<T> extends AbstractServiceConfig{


    private String interfaceName;
    /**
     * {@link #interfaceName} 对应的接口类
     *
     * 非配置
     */
    private Class<?> interfaceClass;

    /**
     * 延迟服务暴露线程池,一个定时的线程池
     * */
    private static final ScheduledExecutorService delayExportExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MiniServiceDelayExporter", true));


    private T ref;


    //注册中心的配置数组
    protected List<ProtocolConfig> protocols;

    /**取消服务暴露*/
    private transient volatile boolean unexported;

    /**为true表示服务已经暴露*/
    private transient volatile boolean exported;


    /**是否泛化*/
    private volatile String generic;

    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }


    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (id == null || id.length() == 0) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? (String) null : interfaceClass.getName());
    }

    /**
     * 开始暴露服务
     * */
    public synchronized void export() {

        //表示Service的export属性为空 则设置为Provider的export属性
        if(provider!=null){
            if (export == null) {
                export = provider.getExport();
            }
        }

        //如果export为空或者为false则表示不暴露服务
        if (export != null && !export) {
            return;
        }

        // 延迟暴露
        if (delay != null && delay > 0) {
            delayExportExecutor.schedule(new Runnable() {
                public void run() {
                    doExport();
                }
            }, delay, TimeUnit.MILLISECONDS);
            // 立即暴露
        } else {
            doExport();
        }
    }

    /**
     *
     * */
    protected synchronized void doExport() {
        //如果已经取消暴露,则当前服务不能暴露了
        //todo 哪些情况下会取消服务暴露 以及取消暴露在框架执行的哪个过程
        if (unexported) {
            throw new IllegalStateException("Already unexported!");
        }

        if (exported) {
            return;
        }
        //既然开始暴露服务了,就需要将这个过程设置为true
        exported = true;

        //进行相应的接口校验
        if (interfaceName == null || interfaceName.length() == 0) {
            throw new IllegalStateException("<mini:service interface=\"\" /> interface not allow null!");
        }

        //TODO:拼接一些属性设置到ProviderConfig 根据properties文件


        if(provider!=null){

        }

        //泛化接口的实现
        if(ref instanceof GenericService){
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        }else{
            try {
                //加载当前类到加载器中
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            // 校验接口和方法
            checkInterfaceAndMethods(interfaceClass, methods);
            // 校验指向的 service 对象
            checkRef();
            generic = Boolean.FALSE.toString();
        }
    }

    private ProviderConfig provider;

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        this.provider = provider;
    }


    @Deprecated
    public void setProviders(List<ProviderConfig> providers) {
        this.protocols = convertProviderToProtocol(providers);
    }

    @Deprecated
    private static final List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());

        //根据provider获取协议相关属性
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }

        return protocols;
    }

    @Deprecated
    private static final ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
//        protocol.setName(provider.getProtocol().getName());
//        protocol.setServer(provider.getServer());
//        protocol.setClient(provider.getClient());
//        protocol.setCodec(provider.getCodec());
//        protocol.setHost(provider.getHost());
//        protocol.setPort(provider.getPort());
//        protocol.setPath(provider.getPath());
//        protocol.setPayload(provider.getPayload());
//        protocol.setThreads(provider.getThreads());
//        protocol.setParameters(provider.getParameters());
        return protocol;
    }
}
