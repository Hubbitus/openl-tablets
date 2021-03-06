package org.openl.rules.tbasic;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.IBindingContext;
import org.openl.domain.EnumDomain;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.tbasic.compile.AlgorithmCompiler;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

import java.util.*;

public class AlgorithmBuilder {

    private static final String LABEL = "label";
    private static final String DESCRIPTION = "description";
    private static final String OPERATION1 = "operation";
    private static final String CONDITION = "condition";
    private static final String ACTION = "action";
    private static final String BEFORE = "before";
    private static final String AFTER = "after";
    private static final String CELL = "cell";
    private static final String UNDERSCORE = "_";

    // Section Description Operation Condition Action Before After
    private static class AlgorithmColumn {
        private String id;
        private int columnIndex;

        private AlgorithmColumn(String id, int columnIndex) {
            this.id = id;
            this.columnIndex = columnIndex;
        }
    }

    private static final String OPERATION = "Operation";

    public static String[] algorithmOperationsArray = null;

    static {
        AlgorithmTableParserManager tbasicParser = AlgorithmTableParserManager.instance();
        TableParserSpecificationBean[] algSpecifications = tbasicParser.getAlgorithmSpecification();

        Set<String> algorithmOperations = new LinkedHashSet<String>();

        for (TableParserSpecificationBean specification : algSpecifications) {
            algorithmOperations.add(specification.getKeyword());
        }
        algorithmOperationsArray = algorithmOperations.toArray(new String[algorithmOperations.size()]);
    }


    private final IBindingContext bindingContext;

    private final Algorithm algorithm;

    private final TableSyntaxNode tsn;

    private Map<String, AlgorithmColumn> columns;

    public AlgorithmBuilder(IBindingContext ctx, Algorithm algorithm, TableSyntaxNode tsn) {
        bindingContext = ctx;
        this.algorithm = algorithm;
        this.tsn = tsn;
    }

    /**
     * Sets CellMetaInfo for operation cell. Thus, editor can use special
     * controller to validate/limit user input.
     *
     * @param c
     * @param r
     * @param grid
     */
    private void bindMetaInfo(IGridTable grid, int c, int r) {
        CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE, null, new DomainOpenClass("operation",
                JavaOpenClass.STRING, new EnumDomain<String>(algorithmOperationsArray), null), false);
        IWritableGrid wgrid = (IWritableGrid) grid.getGrid();
        wgrid.setCellMetaInfo(IGridRegion.Tool.getAbsoluteColumn(grid.getRegion(), c), IGridRegion.Tool.getAbsoluteRow(
                grid.getRegion(), r), meta);
    }

    public void build(ILogicalTable tableBody) throws Exception {
        
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError("Invalid table. Provide table body", null, tsn);
        }
        
        if (tableBody.getHeight() <= 2) {
            throw SyntaxNodeExceptionUtils.createError("Unsufficient rows. Must be more than 2!", null, tsn);
        }

        prepareColumns(tableBody);

        // parse data, row=2..*
        List<AlgorithmRow> algorithmRows = buildRows(tableBody);

        RowParser rowParser = new RowParser(algorithmRows, AlgorithmTableParserManager.instance()
                .getAlgorithmSpecification());

        List<AlgorithmTreeNode> parsedNodes = rowParser.parse();

        AlgorithmCompiler compiler = new AlgorithmCompiler(bindingContext, algorithm.getHeader(), parsedNodes);
        compiler.compile(algorithm);
    }

    private List<AlgorithmRow> buildRows(ILogicalTable tableBody) throws SyntaxNodeException {
        List<AlgorithmRow> result = new ArrayList<AlgorithmRow>();

        IGridTable grid = tableBody.getRows(2).getSource();
        for (int r = 0; r < grid.getHeight(); r++) {

            AlgorithmRow aRow = new AlgorithmRow();

            // set sequential number of the row in table
            aRow.setRowNumber(r + 1);

            IGridTable rowTable = grid.getRow(r);
            aRow.setGridRegion(rowTable.getRegion());

            // parse data row
            for (AlgorithmColumn column : columns.values()) {
                int c = column.columnIndex;

                IGridTable valueTable = rowTable.getColumn(c);
                aRow.setValueGridRegion(column.id, valueTable.getRegion());

                String value = grid.getCell(c, r).getStringValue();

                if (value == null) {
                    value = StringUtils.EMPTY;
                }

                StringValue sv = new StringValue(value, CELL + r + UNDERSCORE + c, null, new GridCellSourceCodeModule(grid,
                        c, r, bindingContext));

                setRowField(aRow, column.id, sv);
                if (OPERATION.equalsIgnoreCase(column.id)) {
                    ICellStyle cellStyle = grid.getCell(c, r).getStyle();
                    int i = (cellStyle == null) ? 0 : cellStyle.getIdent();
                    aRow.setOperationLevel(i);
                    bindMetaInfo(grid, c, r);
                }
            }

            result.add(aRow);
        }

        return result;
    }

    private void prepareColumns(ILogicalTable tableBody) throws SyntaxNodeException {
        columns = new HashMap<String, AlgorithmColumn>();

        ILogicalTable ids = tableBody.getRow(0);

        // parse ids, row=0
        for (int c = 0; c < ids.getWidth(); c++) {
            String id = safeId(ids.getColumn(c).getSource().getCell(0, 0).getStringValue());
            if (id.length() == 0) {
                // ignore column with NO ID
                continue;
            }

            if (columns.get(id) != null) {
                // duplicate ids
                throw SyntaxNodeExceptionUtils.createError("Duplicate column '" + id + "'!", null, tsn);
            }

            columns.put(id, new AlgorithmColumn(id, c));
        }
    }

    private String safeId(String s) {
        String id = "";
        if (s != null) {
            id = s.trim().toLowerCase();
        }
        return id;
    }

    private void setRowField(AlgorithmRow row, String column, StringValue sv) throws SyntaxNodeException {
        if (LABEL.equalsIgnoreCase(column)) {
            row.setLabel(sv);
        } else if (DESCRIPTION.equalsIgnoreCase(column)) {
            row.setDescription(sv);
        } else if (OPERATION1.equalsIgnoreCase(column)) {
            row.setOperation(sv);
        } else if (CONDITION.equalsIgnoreCase(column)) {
            row.setCondition(sv);
        } else if (ACTION.equalsIgnoreCase(column)) {
            row.setAction(sv);
        } else if (BEFORE.equalsIgnoreCase(column)) {
            row.setBefore(sv);
        } else if (AFTER.equalsIgnoreCase(column)) {
            row.setAfter(sv);
        } else {
            throw SyntaxNodeExceptionUtils.createError("Invalid column id '" + column + "'!", null, tsn);
        }
    }
}
