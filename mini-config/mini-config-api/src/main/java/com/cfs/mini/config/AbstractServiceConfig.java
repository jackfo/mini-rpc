package com.cfs.mini.config;

public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {


    protected Integer delay;

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }
}
