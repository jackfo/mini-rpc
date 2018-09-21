package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.Parameters;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Client;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.ExchangeChannel;
import com.cfs.mini.remoting.exchange.ExchangeClient;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;

public class HeaderExchangeClient implements ExchangeClient {


    private final Client client;

    /**信息交换通道*/
    private final ExchangeChannel channel;

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeClient.class);

    public HeaderExchangeClient(Client client,boolean needHeartbeat){

        if(client==null){
            throw new IllegalArgumentException("client == null");
        }

        this.client = client;
        // 创建 HeaderExchangeChannel 对象
        this.channel = new HeaderExchangeChannel(client);


        //String mini = client.getUrl().getParameter(Constants.MINI_VERSION_KEY);

        //TODO:心跳机制
    }


    @Override
    public ExchangeHandler getExchangeHandler() {
        return null;
    }

    @Override
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return null;
    }

    @Override
    public ResponseFuture request(Object request) throws RemotingException {
        return channel.request(request);
    }

    @Override
    public URL getUrl() {
        return channel.getUrl();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void reconnect() throws RemotingException {

    }

    @Override
    public void reset(Parameters parameters) {

    }

    @Override
    public void reset(URL url) {

    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean hasAttribute(String key) {
        return false;
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public void removeAttribute(String key) {

    }
}
