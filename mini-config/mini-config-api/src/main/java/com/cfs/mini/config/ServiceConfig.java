package com.cfs.mini.config;

import java.util.ArrayList;
import java.util.List;

public class ServiceConfig<T> extends AbstractServiceConfig{


    private String interfaceName;
    /**
     * {@link #interfaceName} 对应的接口类
     *
     * 非配置
     */
    private Class<?> interfaceClass;


    private T ref;


    //注册中心的配置数组
    protected List<ProtocolConfig> protocols;

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
