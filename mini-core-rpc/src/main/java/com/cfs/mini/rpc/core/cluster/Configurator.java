package com.cfs.mini.rpc.core.cluster;

import com.cfs.mini.common.URL;

public interface Configurator extends Comparable<Configurator>  {

    URL getUrl();

    URL configure(URL url);
}
