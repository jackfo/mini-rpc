package com.cfs.mini.config;

public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {


    protected Boolean export;

    protected Integer delay;

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean getExport() {
        return export;
    }

    public void setExport(Boolean export) {
        this.export = export;
    }

}
