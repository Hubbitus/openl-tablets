package org.openl.extension.xmlrules.model.single.node.function;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.node.FunctionNode;

public final class FunctionResolverFactory {
    private FunctionResolverFactory() {
    }

    public static FunctionResolver getResolver(FunctionNode node) {
        String functionName = node.getName();
        if (functionName.equals("Out")) {
            return new OutFunctionResolver();
        }
        ProjectData projectData = ProjectData.getCurrentInstance();
        for (Table table : projectData.getTables()) {
            if (functionName.equals(table.getName())) {
                return new TableResolver();
            }
        }

        return new DefaultFunctionResolver();
    }
}
