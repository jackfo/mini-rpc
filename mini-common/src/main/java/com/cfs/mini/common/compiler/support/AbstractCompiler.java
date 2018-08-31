package com.cfs.mini.common.compiler.support;

import com.cfs.mini.common.compiler.Compiler;
import com.cfs.mini.common.utils.ClassHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCompiler implements Compiler {
    /**
     * 正则 - 包名
     */
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([$_a-zA-Z][$_a-zA-Z0-9\\.]*);");
    /**
     * 正则 - 类名
     */
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s+");

    /**
     * 在这里比较有特色的手法是根据正则获取类名
     * */
    @Override
    public Class<?> compile(String code, ClassLoader classLoader) {
        code = code.trim();
        Matcher matcher = PACKAGE_PATTERN.matcher(code);
        String pkg;
        if (matcher.find()) {
            pkg = matcher.group(1);
        } else {
            pkg = "";
        }

        matcher = CLASS_PATTERN.matcher(code);

        String cls;
        if (matcher.find()) {
            cls = matcher.group(1);
        } else {
            throw new IllegalArgumentException("No such class name in " + code);
        }
        // 获得完整类名
        String className = pkg != null && pkg.length() > 0 ? pkg + "." + cls : cls;

        try {
            // 加载成功，说明已存在
            return Class.forName(className, true, ClassHelper.getCallerClassLoader(getClass())); // classloader 为调用方的
        } catch (ClassNotFoundException e) { // 类不存在，说明可能未编译过，进行编译
            // 代码格式不正确
            if (!code.endsWith("}")) {
                throw new IllegalStateException("The java code not endsWith \"}\", code: \n" + code + "\n");
            }
            // 编译代码
            try {
                return doCompile(className, code);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IllegalStateException("Failed to compile class, cause: " + t.getMessage() + ", class: " + className + ", code: \n" + code + "\n, stack: " );
            }
        }
    }

    protected abstract Class<?> doCompile(String name, String source) throws Throwable;
}
