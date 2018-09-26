package com.cfs.mini.remoting.transport.netty4;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Client;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Server;
import com.cfs.mini.remoting.transport.AbstractClient;
import com.cfs.mini.remoting.transport.AbstractServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NettyClient extends AbstractClient implements Client {

    /**相应的日志记录*/
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);


    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));

    private Bootstrap bootstrap;

    private volatile io.netty.channel.Channel channel;

    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }



    


}
