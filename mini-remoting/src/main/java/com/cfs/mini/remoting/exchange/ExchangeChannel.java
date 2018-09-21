package com.cfs.mini.remoting.exchange;

import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.RemotingException;

public interface ExchangeChannel extends Channel {

    ExchangeHandler getExchangeHandler();

    ResponseFuture request(Object request, int timeout) throws RemotingException;

    /**发送请求*/
    ResponseFuture request(Object request) throws RemotingException;


}
