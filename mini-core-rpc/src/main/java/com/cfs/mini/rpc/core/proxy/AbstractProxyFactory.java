package com.cfs.mini.rpc.core.proxy;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.utils.ReflectUtils;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.ProxyFactory;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.service.EchoService;

public abstract class AbstractProxyFactory implements ProxyFactory {

    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        Class<?>[] interfaces = null;


        String config = invoker.getUrl().getParameter("interfaces");

        if (config != null && config.length() > 0) {
            String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
            if (types != null && types.length > 0) {
                interfaces = new Class<?>[types.length + 2];
                interfaces[0] = invoker.getInterface();
                interfaces[1] = EchoService.class;
                for (int i = 0; i < types.length; i++) {
                    interfaces[i + 1] = ReflectUtils.forName(types[i]);
                }
            }
        }

        if (interfaces == null) {
            interfaces = new Class<?>[]{invoker.getInterface(), EchoService.class};
        }
        return getProxy(invoker, interfaces);
    }

    public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);
}
