
package com.cfs.mini.monitor;


import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;

/**
 * MonitorFactory. (SPI, Singleton, ThreadSafe)
 */
@SPI("mini")
public interface MonitorFactory {

    /**
     * Create monitor.
     *
     * @param url
     * @return monitor
     */
    @Adaptive("protocol")
    Monitor getMonitor(URL url);

}