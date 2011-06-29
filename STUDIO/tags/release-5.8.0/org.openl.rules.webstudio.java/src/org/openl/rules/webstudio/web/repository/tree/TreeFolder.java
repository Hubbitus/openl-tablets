package org.openl.rules.webstudio.web.repository.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.openl.util.filter.IFilter;

/**
 * Represents OpenL folder in a tree.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeFolder extends AbstractTreeNode {

    private static final long serialVersionUID = -8236498990436429491L;
    /**
     * Collection of children. In LeafOnly mode it is left uninitialized.
     */
    private Map<Object, AbstractTreeNode> elements;
    
    private IFilter<AProjectArtefact> filter;

    public TreeFolder(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name);
        this.filter = filter;
    }

    // ------ UI methods ------

    /** {@inheritDoc} */
    @Override
    public String getIcon() {
        return UiConst.ICON_FOLDER;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconLeaf() {
        // in both cases we use the same icons
        return getIcon();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return UiConst.TYPE_FOLDER;
    }
    
    @Override
    public String getId() {
        return AbstractTreeNode.FOLDER_PREFIX + super.getId();
    }
    
    protected Map<Object, AbstractTreeNode> getElements() {
        if (elements == null && !isLeafOnly()) {
            elements = new TreeMap<Object, AbstractTreeNode>();
            if (getData() instanceof AProjectFolder) {
                AProjectFolder folder = (AProjectFolder) getData();
                Collection<AProjectArtefact> filteredArtefacts = new ArrayList<AProjectArtefact>();
                for (AProjectArtefact artefact : folder.getArtefacts()) {
                    if (!(filter.supports(artefact.getClass()) && !filter.select(artefact))) {
                        filteredArtefacts.add(artefact);
                    }
                }

                AProjectArtefact[] sortedArtefacts = new AProjectArtefact[filteredArtefacts.size()];
                sortedArtefacts = filteredArtefacts.toArray(sortedArtefacts);

                Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);

                for (AProjectArtefact artefact : sortedArtefacts) {
                    addChild(artefact);
                }
            }
        }
        return elements;
    }
    
    public void addChild(AProjectArtefact childArtefact){
        String id = childArtefact.getName();
        if (childArtefact.isFolder()) {
            TreeFolder treeFolder = new TreeFolder(id, childArtefact.getName(), filter);
            treeFolder.setData(childArtefact);
            add(treeFolder);
        } else {
            TreeFile treeFile = new TreeFile(id, childArtefact.getName());
            treeFile.setData(childArtefact);
            add(treeFile);
        }
    }
    
    @Override
    public void refresh() {
        super.refresh();
        elements = null;
    }

}