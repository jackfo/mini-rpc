package com.cfs.mini.rpc.core.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.rpc.core.Invocation;

public class RpcUtils {


    /**
     * 获得方法名
     *
     * @param invocation Invocation 对象
     * @return 方法名
     */
    public static String getMethodName(Invocation invocation) {
        // 泛化调用，第一个参数为方法名
        if (Constants.$INVOKE.equals(invocation.getMethodName())
                && invocation.getArguments() != null
                && invocation.getArguments().length > 0
                && invocation.getArguments()[0] instanceof String) {
            return (String) invocation.getArguments()[0];
        }
        // 普通调用，直接获得
        return invocation.getMethodName();
    }

    public static Object[] getArguments(Invocation invocation) {
        if (Constants.$INVOKE.equals(invocation.getMethodName())
                && invocation.getArguments() != null
                && invocation.getArguments().length > 2
                && invocation.getArguments()[2] instanceof Object[]) {
            return (Object[]) invocation.getArguments()[2];
        }
        return invocation.getArguments();
    }


}
