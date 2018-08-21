package com.cfs.mini.config;

import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConfigUtils;
import com.cfs.mini.common.utils.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractConfig implements Serializable {

    private static final int MAX_LENGTH = 200;

    private static final int MAX_PATH_LENGTH = 200;

    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");

    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private static final String[] SUFFIXES = new String[]{"Config", "Bean"};

    private static String getTagName(Class<?> cls) {
        String tag = cls.getSimpleName();
        for (String suffix : SUFFIXES) {
            if (tag.endsWith(suffix)) {
                tag = tag.substring(0, tag.length() - suffix.length());
                break;
            }
        }
        tag = tag.toLowerCase();
        return tag;
    }

    protected static void appendProperties(AbstractConfig config){
        if(config==null){
            return;
        }

        //根据配置文件获取相关属性 如ApplicationConfig 则prefix的值是mini.application
        String prefix ="mini."+getTagName(config.getClass())+".";

        /**获取所有的方法*/
        Method[] methods = config.getClass().getMethods();
        for(Method method:methods){
            try {
                String name = method.getName();
                //方法是setter方法 public修饰 参数长度为1 并且是原始类型
                if(name.length()>3&&name.startsWith("set")&&Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 1 && isPrimitive(method.getParameterTypes()[0])){

                    String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), ".");

                    String value = null;

                    //优先根据id获取相应的属性值
                    //TODO:疑问:在这里如果设置相应的系统属性优先级则会高于配置文件中的值
                    if (config.getId() != null && config.getId().length() > 0) {
                        String pn = prefix + config.getId() + "." + property; // 带有 `Config#id`
                        value = System.getProperty(pn);
                        if (!StringUtils.isBlank(value)) {
                            logger.info("Use System Property " + pn + " to config dubbo");
                        }
                    }

                    //如果id不存在再根据方法名来获取属性配置
                    if (value == null || value.length() == 0) {
                        String pn = prefix + property; // // 不带 `Config#id`
                        value = System.getProperty(pn);
                        if (!StringUtils.isBlank(value)) {
                            logger.info("Use System Property " + pn + " to config dubbo");
                        }
                    }

                    if (value == null || value.length() == 0) {
                        Method getter;
                        try {
                            getter = config.getClass().getMethod("get" + name.substring(3), new Class<?>[0]);
                        } catch (NoSuchMethodException e) {
                            try {
                                getter = config.getClass().getMethod("is" + name.substring(3), new Class<?>[0]);
                            } catch (NoSuchMethodException e2) {
                                getter = null;
                            }
                        }
                        if (getter != null) {
                            if (getter.invoke(config, new Object[0]) == null) { // 使用 getter 判断 XML 是否已经设置
                                // 【properties 配置】优先从带有 `Config#id` 的配置中获取，例如：`dubbo.application.demo-provider.name` 。
                                if (config.getId() != null && config.getId().length() > 0) {
                                    value = ConfigUtils.getProperty(prefix + config.getId() + "." + property);
                                }
                                // 【properties 配置】获取不到，其次不带 `Config#id` 的配置中获取，例如：`dubbo.application.name` 。
                                if (value == null || value.length() == 0) {
                                    value = ConfigUtils.getProperty(prefix + property);
                                }

                                //TODO: legacyProperties被干掉 看后期是否具有影响

                            }
                        }
                    }

                    if (value != null && value.length() > 0) {
                        method.invoke(config, new Object[]{convertPrimitive(method.getParameterTypes()[0], value)});
                    }
                }
            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
        }
    }


    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Object.class;
    }

    /**
     * 将字符串转换成目标的基本数据类型的值
     *
     * @param type 目标的基本数据类型
     * @param value 字符串
     * @return 值
     */
    private static Object convertPrimitive(Class<?> type, String value) {
        if (type == char.class || type == Character.class) {
            return value.length() > 0 ? value.charAt(0) : '\0';
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.valueOf(value);
        } else if (type == short.class || type == Short.class) {
            return Short.valueOf(value);
        } else if (type == int.class || type == Integer.class) {
            return Integer.valueOf(value);
        } else if (type == long.class || type == Long.class) {
            return Long.valueOf(value);
        } else if (type == float.class || type == Float.class) {
            return Float.valueOf(value);
        } else if (type == double.class || type == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }


    protected static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (value.length() > maxlength) {
            throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalStateException("Invalid " + property + "=\"" + value + "\" contain illegal charactor, only digit, letter, '-', '_' and '.' is legal.");
            }
        }
    }

    protected static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    public static void main(String[] args) {
        System.out.println(StringUtils.camelToSplitName("heLLo.s","."));
    }

}
