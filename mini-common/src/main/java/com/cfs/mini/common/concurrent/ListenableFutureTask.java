package com.cfs.mini.common.concurrent;



import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ListenableFutureTask<V> extends FutureTask<V> implements ListenableFuture<V> {

    private final ExecutionList executionList = new ExecutionList();


    public ListenableFutureTask(Callable<V> callable) {
        super(callable);
    }

    ListenableFutureTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    public static <V> ListenableFutureTask<V> create(Callable<V> callable) {
        return new ListenableFutureTask<V>(callable);
    }
}
