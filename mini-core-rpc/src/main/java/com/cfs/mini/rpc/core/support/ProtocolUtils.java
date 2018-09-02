package com.cfs.mini.rpc.core.support;

import com.cfs.mini.common.Constants;

public class ProtocolUtils {

    private ProtocolUtils() {
    }

    /**
     * 检测是否是泛化的配置项
     * */
    public static boolean isGeneric(String generic) {
        return generic != null
                && !"".equals(generic)
                && (Constants.GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)  /* Normal generalization cal */
                || Constants.GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) /* Streaming generalization call supporting jdk serialization */
                || Constants.GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic));
    }
}
