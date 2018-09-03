package com.cfs.mini.common.logger;

public interface Logger {

    /**
     * Logs a message with trace log level.
     *
     * @param msg log this message
     */
    void trace(String msg);

    /**
     * Logs an error with trace log level.
     *
     * @param e log this cause
     */
    void trace(Throwable e);

    /**
     * Logs an error with trace log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void trace(String msg, Throwable e);

    /**
     * Logs a message with debug log level.
     *
     * @param msg log this message
     */
    void debug(String msg);

    /**
     * Logs an error with debug log level.
     *
     * @param e log this cause
     */
    void debug(Throwable e);

    /**
     * Logs an error with debug log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void debug(String msg, Throwable e);

    /**
     * Logs a message with info log level.
     *
     * @param msg log this message
     */
    void info(String msg);


    void info(String msg,String className);

    /**
     * Logs an error with info log level.
     *
     * @param e log this cause
     */
    void info(Throwable e);

    /**
     * Logs an error with info log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void info(String msg, Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     */
    void warn(String msg);

    /**
     * Logs a message with warn log level.
     *
     * @param e log this message
     */
    void warn(Throwable e);

    /**
     * Logs a message with warn log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void warn(String msg, Throwable e);

    /**
     * Logs a message with error log level.
     *
     * @param msg log this message
     */
    void error(String msg);

    /**
     * Logs an error with error log level.
     *
     * @param e log this cause
     */
    void error(Throwable e);

    /**
     * Logs an error with error log level.
     *
     * @param msg log this message
     * @param e   log this cause
     */
    void error(String msg, Throwable e);

    void error(String msg,String module);

    /**
     * Is trace logging currently enabled?
     *
     * @return true if trace is enabled
     */
    boolean isTraceEnabled();

    /**
     * Is debug logging currently enabled?
     *
     * @return true if debug is enabled
     */
    boolean isDebugEnabled();

    /**
     * Is info logging currently enabled?
     *
     * @return true if info is enabled
     */
    boolean isInfoEnabled();

    /**
     * Is warn logging currently enabled?
     *
     * @return true if warn is enabled
     */
    boolean isWarnEnabled();

    /**
     * Is error logging currently enabled?
     *
     * @return true if error is enabled
     */
    boolean isErrorEnabled();
}

