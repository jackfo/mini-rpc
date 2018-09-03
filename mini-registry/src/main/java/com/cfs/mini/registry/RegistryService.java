package com.cfs.mini.registry;

import com.cfs.mini.common.URL;

public interface RegistryService {

    void register(URL url);

    void unregister(URL url);

    void subscribe(URL url, NotifyListener listener);
}
