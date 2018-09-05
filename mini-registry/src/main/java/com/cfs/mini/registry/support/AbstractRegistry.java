package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.registry.NotifyListener;
import com.cfs.mini.registry.Registry;

import java.io.File;

public class AbstractRegistry implements Registry{

    public AbstractRegistry(URL url) {
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void register(URL url) {

    }

    @Override
    public void unregister(URL url) {

    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {

    }
}
