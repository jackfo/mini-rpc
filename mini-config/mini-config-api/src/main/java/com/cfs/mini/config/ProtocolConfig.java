package com.cfs.mini.config;

import com.cfs.mini.config.support.Parameter;
import com.cfs.mini.registry.support.AbstractRegistryFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProtocolConfig extends AbstractConfig{

    /**是否已经销毁*/
    private static final AtomicBoolean destroyed = new AtomicBoolean(false);


    private String name;

    @Parameter(excluded = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    private Boolean register;

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
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


    private String host;

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        checkName("host", host);
        this.host = host;
    }

    private Integer port;

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * 销毁所有方法
     * */
    public static void destroyAll() {

        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        /**销毁相关注册中心*/
        AbstractRegistryFactory.destroyAll();


    }
}
