package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.impl.local.LocalArtefactAPI;
import org.openl.rules.webstudio.web.diff.AbstractDiffController;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Supplies repository structured diff UI tree with data.
 *
 * @author Andrey Naumenko
 */
@ManagedBean
@SessionScoped
public class RepositoryDiffController extends AbstractDiffController {
    private final Logger log = LoggerFactory.getLogger(RepositoryDiffController.class);

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{designTimeRepository}")
    private DesignTimeRepository designTimeRepository;

    private AProject projectUW; // User Workspace project
    private AProject projectRepo; // Repository project
    private List<AProjectArtefact> excelArtefactsUW;
    private List<AProjectArtefact> excelArtefactsRepo;

    private String selectedExcelFileUW;
    private String selectedExcelFileRepo;
    private String selectedVersionRepo;

    public void setSelectedExcelFileUW(String selectedExcelFileUW) {
        this.selectedExcelFileUW = selectedExcelFileUW;
    }

    public String getSelectedExcelFileUW() {
        return selectedExcelFileUW;
    }

    public void setSelectedExcelFileRepo(String selectedExcelFileRepo) {
        this.selectedExcelFileRepo = selectedExcelFileRepo;
    }

    public String getSelectedExcelFileRepo() {
        return selectedExcelFileRepo;
    }

    public void setSelectedVersionRepo(String selectedVersionRepo) {
        this.selectedVersionRepo = selectedVersionRepo;
    }

    public String getSelectedVersionRepo() {
        return selectedVersionRepo;
    }

    public SelectItem[] getVersionsRepo() {
        Collection<ProjectVersion> versions = projectUW.getVersions();
        SelectItem[] selectItems = new SelectItem[versions.size()];

        int i = 0;
        for (ProjectVersion version : versions) {
            selectItems[i] = new SelectItem(version.getVersionName());
            i++;
        }

        return selectItems;
    }

    public List<SelectItem> getExcelFilesUW() {
        init();
        List<SelectItem> excelItems = new ArrayList<SelectItem>();
        for (AProjectArtefact excelArtefact : excelArtefactsUW) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(),
                    excelArtefact.getName()));
        }
        return excelItems;
    }

    public List<SelectItem> getExcelFilesRepo() {
        List<SelectItem> excelItems = new ArrayList<SelectItem>();
        for (AProjectArtefact excelArtefact : excelArtefactsRepo) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(),
                    excelArtefact.getName()));
        }
        return excelItems;
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public String init() {
        initProjectUW();
        initProjectRepo();
        //setDiffTree(null);
        return null;
    }

    public void initProjectUW() {
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            if (projectUW != selectedProject) {
                projectUW = selectedProject;
                selectedVersionRepo = projectUW.getVersion().getVersionName();
                setDiffTree(null);
            }
            excelArtefactsUW = getExcelArtefacts(projectUW, "");
        } catch (Exception e) {
            log.warn("Failed to init Diff controller", e);
        }
    }

    public void initProjectRepo() {
        try {
            CommonVersionImpl version = new CommonVersionImpl(selectedVersionRepo);
            try {
                projectRepo = designTimeRepository.getProject(projectUW.getName(), version);
            } catch (Exception e) {
                log.warn("Could not get project\"{}\" of version \"{}\"", projectUW.getName(), version.getVersionName(), e);
                projectRepo = designTimeRepository.getProject(projectUW.getName());
            }
            excelArtefactsRepo = getExcelArtefacts(projectRepo, "");
        } catch (Exception e) {
            log.warn("Failed to retrieve repository project info for Diff controller", e);
        }
    }

    private List<AProjectArtefact> getExcelArtefacts(AProject project, String rootPath) throws Exception {
        List<AProjectArtefact> excelArtefacts = new ArrayList<AProjectArtefact>();
        Collection<? extends AProjectArtefact> projectArtefacts = null;
        if (rootPath != null) {
            try {
                projectArtefacts = getProjectFolder(project, rootPath).getArtefacts();
            } catch (Exception e) {
                return excelArtefacts;
            }
        } else {
            projectArtefacts = project.getArtefacts();
        }
        for (AProjectArtefact projectArtefact : projectArtefacts) {
            String artefactPath = projectArtefact.getArtefactPath().getStringValue();
            if (projectArtefact.isFolder()) {
                excelArtefacts.addAll(getExcelArtefacts(project,
                        projectArtefact.getArtefactPath().getStringValue()));
            } else if (FileTypeHelper.isExcelFile(artefactPath)) {
                excelArtefacts.add(projectArtefact);
            }
        }
        return excelArtefacts;
    }

    private AProjectFolder getProjectFolder(AProject project, String path) throws ProjectException {
        path = removeProjectName(path);

        if (path.length() == 0) {
            return project;
        }

        AProjectFolder projectFolder = (AProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(path));
        return projectFolder;
    }

    private File downloadExelFile(AProjectArtefact excelArtefact) {
        if (excelArtefact == null) {
            return null;
        }
        InputStream in = null;
        try {
            in = ((AProjectResource) excelArtefact).getContent();
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
        }

        File tempFile = null;
        OutputStream out = null;
        String filePrefix = ((excelArtefact.getAPI() instanceof LocalArtefactAPI) ? "uw" : selectedVersionRepo) + "_";
        try {
            tempFile = new File(filePrefix + excelArtefact.getName());
            out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
        return tempFile;
    }

    private String removeProjectName(String path) {
        // remove project name
        path = path.substring(path.indexOf(SEPARATOR, 1) + 1);
        return path;
    }

    private static final char SEPARATOR = '/';

    private AProjectArtefact getExcelArtefactByPath(List<AProjectArtefact> excelArtefacts, String path) {
        for (AProjectArtefact excelArtefact : excelArtefacts) {
            if (excelArtefact.getArtefactPath().getStringValue().equals(path)) {
                return excelArtefact;
            }
        }
        return null;
    }

    public String compare() {
        if (StringUtils.isEmpty(selectedExcelFileUW) || StringUtils.isEmpty(selectedExcelFileRepo)) {
            System.err.println("exit");
            return null;
        }
        AProjectArtefact excelArtefact1 = getExcelArtefactByPath(excelArtefactsUW, selectedExcelFileUW);
        File excelFile1 = downloadExelFile(excelArtefact1);

        AProjectArtefact excelArtefact2 = getExcelArtefactByPath(excelArtefactsRepo, selectedExcelFileRepo);
        File excelFile2 = downloadExelFile(excelArtefact2);

        try {
            // The Diff Tree can be huge. As far as we don't need the
            // previous instance anymore, we should clear it before any
            // further calculations.
            setDiffTree(null);
            XlsDiff2 x = new XlsDiff2();
            DiffTreeNode diffTree = x.diffFiles(excelFile1.getAbsolutePath(), excelFile2.getAbsolutePath());
            setDiffTree(diffTree);
        } finally {
            excelFile1.delete();
            excelFile2.delete();
        }

        return null;
    }

}
