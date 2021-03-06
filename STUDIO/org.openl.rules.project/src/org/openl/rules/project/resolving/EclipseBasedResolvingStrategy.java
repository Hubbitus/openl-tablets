package org.openl.rules.project.resolving;

import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.StringTool;
import org.openl.util.tree.FileTreeIterator;
import org.openl.util.tree.FileTreeIterator.FileTreeAdaptor;
import org.openl.util.tree.TreeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Resolves projects that match default OpenL Eclipse-based convention:
 * 1."openlbuilder" specified in ".project" file 2. Existing java class wrapper
 * <p/>
 * Do not use this strategy!!!
 *
 * @author PUdalau
 */
@Deprecated
public class EclipseBasedResolvingStrategy implements ResolvingStrategy {

    private static final String PROJECT_FILE = ".project";
    private static final String TEXT_TO_SEARCH = "openlbuilder";

    private final Logger log = LoggerFactory.getLogger(EclipseBasedResolvingStrategy.class);

    /**
     * {@link FileTreeAdaptor} that have to be used for file search inside the
     * project to determine which files/folders have to be used in search.
     */
    private FileTreeAdaptor treeAdaptor;

    public FileTreeAdaptor getTreeAdaptor() {
        if (treeAdaptor == null) {
            treeAdaptor = new FileTreeAdaptor();
        }
        return treeAdaptor;
    }

    public void setTreeAdaptor(FileTreeAdaptor treeAdaptor) {
        this.treeAdaptor = treeAdaptor;
    }

    public boolean isRulesProject(File folder) {
        try {
            if (!folder.exists() || !folder.isDirectory()) {
                log.debug("Eclipse based project strategy failed to resolve project folder: folder {} doesn`t exists of is not a directory",
                    folder.getPath());
                return false;
            }
            if (!containsFile(folder, PROJECT_FILE, false)) {
                log.debug("Eclipse based project strategy failed to resolve project folder {}: there is no file {} in the folder",
                    folder.getPath(),
                    PROJECT_FILE);
                return false;
            }
            if (!containsFileText(folder, PROJECT_FILE, TEXT_TO_SEARCH)) {
                log.debug("Eclipse based project strategy failed to resolve project folder {}: {} file doen`t contains {}",
                    folder.getPath(),
                    PROJECT_FILE,
                    TEXT_TO_SEARCH);
                return false;
            }
            if (listPotentialOpenLWrappersClassNames(folder).length == 0) {
                log.debug("Eclipse based project strategy failed to resolve project folder {}: there is no potential Openl wrappers",
                    folder.getPath());
                return false; // no modules.
            }
            log.debug("Project in {} folder was resolved as Eclipse based project", folder.getPath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean containsFile(File f, String fname, boolean isDir) throws IOException {
        File ff = new File(f.getCanonicalPath(), fname);
        return ff.exists() && ff.isDirectory() == isDir;

    }

    private boolean containsFileText(File dir, String fname, String content) throws IOException {
        File f = new File(dir.getCanonicalPath(), fname);

        FileReader fr = new FileReader(f);

        BufferedReader br = new BufferedReader(fr);

        try {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.indexOf(content) >= 0) {
                    return true;
                }
            }
            return false;

        } finally {
            br.close();
            fr.close();
        }
    }

    public String[] listPotentialOpenLWrappersClassNames(File project) throws IOException {

        List<String> list = new ArrayList<String>();

        String startDirs = System.getProperty(IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_PROPERTY,
            IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_DEFAULT);
        String wrapperSuffixes = System.getProperty(IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_PROPERTY,
            IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_DEFAULT);

        String[] srcRoots = StringTool.tokenize(startDirs, ", ");
        String[] suffixes = StringTool.tokenize(wrapperSuffixes, ", ");

        for (String srcRoot : srcRoots)
            listPotentialOpenLWrappersClassNames(project, srcRoot, suffixes, list);

        return list.toArray(new String[list.size()]);
    }

    private String javaClassName(File f, String srcroot) {

        String path = f.getPath();
        int inc = 1;
        if (srcroot.endsWith(File.separator)) {
            inc = 0;
        }

        String jpath = path.substring(srcroot.length() + inc, path.length() - 5);
        return jpath.replace(File.separatorChar, '.');

    }

    public void listPotentialOpenLWrappersClassNames(File project, String srcRoot, String[] suffixes, List<String> list) throws IOException {

        File searchDir = new File(project.getCanonicalPath(), srcRoot);
        TreeIterator<File> fti = new FileTreeIterator(searchDir, getTreeAdaptor(), 0);
        for (; fti.hasNext();) {
            File f = fti.next();
            for (String suffix : suffixes)
                if (f.getName().endsWith(suffix)) {
                    list.add(javaClassName(f, searchDir.getCanonicalPath()));
                }
        }

    }

    // TODO: Refactor.
    // listPotentialOpenLWrappersClassNames is called twice, first when calling
    // isRulesProject, then if yes,
    // call current method. Dont need to scan the file system twice.
    // authod DLiauchuk
    public ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(folder.getName());
        try {
            descriptor.setProjectFolder(folder.getCanonicalFile());
        } catch (IOException e) {
            throw new ProjectResolvingException(e);
        }
        String[] wrapperClassNames;
        try {
            wrapperClassNames = listPotentialOpenLWrappersClassNames(folder);
        } catch (IOException e) {
            wrapperClassNames = new String[] {};
        }
        List<Module> projectModules = new ArrayList<Module>();
        for (String className : wrapperClassNames) {
            projectModules.add(createModule(descriptor, className));
        }
        descriptor.setModules(projectModules);
        descriptor.setClasspath(getClassPath(folder.getAbsolutePath()));
        return descriptor;
    }

    protected Module createModule(ProjectDescriptor project, String className) {
        Module module = new Module();
        module.setProject(project);
        module.setClassname(className);
        module.setName(getModuleName(project.getProjectFolder(), className));
        module.setType(ModuleType.WRAPPER);
        return module;
    }

    public String getModuleName(File projectFolder, String wrapperClassName) {
        OpenLProjectPropertiesLoader propertiesLoader = new OpenLProjectPropertiesLoader();
        Properties p = propertiesLoader.loadProjectProperties(projectFolder.getAbsolutePath());
        if (p == null || !p.containsKey(wrapperClassName + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX)) {
            return wrapperClassName;
        }
        return p.getProperty(wrapperClassName + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, wrapperClassName);
    }

    private List<PathEntry> getClassPath(String projectFolder) {
        String classPath;

        OpenLProjectPropertiesLoader propertiesLoader = new OpenLProjectPropertiesLoader();
        String usedClassPath = propertiesLoader.loadExistingClasspath(projectFolder);

        String usedClasspathSeparator = propertiesLoader.loadExistingClasspathSeparator(projectFolder);
        if (usedClasspathSeparator != null) {
            classPath = usedClassPath.replace(usedClasspathSeparator, File.pathSeparator);
        } else {
            classPath = usedClassPath;
        }

        String[] files = StringTool.tokenize(classPath, File.pathSeparator);

        List<PathEntry> pathEntries = new ArrayList<PathEntry>(files.length);
        for (int i = 0; i < files.length; i++) {
            pathEntries.add(new PathEntry(files[i]));
        }
        return pathEntries;
    }

    @Override
    public void addInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InitializingModuleListener> getInitializingModuleListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllInitializingModuleListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeInitializingModuleListener(InitializingModuleListener initializingModuleListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InitializingProjectListener> getInitializingProjectListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllInitializingProjectListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeInitializingProjectListener(InitializingProjectListener initializingProjectListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInitializingModuleListeners(List<InitializingModuleListener> initializingModuleListeners) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInitializingProjectListeners(List<InitializingProjectListener> initializingProjectListeners) {
        throw new UnsupportedOperationException();
    }
}
