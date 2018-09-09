package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FailbackRegistry extends AbstractRegistry{
    public FailbackRegistry(URL url) {


        super(url);

        //TODO:将重试机制添加到线程池
    }

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    @Override
    public void register(URL url) {

        if (destroyed.get()){
            return;
        }
        // 添加到 `registered` 变量
        super.register(url);

        doRegister(url);

    }

    protected abstract void doRegister(URL url);
}
