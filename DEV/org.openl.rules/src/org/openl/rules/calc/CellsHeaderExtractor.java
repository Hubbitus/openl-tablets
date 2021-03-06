package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;

/**
 * Extractor for values that are represented as column and row names in spreadsheet.
 * 
 * @author DLiauchuk
 *
 */
public class CellsHeaderExtractor {
    private String[] rowNames;
    private String[] columnNames;
    private Set<String> dependentSpreadsheetTypes;
    
    /** table representing column section in the spreadsheet **/
    private final ILogicalTable columnNamesTable;

    /** table representing row section in the spreadsheet **/
    private final ILogicalTable rowNamesTable;

    /** Spreadsheet signature */
    private final String spreadsheetSignature;
    
    // regex that represents the next line:
    // [any_symbols] : SpreadsheetResult<custom_spreadsheet_result_name>
    //
    // package scope just for tests
    static final String DEPENDENT_CSR_REGEX = "^.*\\s*:\\s*SpreadsheetResult[^\\s\\[\\]].*";

    /**
     * Pattern that represents the next line:
     * Spreadsheet SpreadsheetResult<custom_spreadsheet_result_name> <name_and_params_declaration>
     */
    private static final Pattern CSR_IN_RETURN_PATTERN = Pattern.compile("\\s*Spreadsheet\\s*SpreadsheetResult([^\\s\\[\\]]+).+");

    /**
     * Pattern that represents parameters of the spreadsheet
     */
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("\\((.+)\\)");

    /**
     * Pattern that represents custom spreadsheet type parameter
     */
    private static final Pattern CSR_TYPE_PATTERN = Pattern.compile("\\s*SpreadsheetResult([^\\s\\[\\]]+).+");
    
    public CellsHeaderExtractor(String spreadsheetSignature, ILogicalTable columnNamesTable, ILogicalTable rowNamesTable) {
        this.spreadsheetSignature = spreadsheetSignature;
        this.columnNamesTable = columnNamesTable;
        this.rowNamesTable = rowNamesTable;
    }

    public void extract() {
        extractRowNames();
        extractColumnNames();
    }
    
    public ILogicalTable getColumnNamesTable() {
        return columnNamesTable;
    }
    
    public ILogicalTable getRowNamesTable() {
        return rowNamesTable;
    }
     
    public String[] getRowNames() {
        if (rowNames == null) {
            extractRowNames();
        }
        return rowNames.clone();
    }
    
    public String[] getColumnNames() {
        if (columnNames == null) {
            extractColumnNames();
        }
        return columnNames.clone();
    }
    
    public Set<String> getDependentSpreadsheetTypes() {
        if (dependentSpreadsheetTypes == null) {
            dependentSpreadsheetTypes = new HashSet<String>();
            dependentSpreadsheetTypes.addAll(getSignatureDependencies(spreadsheetSignature));
            dependentSpreadsheetTypes.addAll(getDependencies(columnNames));
            dependentSpreadsheetTypes.addAll(getDependencies(rowNames));
        }
        return dependentSpreadsheetTypes;
    }
    
    public StringValue getRowNameForHeader(String value, int row, IBindingContext bindingContext) {
        StringValue sv = null;
        if (value != null) {
            String shortName = String.format("srow%d", row);
            IGridTable nameCell = rowNamesTable.getRow(row).getColumn(0).getSource();
            sv = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                bindingContext));
        }
        return sv;
    }
    
    public StringValue getColumnNameForHeader(String value, int column, IBindingContext bindingContext) {
        StringValue stringValue = null;
        if (value != null) {
            String shortName = String.format("scol%d", column);            
            IGridTable nameCell = columnNamesTable.getColumn(column).getRow(0).getSource();            
            stringValue = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                bindingContext));
        }
        return stringValue;
    }
    
    private List<String> getDependencies(String[] cellNames) {
        List<String> dependentSpreadsheets = new ArrayList<String>();
        
        for (String cellName : cellNames) {
            if (cellName != null && cellName.matches(DEPENDENT_CSR_REGEX)) {
                String[] res = cellName.split(SpreadsheetResult.class.getSimpleName());
                String dependentMethodName = res[res.length - 1];
                /*If we have the array of spreadsheetresult we need to delete [] from the method name*/
                dependentMethodName = dependentMethodName.replace("[]", "");
                
                dependentSpreadsheets.add(dependentMethodName);
            }
        }
        return dependentSpreadsheets;
    }

    // package scope just for tests
    static List<String> getSignatureDependencies(String signature) {
        List<String> dependentSpreadsheets = new ArrayList<String>();

        Matcher matcher = CSR_IN_RETURN_PATTERN.matcher(signature);
        if (matcher.matches()) {
            dependentSpreadsheets.add(matcher.group(1));
        }

        matcher = PARAMETERS_PATTERN.matcher(signature);
        if (matcher.find()) {
            String allParams = matcher.group(1);
            for (String param : allParams.split("\\s*,\\s*")) {
                Matcher paramMatcher = CSR_TYPE_PATTERN.matcher(param);
                if (paramMatcher.matches()) {
                    dependentSpreadsheets.add(paramMatcher.group(1));
                }
            }
        }

        return dependentSpreadsheets;
    }

    private String[] extractRowNames() {   
        int height = rowNamesTable.getHeight();
        rowNames = new String[height];
        for (int row = 0; row < height; row++) {
            rowNames[row] = getRowName(row, rowNamesTable.getRow(row));
        }        
        return rowNames;        
    }
    
    private String[] extractColumnNames() {        
        int width = columnNamesTable.getWidth();  
        columnNames = new String[width];
        for (int col = 0; col < width; col++) {
            columnNames[col] = getColumnName(col, columnNamesTable.getColumn(col));
        }
        return columnNames;
    }
    
    private String getRowName(int row, ILogicalTable rowNameCell) {
        IGridTable nameCell = rowNameCell.getColumn(0).getSource();
        String value = nameCell.getCell(0, 0).getStringValue();
        return value;
    }
    
    private String getColumnName(int column, ILogicalTable columnNameCell) {
        IGridTable nameCell = columnNameCell.getRow(0).getSource();
        String value = nameCell.getCell(0, 0).getStringValue();
        return value;
    }
}
