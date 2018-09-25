package com.cfs.mini.remoting;

import com.cfs.mini.common.Resetable;

import java.util.Collection;


public interface Server extends Endpoint, Resetable{

    Collection<Channel> getChannels();

}
