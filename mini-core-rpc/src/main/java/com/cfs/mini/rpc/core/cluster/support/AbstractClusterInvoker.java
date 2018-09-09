package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.cluster.Directory;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractClusterInvoker<T> implements Invoker<T> {


    protected final Directory<T> directory;

    protected final boolean availablecheck;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractClusterInvoker(Directory<T> directory) {
        this(directory, directory.getUrl());
    }

    public AbstractClusterInvoker(Directory<T> directory, URL url) {
        // 初始化 directory
        if (directory == null) {
            throw new IllegalArgumentException("service directory == null");
        }
        this.directory = directory;
        // sticky: invoker.isAvailable() should always be checked before using when availablecheck is true.
        // 初始化 availablecheck
        this.availablecheck = url.getParameter(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Constants.DEFAULT_CLUSTER_AVAILABLE_CHECK);
    }
}
