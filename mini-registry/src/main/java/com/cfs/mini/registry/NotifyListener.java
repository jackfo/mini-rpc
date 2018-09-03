package com.cfs.mini.registry;

import com.cfs.mini.common.URL;

import java.util.List;

public interface NotifyListener {

    void notify(List<URL> urls);
}
