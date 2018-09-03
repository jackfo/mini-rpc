package com.cfs.mini.registry.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import com.cfs.mini.rpc.core.Invoker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderConsumerRegTable {


    public static ConcurrentHashMap<String, Set<ProviderInvokerWrapper>> providerInvokers = new ConcurrentHashMap<String, Set<ProviderInvokerWrapper>>();


    public static ConcurrentHashMap<String, Set<ConsumerInvokerWrapper>> consumerInvokers = new ConcurrentHashMap<String, Set<ConsumerInvokerWrapper>>();


    /**
     * 将其添加到对应的providerInvokers这个Map中去
     * */
    public static void registerProvider(Invoker invoker, URL registryUrl, URL providerUrl) {
        ProviderInvokerWrapper wrapperInvoker = new ProviderInvokerWrapper(invoker, registryUrl, providerUrl);

        String serviceUniqueName = providerUrl.getServiceKey();

        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);

        if (invokers == null) {
            providerInvokers.putIfAbsent(serviceUniqueName, new ConcurrentHashSet<ProviderInvokerWrapper>());
            invokers = providerInvokers.get(serviceUniqueName);
        }
        invokers.add(wrapperInvoker);
    }

    public static Set<ProviderInvokerWrapper> getProviderInvoker(String serviceUniqueName) {
        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return Collections.emptySet();
        }
        return invokers;
    }

    public static ProviderInvokerWrapper getProviderWrapper(Invoker invoker) {
        // 获得服务提供者 URL
        URL providerUrl = invoker.getUrl();
        if (Constants.REGISTRY_PROTOCOL.equals(providerUrl.getProtocol())) {
            providerUrl = URL.valueOf(providerUrl.getParameterAndDecoded(Constants.EXPORT_KEY));
        }
        // 获得指定的 Provider Invoker 集合
        String serviceUniqueName = providerUrl.getServiceKey();
        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return null;
        }
        // 获得 invoker 对应的 ProviderInvokerWrapper 对象
        for (ProviderInvokerWrapper providerWrapper : invokers) {
            Invoker providerInvoker = providerWrapper.getInvoker();
            if (providerInvoker == invoker) {
                return providerWrapper;
            }
        }
        return null;
    }

}
