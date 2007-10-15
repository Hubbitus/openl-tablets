/**
 * Created Feb 17, 2007
 */
package org.openl.rules.ui;

import static org.openl.jsf.Util.getWebStudio;
import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 */
public class EditorHelper implements WebStudio.StudioListener
{
	private TableEditorModel model;
    private int elementID = -1;

    public EditorHelper() {
        WebStudio webStudio = getWebStudio();
        if (webStudio != null) webStudio.addEventListener(this);
    }

    public void setTableID(int elementID, ProjectModel prj, String mode, boolean cancel)
	{
        if (model != null && (cancel || elementID != this.elementID)) model.cancel();
        IGridTable table = prj.getTableWithMode(elementID, mode);
        TableEditorModel newModel = new TableEditorModel(table);
        if (model != null && elementID == this.elementID) newModel.getUndoableActions(model);
        model = newModel;
        this.elementID = elementID;
    }

   public void setTableID(int elementID, ProjectModel prj)
	{
		setTableID(elementID, prj, null, true);
	}

    public int getElementID() {
        return elementID;
    }

    public String showTable()
	{
		return ProjectModel.showTable(model.getUpdatedTable(),  false);
	}


	public TableEditorModel getModel()
	{
		return this.model;
	}


	public void setModel(TableEditorModel model)
	{
		this.model = model;
	}

    public void studioReset() {
        if (model != null) {
            model.cancel();
            model = null;
        }
        elementID = -1;
    }
}
