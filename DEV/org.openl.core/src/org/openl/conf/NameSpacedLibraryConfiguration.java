/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.MethodSearch;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionsUtil;

/**
 * @author snshor
 *
 */
public class NameSpacedLibraryConfiguration extends AConfigurationElement {

    String namespace;

    IMethodFactoryConfigurationElement[] factories = {};

    public void addAnyLibrary(GenericLibraryConfiguration glb) {
        factories = (IMethodFactoryConfigurationElement[]) CollectionsUtil.add(factories, glb);
    }

    public void addJavalib(JavaLibraryConfiguration factory) {
        factories = (IMethodFactoryConfigurationElement[]) CollectionsUtil.add(factories, factory);
    }

    public IOpenField getField(String name, IConfigurableResourceContext cxt, boolean strictMatch) {
        for (int i = 0; i < factories.length; i++) {
            IOpenField field = factories[i].getLibrary(cxt).getVar(name, strictMatch);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    public IMethodCaller getMethodCaller(String name,
            IOpenClass[] params,
            ICastFactory casts,
            IConfigurableResourceContext cxt) throws AmbiguousMethodException {
        for (int i = 0; i < factories.length; i++) {
            IMethodCaller mc = MethodSearch.getMethodCaller(name, params, casts, factories[i].getLibrary(cxt), true);
            if (mc != null) {
                return mc;
            }
        }
        
        List<IOpenMethod> methods = new LinkedList<IOpenMethod>();
        for (int i = 0; i < factories.length; i++) {
            Iterator<IOpenMethod> itr = factories[i].getLibrary(cxt).methods(name);
            while (itr.hasNext()){
                methods.add(itr.next());
            }
        }
        
        return MethodSearch.getCastingMethodCaller(name, params, casts, methods.iterator());
    }

    /**
     * @return
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param string
     */
    public void setNamespace(String string) {
        namespace = string;
    }

    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        for (int i = 0; i < factories.length; i++) {
            factories[i].validate(cxt);
        }
    }

}
