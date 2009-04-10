/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ErrorBoundNode extends ABoundNode {

    public ErrorBoundNode(ISyntaxNode node) {
        super(node, IBoundNode.EMPTY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#assign(java.lang.Object)
     */
    @Override
    public void assign(Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluateRuntime(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException("You are trying to run openl code with a compile error in it");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#invoke(java.lang.Object[])
     */
    // public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
    // {
    // throw new UnsupportedOperationException();
    // }
    //

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getChild()
     */
    @Override
    public IBoundNode[] getChildren() {
        return IBoundNode.EMPTY;
    }

    @Override
    public IBoundNode getTargetNode() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#isLvalue()
     */
    @Override
    public boolean isLvalue() {
        return false;
    }

}
