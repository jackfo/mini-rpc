package com.cfs.mini.config.support;

import java.lang.annotation.*;
import java.util.Map;

/**
 * Parameter 参数
 * 用于 Dubbo {@link com.cfs.mini.common.URL} 的参数拼接
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {

    /**
     * 键（别名）
     */
    String key() default "";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 是否忽略
     */
    boolean excluded() default false;

    /**
     * 是否转义
     */
    boolean escaped() default false;

    /**
     * 是否为属性
     *
     * 目前用于《事件通知》http://dubbo.io/books/dubbo-user-book/demos/events-notify.html
     */
    boolean attribute() default false;

    /**
     * 是否拼接默认属性，参见 {@link com.alibaba.dubbo.config.AbstractConfig#appendParameters(Map, Object, String)} 方法。
     *
     * 我们来看看 `#append() = true` 的属性，有如下四个：
     *   + {@link AbstractInterfaceConfig#getFilter()}
     *   + {@link AbstractInterfaceConfig#getListener()}
     *   + {@link AbstractReferenceConfig#getFilter()}
     *   + {@link AbstractReferenceConfig#getListener()}
     *   + {@link AbstractServiceConfig#getFilter()}
     *   + {@link AbstractServiceConfig#getListener()}
     * 那么，以 AbstractServiceConfig 举例子。
     *
     * 我们知道 ProviderConfig 和 ServiceConfig 继承 AbstractServiceConfig 类，那么 `filter` , `listener` 对应的相同的键。
     * 下面我们以 `filter` 举例子。
     *
     * 在 ServiceConfig 中，默认会<b>继承</b> ProviderConfig 配置的 `filter` 和 `listener` 。
     * 所以这个属性，就是用于，像 ServiceConfig 的这种情况，从 ProviderConfig 读取父属性。
     *
     * 举个例子，如果 `ProviderConfig.filter=aaaFilter` ，`ServiceConfig.filter=bbbFilter` ，最终暴露到 Dubbo URL 时，参数为 `service.filter=aaaFilter,bbbFilter` 。
     */
    boolean append() default false;

}