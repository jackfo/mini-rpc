package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import com.cfs.mini.common.utils.StringUtils;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Transporter;
import com.cfs.mini.remoting.exchange.*;
import com.cfs.mini.remoting.exchange.suport.ExchangeHandlerAdapter;
import com.cfs.mini.rpc.core.*;
import com.cfs.mini.rpc.core.protocol.AbstractProtocol;
import com.cfs.mini.rpc.core.support.ProtocolUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MiniProtocol extends AbstractProtocol {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_PORT = 9748;

    /**暴露的MAP映射*/
    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    protected static String serviceKey(URL url) {
        return ProtocolUtils.serviceKey(url);
    }

    protected static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        return ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup);
    }

    /**通信服务器的集合*/
    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>();

    /**key服务器地址加端口号*/
    private final Map<String, ReferenceCountExchangeClient> referenceClientMap = new ConcurrentHashMap<String, ReferenceCountExchangeClient>();

    /**
     * Invoker 集合
     */
    //TODO SOFEREFENCE
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<Invoker<?>>();

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    /**
     * 根据相应的ServiceKey将invoker封装进去添加到Map
     *
     * 在export过程中会暴bind服务,这样外部就可以进行访问了
     *
     * */
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();

        String key = serviceKey(url);

        MiniExporter miniExporter = new MiniExporter(invoker,key,exporterMap);

        exporterMap.put(key,miniExporter);

        openServer(url);

        return miniExporter;
    }

    /**
     * 启动通信服务器
     * */
    private void openServer(URL url) {
        String key = url.getAddress();
        //查看当前URL是否是一个服务
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);

        if(isServer){
            ExchangeServer server = serverMap.get(key);
            if (server == null) {
                serverMap.put(key, createServer(url));
            } else {
                server.reset(url);
            }
        }
    }


    private ExchangeServer createServer(URL url) {

        ExchangeServer server;

        try {
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        return server;

    }


    /**
     * 在这边获取真实的Invoker对象
     * */
    @Override
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {

        MiniInvoker<T> invoker = new MiniInvoker<T>(serviceType, url, getClients(url), invokers);
        // 添加到 `invokers`
        invokers.add(invoker);
        return invoker;
    }



    /**
     * 获得连接服务提供者的远程通信客户端数组
     *
     * @param url 服务提供者 URL
     * @return 远程通信客户端
     */
    private ExchangeClient[] getClients(URL url) {
        // 是否共享连接
        // whether to share connection
        boolean service_share_connect = false;
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 0);
        // if not configured, connection is shared, otherwise, one connection for one service
        if (connections == 0) { // 未配置时，默认共享
            service_share_connect = true;
            connections = 1;
        }

        // 创建连接服务提供者的 ExchangeClient 对象数组
        ExchangeClient[] clients = new ExchangeClient[connections];
        for (int i = 0; i < clients.length; i++) {
            if (service_share_connect) { // 共享
                clients[i] = getSharedClient(url);
            } else { // 不共享
                clients[i] = initClient(url);
            }
        }
        return clients;
    }

    /**创建一个新的连接*/
    private ExchangeClient initClient(URL url) {
        String str = url.getParameter(Constants.CLIENT_KEY, url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));
        /**校验是否存在相应的扩展机制,不存在则扔出异常*/
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                    " supported client type is " + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }

        /**设置编解码器*/
        //url = url.addParameter(Constants.CODEC_KEY, MiniCodec.NAME);

        //TODO:开启心跳机制
        ExchangeClient client;
        try {
                client = Exchangers.connect(url, requestHandler);

        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
        return client;
    }

    private ExchangeClient getSharedClient(URL url) {
        // 从集合中，查找 ReferenceCountExchangeClient 对象
        String key = url.getAddress();
        ReferenceCountExchangeClient client = referenceClientMap.get(key);
        if (client != null) {
            // 若未关闭，增加指向该 Client 的数量，并返回它
            if (!client.isClosed()) {
                client.incrementAndGetCount();
                return client;
                // 若已关闭，移除
            } else {
                referenceClientMap.remove(key);
            }
        }
        // 同步，创建 ExchangeClient 对象。
        synchronized (key.intern()) {
            // 创建 ExchangeClient 对象
            ExchangeClient exchangeClient = initClient(url);
            // 将 `exchangeClient` 包装，创建 ReferenceCountExchangeClient 对象
            client = new ReferenceCountExchangeClient(exchangeClient);
            // 添加到集合
            referenceClientMap.put(key, client);

            return client;
        }

    }

    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        boolean isCallBackServiceInvoke;
        boolean isStubServiceInvoke;
        int port = channel.getLocalAddress().getPort();
        String path = inv.getAttachments().get(Constants.PATH_KEY);
        isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(Constants.STUB_EVENT_KEY));
        if (isStubServiceInvoke) {
            port = channel.getRemoteAddress().getPort();
        }
        // 获得服务建
        String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));
        // 获得 Exporter 对象
        MiniExporter<?> exporter = (MiniExporter<?>) exporterMap.get(serviceKey);
        // 获得 Invoker 对象
        if (exporter == null) {
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
        }
        return exporter.getInvoker();
    }

    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {
        @Override
        public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
             if(message instanceof Invocation){
                 Invocation inv = (Invocation) message;
                 //获取其相应的调用者
                 Invoker<?> invoker = getInvoker(channel, inv);
                 // 设置调用方的地址

                 // 执行调用
                 return invoker.invoke(inv);
             }

             throw new RpcException("调用reply,进行远程操控出现异常");
        }

        @Override
        public void connected(Channel channel) {
            this.invoke(channel, Constants.ON_CONNECT_KEY);
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                this.reply((ExchangeChannel) channel, message);
            } else {
                super.received(channel, message);
            }
        }

        private void invoke(Channel channel, String methodKey) {
            // 创建 Invocation 对象
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            // 调用 received 方法，执行对应的方法
            if (invocation != null) {
                try {
                    this.received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }


        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
                invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }

    };





}
