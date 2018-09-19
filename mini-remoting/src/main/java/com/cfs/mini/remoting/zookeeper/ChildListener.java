package com.cfs.mini.remoting.zookeeper;

import java.util.List;

public interface ChildListener {

    /**子节点发生变化是回溯*/
    void childChanged(String path, List<String> children);
}
