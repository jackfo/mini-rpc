package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;
import com.cfs.mini.rpc.core.cluster.LoadBalance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FailoverClusterInvoker<T> extends  AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);


    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }


    @Override
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {


        List<Invoker<T>> copyinvokers = invokers;

        checkInvokers(copyinvokers,invocation);

        int len = getUrl().getMethodParameter(invocation.getMethodName(),Constants.RETRIES_KEY,Constants.DEFAULT_RETRIES);

        if(len==0){
            len = 1;
        }

        /**
         * 记录最后一次出现异常的情况
         * */
        RpcException re = null;

        List<Invoker<T>> invoked = new ArrayList<>(copyinvokers.size());

        Set<String> providers = new HashSet<String>(len);

        for(int i=0;i<len;i++){
            if(i>0){
                checkWhetherDestroyed();
                copyinvokers = list(invocation);
                checkInvokers(copyinvokers,invocation);
            }

            Invoker<T> invoker = select(loadbalance,invocation,copyinvokers,invoked);

            invoked.add(invoker);

            //TODO:添加到RPC上下文
            try{
                Result result = invoker.invoke(invocation);
                // 重试过程中，将最后一次调用的异常信息以 warn 级别日志输出
                if (re != null && logger.isWarnEnabled()) {
                    logger.warn("Although retry the method " + invocation.getMethodName()
                            + " in the service " + getInterface().getName()
                            + " was successful by the provider " + invoker.getUrl().getAddress()
                            + ", but there have been failed providers " + providers
                            + " (" + providers.size() + "/" + copyinvokers.size()
                            + ") from the registry " + directory.getUrl().getAddress()
                            + " on the consumer " + NetUtils.getLocalHost()
                            + " using the dubbo version " + Version.getVersion() + ". Last error is: "
                            + re.getMessage(), re);
                }
                return result;
            }catch (RpcException e){
                re = e;
            } catch (Throwable e) {
                re = new RpcException(e.getMessage(), e);
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }


        throw new RpcException("FailoverClusterInvoker调用失败");
    }
}
