package com.cfs.mini.config;

import com.cfs.mini.config.support.Parameter;

public class ProtocolConfig extends AbstractConfig{


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
}
