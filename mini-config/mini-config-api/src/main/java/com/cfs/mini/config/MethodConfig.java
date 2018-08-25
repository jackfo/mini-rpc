package com.cfs.mini.config;

import java.util.List;

public class MethodConfig extends AbstractMethodConfig{

    private String name;


    private List<ArgumentConfig> arguments;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ArgumentConfig> getArguments() {
        return arguments;
    }

    @SuppressWarnings("unchecked")
    public void setArguments(List<? extends ArgumentConfig> arguments) {
        this.arguments = (List<ArgumentConfig>) arguments;
    }


}
