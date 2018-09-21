package com.cfs.mini.registry;

import com.cfs.mini.common.URL;

public interface RegistryService {

    /**注册*/
    void register(URL url);

    /**取消注册*/
    void unregister(URL url);

    /**订阅*/
    void subscribe(URL url, NotifyListener listener);

    /**取消订阅*/
    void unsubscribe(URL url, NotifyListener listener);
}
