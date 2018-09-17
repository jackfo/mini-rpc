package com.cfs.mini.config;

public class ConsumerConfig extends AbstractReferenceConfig{

    private static final long serialVersionUID = 2827274711143680600L;

    private Boolean isDefault;


    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
}
