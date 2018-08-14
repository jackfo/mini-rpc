package com.cfs.mini.common;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;

public class Version {
    private static final String DEFAULT_DUBBO_VERSION = "2.0.0";
    private static final Logger logger = LoggerFactory.getLogger(Version.class);
    //private static final String VERSION = getVersion(Version.class, DEFAULT_DUBBO_VERSION);

    static {
        // check if there's duplicated jar
        //Version.checkDuplicate(Version.class);
    }

    private Version() {
    }

    public static String getVersion() {
        return DEFAULT_DUBBO_VERSION;
    }
}
