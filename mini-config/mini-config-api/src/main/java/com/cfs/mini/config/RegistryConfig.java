package com.cfs.mini.config;

import com.cfs.mini.config.support.Parameter;

public class RegistryConfig extends AbstractConfig{


    public static final String NO_AVAILABLE = "N/A";


    private String address;

    @Parameter(excluded = true) // 排除
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
