package com.mini.rpc.core;

/**
 * Exporter. (API/SPI, Prototype, ThreadSafe)
 *
 * Exporter ，Invoker 暴露服务在 Protocol 上的对象。
 *
 */
public interface Exporter<T> {

    /**
     * get invoker.
     *
     * @return invoker
     */
    Invoker<T> getInvoker();

    /**
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();

}