package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;

public class FailbackRegistry extends AbstractRegistry{
    public FailbackRegistry(URL url) {


        super(url);

        //TODO:将重试机制添加到线程池
    }

    @Override
    public void register(URL url) {


    }


}
