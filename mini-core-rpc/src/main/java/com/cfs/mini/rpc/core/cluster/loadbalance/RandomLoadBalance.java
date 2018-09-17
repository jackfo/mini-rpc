package com.cfs.mini.rpc.core.cluster.loadbalance;

import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance{

    public static final String NAME = "random";

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size(); // Number of invokers
        int totalWeight = 0; // The sum of weights
        boolean sameWeight = true; // Every invoker has the same weight?
        // 计算总权限
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation); // 获得权重
            totalWeight += weight; // Sum
            if (sameWeight && i > 0 && weight != getWeight(invokers.get(i - 1), invocation)) {
                sameWeight = false;
            }
        }
        // 权重不相等，随机后，判断在哪个 Invoker 的权重区间中
        if (totalWeight > 0 && !sameWeight) {
            // 随机
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = random.nextInt(totalWeight);
            // Return a invoker based on the random value.
            // 区间判断
            for (Invoker<T> invoker : invokers) {
                offset -= getWeight(invoker, invocation);
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        // 权重相等，平均随机
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return invokers.get(random.nextInt(length));
    }
}
