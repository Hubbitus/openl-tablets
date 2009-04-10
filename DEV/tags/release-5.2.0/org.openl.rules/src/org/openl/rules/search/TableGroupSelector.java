/**
 * Created May 9, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 *
 */
public class TableGroupSelector extends ATableSyntaxNodeSelector implements ISearchConstants {

    class GroupResult {
        boolean groupRes = true;
        int idx = 0;

        boolean execute(Object target) {
            boolean res = true;
            while (idx < tableElements.length) {
                int oldIdx = idx;
                boolean x = executeGroup(target);
                res = tableElements[oldIdx].getOperator().op(res, x);
            }

            return res;
        }

        boolean executeGroup(Object target) {
            boolean res = true;
            int N = tableElements.length;
            for (int i = idx; i < N; ++i) {
                if (i > idx && tableElements[i].getOperator().isGroup()) {
                    idx = i;
                    return res;
                }

                boolean x = selectors[i].select(target);
                x = tableElements[i].isNotFlag() ? !x : x;
                res = i == idx ? x : tableElements[i].getOperator().op(res, x);

            }
            idx = N;
            return res;

        }
    }
    SearchElement[] tableElements;

    ATableSyntaxNodeSelector[] selectors;

    /**
     * @param tableElements
     */
    public TableGroupSelector(SearchElement[] tableElements) {
        this.tableElements = tableElements;
        init(tableElements);
    }

    void init(SearchElement[] se) {
        selectors = new ATableSyntaxNodeSelector[se.length];
        for (int i = 0; i < se.length; i++) {
            ATableSyntaxNodeSelector tsnSel = makeTableSyntaxNodeSelector(se[i]);
            selectors[i] = tsnSel;
        }
    }

    /**
     * @param element
     * @return
     */
    private ATableSyntaxNodeSelector makeTableSyntaxNodeSelector(SearchElement se) {
        if (HEADER.equals(se.getType())) {
            return new TableHeaderSelector(se);
        }
        if (PROPERTY.equals(se.getType())) {
            return new PropertySelector(se);
        }
        throw new RuntimeException("Unknown selector type: " + se.getType());
    }

    @Override
    public boolean selectTable(TableSyntaxNode node) {
        return new GroupResult().execute(node);

    }

}
