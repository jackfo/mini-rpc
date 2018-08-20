package com.cfs.mini.config;

import java.io.Serializable;

public abstract class AbstractConfig implements Serializable {

    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {

        System.out.println("****************** setId");

        this.id = id;
    }
}
