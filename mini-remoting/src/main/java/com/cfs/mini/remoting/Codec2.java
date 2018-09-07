package com.cfs.mini.remoting;


import com.cfs.mini.common.Constants;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;
import org.jboss.netty.buffer.ChannelBuffer;

import java.io.IOException;

/**
 * 编解码器
 * */

@SPI
public interface Codec2 {

    /**
     * 编码
     *
     * @param channel 通道
     * @param buffer Buffer
     * @param message 消息
     * @throws IOException 当编码发生异常时
     */
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    /**
     * 解码
     *
     * @param channel 通道
     * @param buffer Buffer
     * @return 消息
     * @throws IOException 当解码发生异常时
     */
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;

    /**
     * 解码结果
     */
    enum DecodeResult {
        /**
         * 需要更多输入
         */
        NEED_MORE_INPUT,
        /**
         * 忽略一些输入
         */
        SKIP_SOME_INPUT
    }


}
