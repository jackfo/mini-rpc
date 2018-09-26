package com.cfs.mini.remoting;

import com.cfs.mini.common.Resetable;

import java.net.InetSocketAddress;
import java.util.Collection;


public interface Server extends Endpoint, Resetable{

    /**获取所有的通道*/
    Collection<Channel> getChannels();

    /**根据远程地址获取相应的通道*/
    Channel getChannel(InetSocketAddress remoteAddress);

    /**判断是否本地绑定*/
    boolean isBound();

}
