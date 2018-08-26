package com.cfs.mini.config;

import com.cfs.mini.config.support.Parameter;

public class ProviderConfig extends AbstractServiceConfig{


    private Boolean isDefault;

    private String host;

    private Integer port;


    //排出代表在URL进行相应的解析的时候,不对这个属性进行相应的转化,防止其暴露在URL上面
    @Parameter(excluded = true)
    public Boolean isDefault() {
        return isDefault;
    }

    @Deprecated
    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    private String contextpath;

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        checkPathName("contextpath", contextpath);
        this.contextpath = contextpath;
    }


    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    @Deprecated
    public void setPort(Integer port) {
        this.port = port;
    }


    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
