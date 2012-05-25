package org.openl.tablets.tutorial6.sudoku;

import java.util.List;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.SimpleHtmlFilter;
import org.openl.rules.table.ui.filters.TableValueFilter;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.MethodsHelper;

/**
 * 
 * @author snshor
 *
 * The GameInterface class displays the solution by substituting the values in the original table by
 * the ones from the solution. It uses TableValueFilter to do this.
 */

public class GameInterface 
{
	
    public static IGridTable findTable(String methodName, List<IOpenMethod> methods) {

		IOpenMethod m = MethodsHelper.getSingleMethod(methodName, methods);
		DecisionTable dt = (DecisionTable) m;
		TableSyntaxNode tsn = dt.getSyntaxNode();

		IGridTable gt = tsn.getTable(IXlsTableNames.VIEW_BUSINESS).getSource();
		return gt;
	}

    public static IGridTable findTable(String methodName, Object thizz) {
        return findTable(methodName, ((DynamicObject) thizz).getType().getMethods());   
    }

    public static Object display(String methodName, Object thizz, final int[][] res) {
		IGridTable gt = findTable(methodName, thizz);

		TableValueFilter.Model model = new TableValueFilter.Model() {

			public Object getValue(int col, int row) {
				if (row < 1)
					return null; // the row 0 contains column headers
				if (col < 0)
					return null;
				if (res == null) {				    
				    return null;
				}
				if (res.length <= col || res.length < row)
					return null;

				return res[row-1][col];
			}

		};

		IGridFilter[] filters = { new TableValueFilter(gt, model), new SimpleHtmlFilter(), };

		FilteredGrid fg = new FilteredGrid(gt.getGrid(), filters);

		return new Object[]{gt, new GridTable(gt.getRegion(), fg)};
	}

}
