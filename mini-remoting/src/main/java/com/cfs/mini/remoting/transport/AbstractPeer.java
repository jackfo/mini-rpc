package com.cfs.mini.remoting.transport;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Endpoint;
import com.cfs.mini.remoting.RemotingException;

public abstract class AbstractPeer implements Endpoint, ChannelHandler {

    private ChannelHandler handler;

    private volatile URL url;

    /**正在关闭*/
    private volatile boolean closing;

    /**已经关闭*/
    private volatile boolean closed;

    public AbstractPeer(URL url,ChannelHandler channelHandler){
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void send(Object message) throws RemotingException {
        send(message, url.getParameter(Constants.SENT_KEY, false));
    }


    /**
     * 检测是否关闭
     * */
    @Override
    public void startClose() {
        if (isClosed()) {
            return;
        }
        closing = true;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }


    @Override
    public ChannelHandler getChannelHandler() {
        if (handler instanceof ChannelHandlerDelegate) { // 获得真实的 ChannelHandler
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    /**
     * @return ChannelHandler
     */
    @Deprecated
    public ChannelHandler getHandler() {
        return getDelegateHandler();
    }

    /**
     * Return the final handler (which may have been wrapped). This method should be distinguished with getChannelHandler() method
     *
     * @return ChannelHandler
     */
    public ChannelHandler getDelegateHandler() {
        return handler;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return closing && !closed;
    }

    @Override
    public void connected(Channel ch) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(ch);
    }

    @Override
    public void disconnected(Channel ch) throws RemotingException {
        handler.disconnected(ch);
    }

    @Override
    public void sent(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(ch, msg);
    }

    @Override
    public void received(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(ch, msg);
    }

    @Override
    public void caught(Channel ch, Throwable ex) throws RemotingException {
        handler.caught(ch, ex);
    }
}
