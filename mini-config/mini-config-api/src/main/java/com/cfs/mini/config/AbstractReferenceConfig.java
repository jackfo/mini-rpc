package com.cfs.mini.config;

import com.cfs.mini.config.support.Parameter;
import com.cfs.mini.rpc.core.support.ProtocolUtils;

public class AbstractReferenceConfig extends AbstractInterfaceConfig {


    //TODO:思考为什么需要在这里定义序列ID 是否需要做序列化
    private static final long serialVersionUID = -2786526984373031126L;

    protected String generic;

    @Parameter(excluded = true)
    public Boolean isGeneric() {
        return ProtocolUtils.isGeneric(generic);
    }

    public void setGeneric(Boolean generic) {
        if (generic != null) {
            this.generic = generic.toString();
        }
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        this.generic = generic;
    }

    /**该属性检验是否是本地直连*/
    protected Boolean injvm;

    public Boolean isInjvm() {
        return injvm;
    }

    public void setInjvm(Boolean injvm) {
        this.injvm = injvm;
    }
}
