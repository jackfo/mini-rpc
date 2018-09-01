package com.cfs.mini.config;

import java.util.List;

public class ApplicationConfig extends AbstractConfig{

    // is default or not
    private Boolean isDefault;
    public Boolean isDefault() {
        return isDefault;
    }
    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    protected List<RegistryConfig> registries;

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }
}
