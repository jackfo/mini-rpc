package com.cfs.mini.rpc.core.cluster.loadbalance;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.cluster.LoadBalance;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {


    /**
     * 计算权重
     * */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        // 计算权重
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        // 权重范围为 [0, weight] 之间
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if(invokers==null||invokers.isEmpty()){
            return null;
        }
        if(invokers.size()==1){
            return invokers.get(0);
        }

        return doSelect(invokers,url,invocation);

    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 获得 weight 配置，即服务权重。默认为 100
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0) {
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                // 获得启动总时长
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                // 获得预热需要总时长。默认为 10 * 60 * 1000 = 10 分钟
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                // 处于预热中，计算当前的权重
                if (uptime > 0 && uptime < warmup) {
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }
}
