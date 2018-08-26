package com.mini.rpc.core.cluster;

import com.cfs.mini.common.URL;

public interface Configurator {

    URL getUrl();

    URL configure(URL url);
}
