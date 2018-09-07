package com.cfs.mini.remoting.transport.codec;

import com.cfs.mini.common.io.UnsafeByteArrayInputStream;
import com.cfs.mini.common.io.UnsafeByteArrayOutputStream;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.Codec;
import com.cfs.mini.remoting.Codec2;
import org.jboss.netty.buffer.ChannelBuffer;
import org.springframework.util.Assert;

import java.io.IOException;

public class CodecAdapter implements Codec2 {

    private Codec codec;

    public CodecAdapter(Codec codec) {
        Assert.notNull(codec, "codec == null");
        this.codec = codec;
    }

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message)
            throws IOException {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024);
        // 编码
        codec.encode(channel, os, message);
        // 写入 buffer
        buffer.writeBytes(os.toByteArray());
    }

    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        // 读取字节到数组
        byte[] bytes = new byte[buffer.readableBytes()];
        int savedReaderIndex = buffer.readerIndex();
        buffer.readBytes(bytes);
        // 解码
        UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(bytes);
        Object result = codec.decode(channel, is);
        // 设置最新的开始读取位置
        buffer.readerIndex(savedReaderIndex + is.position());
        // 返回是否要进一步读取
        return result == Codec.NEED_MORE_INPUT ? DecodeResult.NEED_MORE_INPUT : result;
    }

    public Codec getCodec() {
        return codec;
    }
}
