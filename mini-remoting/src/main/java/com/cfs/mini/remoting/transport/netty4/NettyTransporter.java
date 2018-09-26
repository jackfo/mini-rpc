package com.cfs.mini.remoting.transport.netty4;

import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.*;

public class NettyTransporter implements Transporter {

    public static final String NAME = "netty4";

    public Server bind(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyServer(url, listener);
    }

    public Client connect(URL url, ChannelHandler listener) throws RemotingException {
        return new NettyClient(url, listener);
    }
}
