package com.cfs.mini.remoting.transport;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Server;

import java.net.InetSocketAddress;
import java.util.Collection;

public abstract class AbstractServer extends AbstractEndpoint implements Server {

    protected static final String SERVER_THREAD_POOL_NAME = "MiniServerHandler";

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    /**
     * 服务地址
     */
    private InetSocketAddress localAddress;
    /**
     * 绑定地址
     */
    private InetSocketAddress bindAddress;

    private int accepts;

    private int idleTimeout;

    public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);

        localAddress = getUrl().toInetSocketAddress();

        /**获取绑定ip和端口*/
        String bindIp = getUrl().getParameter(Constants.BIND_IP_KEY, getUrl().getHost());
        int bindPort = getUrl().getParameter(Constants.BIND_PORT_KEY, getUrl().getPort());

        if (url.getParameter(Constants.ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
            bindIp = NetUtils.ANYHOST;
        }

        bindAddress = new InetSocketAddress(bindIp, bindPort);

        this.accepts = url.getParameter(Constants.ACCEPTS_KEY, Constants.DEFAULT_ACCEPTS);

        this.idleTimeout = url.getParameter(Constants.IDLE_TIMEOUT_KEY, Constants.DEFAULT_IDLE_TIMEOUT);

        /**开启服务器*/
        try {
            doOpen();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export " + getLocalAddress());
            }
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName()
                    + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
        }

    }

    protected abstract void doOpen() throws Throwable;

    protected abstract void doClose() throws Throwable;

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        // 获得所有的客户端的通道
        Collection<Channel> channels = getChannels();
        // 群发消息
        for (Channel channel : channels) {
            if (channel.isConnected()) {
                channel.send(message, sent);
            }
        }
    }
}
