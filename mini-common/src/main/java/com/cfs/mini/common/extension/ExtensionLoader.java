package com.cfs.mini.common.extension;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ConcurrentHashSet;
import com.cfs.mini.common.utils.Holder;
import com.cfs.mini.common.utils.StringUtils;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String MINI_DIRECTORY = "META-INF/mini/";

    private static final String MINI_INTERNAL_DIRECTORY = MINI_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");


    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();


    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**缓存自适应的类*/
    private volatile Class<?> cachedAdaptiveClass = null;

    /**缓存默认的扩展名*/
    private String cachedDefaultName;

    /**标记在创建过程中是否出现异常*/
    private volatile Throwable createAdaptiveInstanceError;

    /**扩展接口类型*/
    private final Class<?> type;

    /**对象工厂,用来加载具体的实例*/
    private final ExtensionFactory objectFactory;

    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<String, Activate>();

    private Set<Class<?>> cachedWrapperClasses;

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap();


    /**
     * 缓存的拓展对象集合
     *
     * key：拓展名
     * value：拓展对象
     *
     * 例如，Protocol 拓展
     *          key：dubbo value：DubboProtocol
     *          key：injvm value：InjvmProtocol
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }


    /**
     * T表示要进行扩展的类,首先将其保存到扩展节点
     * */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if(type==null){
            throw new IllegalArgumentException("Extension type == null");
        }

        //类型必须是一个接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        //从对应的Map根据类型获取,检测是否具有扩展加载点
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);

        //如果没有则新建一个扩展类加载,之后加载到Map映射中
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }



    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        //如果name为true,查找默认当前加载器默认的实例
        if ("true".equals(name)) {
            return getDefaultExtension();
        }

        //从缓存中获得对应的拓展对象
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }

        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                // 从 缓存中 未获取到，进行创建缓存对象。
                if (instance == null) {
                    instance = createExtension(name);
                    // 设置创建对象到缓存中
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * Return default extension, return <code>null</code> if it's not configured.
     */
    /**
     * 返回缺省的扩展，如果没有设置则返回<code>null</code>。
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) { // 如果为 true ，不能继续调用 `#getExtension(true)` 方法，会形成死循环。
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    /**
     * 创建拓展名的拓展对象，并缓存。
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获得拓展名对应的拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name); // 抛出异常
        }
        try {
            // 从缓存中，获得拓展对象。
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 当缓存不存在时，创建拓展对象，并添加到缓存中。
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            // 注入依赖的属性
            injectExtension(instance);
            //这段代码的作用,注入当前实例所有封装类
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        // 生成不存在该拓展类实现的异常。
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }
            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }



    /**检测是否存在对应的类*/
    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        try {
            return getExtensionClass(name) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (name == null)
            throw new IllegalArgumentException("Extension name == null");
        // 获得拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null)
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        return clazz;
    }


    /**
     * 从缓存中获取扩展对象
     * */
    public T getAdaptiveExtension(){
        //每一个ExtensionLoder维持者一个对应的实例
        Object instance = cachedAdaptiveInstance.get();
        if(instance==null){
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建自适应拓展对象
                            instance = createAdaptiveExtension();
                            // 设置到缓存
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            // 记录异常
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
            }
        }
        return (T) instance;
    }


    /**
     * 获取相应的适配类
     * 适配类存在两种:
     *      第一种是在实现类上加@Adaptive注解的类
     *      第二种是在部分方法上添加@Adaptive的注解类,这种类没有实现注解的方法会在内部扔出异常
     * */
    private Class<?> getAdaptiveExtensionClass() {
        //在这上面是实现类存在Adaptive注解 则直接注入
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        //如果实现类不存在那么针对于接口所有方法进行上Adaptive注解进行创建,没有Adaptive则直接在方法内部扔出提倡
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * TODO:动态编译的事情
     * */
    private Class<?> createAdaptiveExtensionClass() {
        // 自动生成自适应拓展的代码实现的字符串
        String code = createAdaptiveExtensionClassCode();
        ClassLoader classLoader = findClassLoader();
        com.cfs.mini.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.cfs.mini.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }



    private String createAdaptiveExtensionClassCode() {


        StringBuilder codeBuidler = new StringBuilder();

        //获取当前接口所有的方法
        Method[] methods = type.getMethods();


        /**
         * 当不存在对应的@Adaptive注解类,则需要检测接口中是否某个方法具有注解
         * 如果没有一个方法存在,就扔出异常
         * 如果有方法存在,就会动态实现类,有Adaptive实现相应过程,没有的扔出异常
         * */
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }
        if (!hasAdaptiveAnnotation)
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");

        /**生产相应的字节码*/
        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements " + type.getCanonicalName() + " {");


        for(Method method : methods){
            //获取相应的返回类型
            Class<?> rt = method.getReturnType();
            Class<?>[] pts = method.getParameterTypes();
            Class<?>[] ets = method.getExceptionTypes();

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            /**
             * @param code 代表方法体里面的内容
             * 如果方法不存在Adaptive注解即adaptiveAnnotation为空那么直接在方法体里面扔出日常
             * 如果方法体存在Adaptive注解
             * */
            StringBuilder code = new StringBuilder(512);
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            }else{
                //找到参数类型为URL的游标索引
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }

                // 有类型为URL的参数，生成代码：生成校验 URL 非空的代码
                if (urlTypeIndex != -1) {
                    //检验URL是否为空
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");", urlTypeIndex);
                    code.append(s);
                    //赋值操作
                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }else{
                    //在没有发现参数为URL的时候
                    String attribMethod = null;

                    /**
                     * 这段代码有一个手法:找到第一个匹配的值,直接跳出双重循环
                     * 这端代码的意思,根据当前方法所有参数类型,获取去class之后,获取所有方法,然后找到有getURL的方法,并进行记录
                     * */
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) { // pubic && getting 方法
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    /**如果不存在URL则需要扔出异常了*/
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }


                    /**生成相应的代码,主要是进行URL赋值*/
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");", urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");", urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);
                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                //获取注解上面的值
                String[] value = adaptiveAnnotation.value();
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }


                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("com.cfs.mini.rpc.Invocation")) {
                        // 生成代码：校验 Invocation 非空
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);

                        // 生成代码：获得方法名
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);

                        // 标记有 Invocation 参数
                        hasInvocation = true;
                        break;
                    }
                }

                String defaultExtName = cachedDefaultName;
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) { // 倒序的原因，因为是顺序获取参数，参见【复杂】2. 的例子
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation) // 当【有】 Invocation 参数时，使用 `URL#getMethodParameter()` 方法。
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else // 当【非】 Invocation 参数时，使用 `URL#getParameter()` 方法。
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else // 当属性名是 "protocol" ，使用 `URL#getProtocl()` 方法获取。
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName); // 此处的 defaultExtName ，可以去掉的。
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }

                // 生成代码：获取参数的代码。例如：String extName = url.getParameter("proxy", "javassist");
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                // 生成代码：拓展对象，调用方法。例如
                // `com.mini.cfs.rpc.ProxyFactory extension = (com.mini.cfs.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(com.mini.cfs.rpc.ProxyFactory.class)
                //                                                                                                           .getExtension(extName);` 。
                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);", type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");


            }

            /**生成对应的方法,把code里面的代码加进去*/
            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");  // 异常
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }

        // 生成类末尾的 `}`
        codeBuidler.append("\n}");

        // 调试，打印生成的代码
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuidler.toString());
        }
        return codeBuidler.toString();
    }

    /**
     * 从缓存中获取当前扩展加载器中加载过的实现类
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中，获得拓展实现类数组
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    // 从配置文件中，加载拓展实现类数组
                    classes = loadExtensionClasses();
                    // 设置到缓存中
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClasses(){
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if(defaultAnnotation !=null ){
            //获取注解上相关的值
            String value = defaultAnnotation.value();

            if ((value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) cachedDefaultName = names[0];
            }
        }
        //注解name已经获取,那么从相应文件加载对应的实现类
        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadFile(extensionClasses, MINI_INTERNAL_DIRECTORY);
        loadFile(extensionClasses, MINI_DIRECTORY);
        loadFile(extensionClasses, SERVICES_DIRECTORY);
        return extensionClasses;
    }

    private void loadFile(Map<String, Class<?>> extensionClasses, String dir){
        //获取完成的文件名  dir + class,getName
        String fileName = dir + type.getName();
        try{
            Enumeration<java.net.URL> urls;

            /**通过相应的类加载找到文件中所有的URL*/
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }

            if(urls!=null){
                while (urls.hasMoreElements()){
                    java.net.URL url = urls.nextElement();
                    try{
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try{
                            String line;
                            while ((line = reader.readLine()) != null) {
                                // 跳过当前被注释掉的情况，例如 #spring=xxxxxxxxx
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        //=具体文件中按照等号进行拆解,line代表具体实现类
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            //加载类之后需要判断是否继承接口,没有继承肯定得直接扔出异常
                                            Class<?> clazz = Class.forName(line, true, classLoader);

                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }

                                            /**
                                             * 当前加载的类存在两种情况
                                             * 第一种具备Adaptive注解,则将他赋值给cachedAdaptiveClass
                                             * 第二种则是不具备,则加载到cachedWrapperClasses这个set集合中
                                             * */
                                            if (clazz.isAnnotationPresent(Adaptive.class)) {
                                                if (cachedAdaptiveClass == null) {
                                                    cachedAdaptiveClass = clazz;
                                                } else if (!cachedAdaptiveClass.equals(clazz)) {
                                                    throw new IllegalStateException("More than 1 adaptive class found: "
                                                            + cachedAdaptiveClass.getClass().getName()
                                                            + ", " + clazz.getClass().getName());
                                                }
                                            } else {
                                                // 缓存拓展 Wrapper 实现类到 `cachedWrapperClasses`
                                                try {
                                                    clazz.getConstructor(type);
                                                    Set<Class<?>> wrappers = cachedWrapperClasses;
                                                    if (wrappers == null) {
                                                        cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                        wrappers = cachedWrapperClasses;
                                                    }
                                                    wrappers.add(clazz);

                                                    // 缓存拓展实现类到 `extensionClasses`
                                                } catch (NoSuchMethodException e) {
                                                    clazz.getConstructor();
                                                    // 未配置拓展名，自动生成。例如，DemoFilter 为 demo 。主要用于兼容 Java SPI 的配置。
                                                    if (name == null || name.length() == 0) {
                                                        // TODO name = findAnnotationName(clazz);
                                                        //name = clazz.getName();
                                                        if (name == null || name.length() == 0) {
                                                            if (clazz.getSimpleName().length() > type.getSimpleName().length() && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                                name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                            } else {
                                                                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                            }
                                                        }
                                                    }
                                                    // 获得拓展名，可以是数组，有多个拓展名。
                                                    String[] names = NAME_SEPARATOR.split(name);
                                                    if (names != null && names.length > 0) {
                                                        // 缓存 @Activate 到 `cachedActivates` 。
                                                        Activate activate = clazz.getAnnotation(Activate.class);
                                                        if (activate != null) {
                                                            cachedActivates.put(names[0], activate);
                                                        }
                                                        for (String n : names) {
                                                            // 缓存到`cachedNames`
                                                            if (!cachedNames.containsKey(clazz)) {
                                                                cachedNames.put(clazz, n);
                                                            }
                                                            // 缓存拓展实现类到 `extensionClasses`
                                                            Class<?> c = extensionClasses.get(n);
                                                            if (c == null) {
                                                                extensionClasses.put(n, clazz);
                                                            } else if (c != clazz) {
                                                                throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Throwable t) {
                                        // 发生异常，记录到异常集合
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        }finally {
                            reader.close();
                        }

                    }catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " +
                                type + ", class file: " + url + ") in " + url, t);
                    }
                }
            }

//            private String findAnnotationName(Class<?> clazz) {
//                com.cfs.mini.common.Extension extension = clazz.getAnnotation(com.alibaba.dubbo.common.Extension.class);
//                if (extension == null) {
//                    String name = clazz.getSimpleName();
//                    if (name.endsWith(type.getSimpleName())) {
//                        name = name.substring(0, name.length() - type.getSimpleName().length());
//                    }
//                    return name.toLowerCase();
//                }
//                return extension.value();
//            }

        }catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }


    /**
     * 获取扩展加载器所在的类加载器
     * */
    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    /**
     * 创建一个可适配的扩展加载点
     * */
    private T createAdaptiveExtension() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can not create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    private T injectExtension(T instance) {
        try{
            //
            if(objectFactory!=null){
                for (Method method : instance.getClass().getMethods()) {
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) { // setting && public 方法
                        // 获得属性的类型
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            // 获得属性
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            // 获得属性值
                            Object object = objectFactory.getExtension(pt, property);
                            // 设置属性值
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }

        return instance;
    }


    /**
     * 检测类是否包含SPI注解
     * */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }
}
