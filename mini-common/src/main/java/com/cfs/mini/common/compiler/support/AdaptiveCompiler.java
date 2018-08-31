package com.cfs.mini.common.compiler.support;


import com.cfs.mini.common.compiler.Compiler;
import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.ExtensionLoader;

@Adaptive
public class AdaptiveCompiler implements Compiler {


    private static volatile String DEFAULT_COMPILER;

    public static void setDefaultCompiler(String compiler) {
        DEFAULT_COMPILER = compiler;
    }

    @Override
    public Class<?> compile(String code, ClassLoader classLoader) {
        Compiler compiler;
        ExtensionLoader<Compiler> loader = ExtensionLoader.getExtensionLoader(Compiler.class);

        String name = DEFAULT_COMPILER;

        if(name!=null&&name.length()>0){
            compiler = loader.getExtension(name);
        }else{
            compiler = loader.getDefaultExtension();
        }
        return compiler.compile(code, classLoader);
    }
}
