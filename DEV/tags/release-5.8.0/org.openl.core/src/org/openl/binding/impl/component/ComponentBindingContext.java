package org.openl.binding.impl.component;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.MethodSearch;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.StringTool;

/**
 * Binding context for different Openl components.<br>
 * Handles {@link ComponentOpenClass} for which binding is performed.<br> 
 * And a map of internal types that are found during binding.<br>
 * 
 * Was created by extracting functionality from {@link ModuleBindingContext} of 20192 revision.
 * 
 * @author DLiauchuk
 *
 */
public class ComponentBindingContext extends BindingContextDelegator {
    
    private ComponentOpenClass componentOpenClass;
    
    private Map<String, IOpenClass> internalTypes = null;
    
    public ComponentBindingContext(IBindingContext delegate, ComponentOpenClass componentOpenClass) {
        super(delegate);
        this.componentOpenClass = componentOpenClass;
    }
    
    protected ComponentOpenClass getComponentOpenClass() {
        return componentOpenClass;
    }
    
    @Override
    public synchronized void addType(String namespace, IOpenClass type)
            throws Exception {

        String nameWithNamespace = StringTool.buildTypeName(namespace, type.getName());
        add(nameWithNamespace, type);
    }

    private synchronized void add(String nameWithNamespace, IOpenClass type) throws Exception {
        Map<String, IOpenClass> map = initInternalTypes();
        
        if (map.containsKey(nameWithNamespace)) {
            throw new Exception("Type " + nameWithNamespace + " has been defined already");
        }

        map.put(nameWithNamespace, type);
    }
    
    @Override
    public synchronized void addTypes(Map<String, IOpenClass> types) throws Exception {
        for (String nameWithNamespace : types.keySet()) {
            add(nameWithNamespace, types.get(nameWithNamespace));
        }
    }

    @Override
    public synchronized void removeType(String namespace, IOpenClass type)
            throws Exception {

        String key = StringTool.buildTypeName(namespace, type.getName());
        Map<String, IOpenClass> map = initInternalTypes();
        map.remove(key);
    }

    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type)
            throws DuplicatedVarException {
        return null;
    }
    
    private synchronized Map<String, IOpenClass> initInternalTypes() {
        if (internalTypes == null) {
            internalTypes = new HashMap<String, IOpenClass>();
        }
        return internalTypes;
    }
    
    
    
    @Override
    public IMethodCaller findMethodCaller(String namespace, String methodName,
            IOpenClass[] parTypes) throws AmbiguousMethodException {

        IMethodCaller imc = null;
        if (ISyntaxConstants.THIS_NAMESPACE.equals(namespace)) {
            imc = MethodSearch.getMethodCaller(methodName, parTypes, this,
                componentOpenClass);
        }

        return imc != null ? imc : super.findMethodCaller(namespace,
                methodName, parTypes);
    }
    
    @Override
    public IOpenClass findType(String namespace, String typeName) {

        if (internalTypes != null) {
            String key = StringTool.buildTypeName(namespace, typeName);
            IOpenClass ioc = internalTypes.get(key);
            if (ioc != null) {
                return ioc;
            }
        }

        return super.findType(namespace, typeName);
    }
    
    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch)
            throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = componentOpenClass.getField(name, strictMatch);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }
}