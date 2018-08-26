package com.cfs.mini.config;

import java.util.List;

public  abstract class AbstractInterfaceConfig extends AbstractMethodConfig {


    protected String stub;

    public String getStub() {
        return stub;
    }

    public void setStub(String stub) {
        this.stub = stub;
    }


    /**
     * 解析应用配置
     *
     * */
    protected ApplicationConfig application;

    public ApplicationConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationConfig application) {
        this.application = application;
    }

    /**
     *
     * */
    protected MonitorConfig monitor;

    /**
     * 检验接口是否存在配置文件中方法
     * */
    protected void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
         if(interfaceClass == null ){
             throw new IllegalStateException("interface not allow null!");
         }

        //验证类是否是一个接口
         if (!interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }

        /**
         * 检验接口是否存在当前方法,如果接口不存在这个方法,那么是无法进行暴露的
         * 这种情况是防止配置是否显示配置了一个在接口中不存在的方法
         * */
        if(methods !=null && !methods.isEmpty()){
             for(MethodConfig methodConfig:methods){
                 String methodName = methodConfig.getName();
                 if (methodName == null || methodName.length() == 0) {
                     throw new IllegalStateException("<mini:method> name attribute is required! Please check: <dubbo:service interface=\"" + interfaceClass.getName() + "\" ... ><dubbo:method name=\"\" ... /></<dubbo:reference>");
                 }
                 boolean hasMethod = false;
                 for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
                     if (method.getName().equals(methodName)) {
                         hasMethod = true;
                         break;
                     }
                 }
                 if (!hasMethod) {
                     throw new IllegalStateException("The interface " + interfaceClass.getName() + " not found method " + methodName);
                 }
             }
        }
    }
}
