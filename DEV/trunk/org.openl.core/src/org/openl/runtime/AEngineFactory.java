package org.openl.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public abstract class AEngineFactory {

    private static final String FIELD_PREFIX = "get";

    public abstract Object makeInstance();

    protected Object makeEngineInstance(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv,
            ClassLoader classLoader) {

        Class<?>[] proxyInterfaces = getInstanceInterfaces();
        InvocationHandler handler = makeInvocationHandler(openClassInstance, methodMap, runtimeEnv);

        return Proxy.newProxyInstance(classLoader, proxyInterfaces, handler);
    }

    protected abstract Class<?>[] getInstanceInterfaces();

    protected abstract InvocationHandler makeInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv);

    /**
     * Creates methods map that contains interface's methods as key and
     * appropriate open class's members as value.
     * 
     * @param engineInterface interface that provides method for engine
     * @param openClass open class that used by engine to invoke appropriate
     *            rules
     * @return methods map
     */
    protected Map<Method, IOpenMember> makeMethodMap(Class<?> engineInterface, IOpenClass openClass) {

        // Methods map.
        // 
        Map<Method, IOpenMember> methodMap = new HashMap<Method, IOpenMember>();
        // Get declared by engine interface methods.
        //
        Method[] interfaceMethods = engineInterface.getDeclaredMethods();

        for (Method interfaceMethod : interfaceMethods) {
            // Get name of method.
            //
            String interfaceMethodName = interfaceMethod.getName();
            // Try to find openClass's method with appropriate name and
            // parameter types.
            //
            IOpenMethod rulesMethod = openClass.getMatchingMethod(interfaceMethodName,
                JavaOpenClass.getOpenClasses(interfaceMethod.getParameterTypes()));

            if (rulesMethod != null) {
                // If openClass has appropriate method then add new entry to
                // methods map.
                //
                methodMap.put(interfaceMethod, rulesMethod);
            } else {
                // Try to find appropriate method candidate in openClass's
                // fields.
                //
                if (interfaceMethodName.startsWith(FIELD_PREFIX)) {
                    // Build field name to find.
                    //
                    String fieldName = StringUtils.uncapitalize(interfaceMethodName.substring(FIELD_PREFIX.length()));
                    // Try to find appropriate field.
                    //
                    IOpenField rulesField = openClass.getField(fieldName, true);

                    if (rulesField != null) {
                        // Cast method return type to appropriate OpenClass
                        // type.
                        //
                        JavaOpenClass methodReturnType = JavaOpenClass.getOpenClass(interfaceMethod.getReturnType());

                        if (methodReturnType == rulesField.getType()) {
                            // If openClass's field type is equal to method
                            // return
                            // type then add new entry to methods map.
                            //
                            methodMap.put(interfaceMethod, rulesField);
                            // Jump to the next interface method.
                            //
                            continue;
                        } else {
                            // If openClass doesn't have appropriate field
                            // (field's type doesn't
                            // equal to method return type) then throw runtime
                            // exception.
                            //
                            String message = String.format("Return type of method \"%s\" should be %s",
                                interfaceMethodName,
                                rulesField.getType());

                            throw new RuntimeException(message);
                        }
                    }
                }

                // If openClass doesn't have appropriate method or field then
                // throw runtime exception.
                //
                String message = String.format("There is no implementation in rules for interface method \"%s\"",
                    interfaceMethod);

                throw new RuntimeException(message);
            }
        }

        return methodMap;
    }

}
