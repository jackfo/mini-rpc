package com.cfs.mini.remoting.transport.netty;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.Codec2;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.*;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * netty编解码适配器
 * */
final class NettyCodecAdapter {

    private final Codec2 codec;
    /**
     * Dubbo URL
     */
    private final URL url;
    /**
     * 网络读写缓冲区大小
     */
    private final int bufferSize;


    private final com.cfs.mini.remoting.ChannelHandler handler;

    /**
     * 注入对应的URL和编解码器
     * */
    public NettyCodecAdapter(Codec2 codec, URL url, com.cfs.mini.remoting.ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        // 设置 `bufferSize`
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    @Sharable
    private class InternalEncoder extends OneToOneEncoder {

        @Override
        protected Object encode(ChannelHandlerContext ctx, Channel ch, Object msg) throws Exception {
          throw new RuntimeException("编码异常");
        }

    }


}
