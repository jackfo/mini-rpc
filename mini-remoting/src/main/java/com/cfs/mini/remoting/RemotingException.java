package com.cfs.mini.remoting;

import java.net.InetSocketAddress;

public class RemotingException extends Exception{

    private static final long serialVersionUID = -3160452149606778709L;

    /**
     * 本地地址
     */
    private InetSocketAddress localAddress;
    /**
     * 远程地址
     */
    private InetSocketAddress remoteAddress;

    public RemotingException(Channel channel, String msg) {
        this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(),
                msg);
    }

    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(message);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public RemotingException(Channel channel, Throwable cause) {
        this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(),
                cause);
    }

    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Throwable cause) {
        super(cause);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public RemotingException(Channel channel, String message, Throwable cause) {
        this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(),
                message, cause);
    }

    public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message,
                             Throwable cause) {
        super(message, cause);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
