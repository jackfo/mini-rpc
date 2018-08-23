package com.cfs.mini.common;



public interface Node {
    URL getUrl();

    /**
     * is available.
     *
     * 是否可用
     *
     * @return available.
     */
    boolean isAvailable();

    /**
     * 销毁
     *
     * destroy.
     */
    void destroy();
}
