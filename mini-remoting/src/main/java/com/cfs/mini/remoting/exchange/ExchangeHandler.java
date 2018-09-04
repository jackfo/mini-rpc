package com.cfs.mini.remoting.exchange;

import com.cfs.mini.remoting.RemotingException;

public interface ExchangeHandler {

    /**恢复请求结果*/
    Object reply(ExchangeChannel channel, Object request) throws RemotingException;
}
