package com.cfs.mini.remoting.exchange;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.remoting.RemotingException;


public class Exchangers {

    static {
       //TODO:版本检查
    }

    private Exchangers() {
    }




    /**
     * 绑定相关的Server
     * */
    public static ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        if(url == null){
            throw new IllegalArgumentException("url == null");
        }
        if(handler == null){
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");

        return getExchanger(url).bind(url, handler);
    }


    /**
     * 根据URL获取指定类型的Exchanger实现类
     * */
    public static Exchanger getExchanger(URL url){
        String type = url.getParameter(Constants.EXCHANGER_KEY,Constants.DEFAULT_EXCHANGER);
        return getExchanger(type);
    }

    public static Exchanger getExchanger(String type){
        return ExtensionLoader.getExtensionLoader(Exchanger.class).getExtension(type);
    }

    public static ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
        return getExchanger(url).connect(url, handler);
    }
}
