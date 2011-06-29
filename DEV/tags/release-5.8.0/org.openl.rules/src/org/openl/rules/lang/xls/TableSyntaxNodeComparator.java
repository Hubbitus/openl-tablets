package org.openl.rules.lang.xls;

import java.util.Comparator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class TableSyntaxNodeComparator implements Comparator<TableSyntaxNode> {

    public int compare(TableSyntaxNode tableSyntaxNode1, TableSyntaxNode tableSyntaxNode2) {

        String type1 = tableSyntaxNode1.getType();
        String type2 = tableSyntaxNode2.getType();

        int i1 = XlsNodeTypes.XLS_TEST_METHOD.toString().equals(type1) || 
            XlsNodeTypes.XLS_RUN_METHOD.toString().equals(type1) ? 1 : 0;
        int i2 = XlsNodeTypes.XLS_TEST_METHOD.toString().equals(type2) || 
            XlsNodeTypes.XLS_RUN_METHOD.toString().equals(type2) ? 1 : 0;

        return i1 - i2;
    }
}