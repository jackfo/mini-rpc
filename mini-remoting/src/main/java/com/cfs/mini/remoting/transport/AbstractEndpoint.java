package com.cfs.mini.remoting.transport;


import com.cfs.mini.common.Constants;
import com.cfs.mini.common.Resetable;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Codec;
import com.cfs.mini.remoting.Codec2;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.transport.codec.CodecAdapter;

import java.net.InetSocketAddress;

public abstract class AbstractEndpoint extends AbstractPeer implements Resetable {


    private static final Logger logger = LoggerFactory.getLogger(AbstractEndpoint.class);


    /**编解码器*/
    private Codec2 codec;

    /**超时时间*/
    private int timeout;

    /**连接超时时间*/
    private int connectTimeout;

    public AbstractEndpoint(URL url, ChannelHandler handler) {
        super(url, handler);
        this.codec = getChannelCodec(url);
        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
    }

    @Override
    public void reset(URL url) {

    }



    /**根据URL获取相应的编解码器*/
    protected static Codec2 getChannelCodec(URL url) {
        String codecName = url.getParameter(Constants.CODEC_KEY, "telnet");
        if (ExtensionLoader.getExtensionLoader(Codec2.class).hasExtension(codecName)) { // 例如，在 DubboProtocol 中，会获得 DubboCodec
            return ExtensionLoader.getExtensionLoader(Codec2.class).getExtension(codecName);
        } else {
            return new CodecAdapter(ExtensionLoader.getExtensionLoader(Codec.class).getExtension(codecName));
        }
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }



    protected Codec2 getCodec() {
        return codec;
    }

    protected int getTimeout() {
        return timeout;
    }

    protected int getConnectTimeout() {
        return connectTimeout;
    }
}

