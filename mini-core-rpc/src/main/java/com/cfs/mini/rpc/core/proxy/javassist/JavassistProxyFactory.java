package com.cfs.mini.rpc.core.proxy.javassist;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.bytecode.Proxy;
import com.cfs.mini.common.bytecode.Wrapper;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.proxy.AbstractProxyInvoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.proxy.AbstractProxyFactory;
import com.cfs.mini.rpc.core.proxy.InvokerInvocationHandler;

public class JavassistProxyFactory extends AbstractProxyFactory {


    @Override
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {

        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);

        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }
}
