package com.cfs.mini.remoting.exchange;

import com.cfs.mini.remoting.RemotingException;

/**
 * 响应Future
 * */
public interface ResponseFuture {
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.
     * 获得值
     *
     * @param timeoutInMillis timeout. 超时时长
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;


    /**
     * check is done.
     * 是否完成
     *
     * @return done or not.
     */
    boolean isDone();
}
