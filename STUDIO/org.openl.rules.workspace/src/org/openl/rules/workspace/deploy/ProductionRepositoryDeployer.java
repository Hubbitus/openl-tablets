package org.openl.rules.workspace.deploy;

import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class allows to deploy a zip-based project to a production repository.
 * By default configuration of destination repository is get from "deployer.properties" file.
 *
 * @author Yury Molchan
 */
public class ProductionRepositoryDeployer {
    // Some user name for JCR
    private static WorkspaceUser user = new WorkspaceUserImpl("OpenL_Deployer");
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryDeployer.class);

    /**
     * Deploys a new project to the production repository. If the project exists then it will be skipped to deploy.
     *
     * @param zipFile the project to deploy
     * @param config  the configuration file name
     * @throws Exception
     */
    public void deploy(File zipFile, String config) throws Exception {
        deployInternal(zipFile, config, true);
    }

    /**
     * Deploys a new or redeploys an existing project to the production repository.
     *
     * @param zipFile the project to deploy
     * @param config  the configuration file name
     * @throws Exception
     */
    public void redeploy(File zipFile, String config) throws Exception {
        deployInternal(zipFile, config, false);
    }

    private void deployInternal(File zipFile, String config, boolean skipExist) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }

        // Initialize repo
        ProductionRepositoryFactoryProxy repositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
        JcrProductionDeployer deployer = new JcrProductionDeployer(repositoryFactoryProxy, config);

        // Temp folders
        File tempDirectory = FileUtils.createTempDirectory();

        String name = FileUtils.getBaseName(zipFile.getName());

        File zipFolder = new File(tempDirectory, name);
        zipFolder.mkdirs();
        File workspaceLocation = new File(tempDirectory, "workspace");
        workspaceLocation.mkdirs();

        try {
            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            if (rules.exists()) {
                String rulesName = getProjectName(rules);
                if (rulesName != null && !rulesName.isEmpty()) {
                    // rename project
                    File renamed = new File(tempDirectory, rulesName);
                    zipFolder.renameTo(renamed);
                    zipFolder = renamed;
                }
            }

            // Create a deployment project
            ArtefactPathImpl path = new ArtefactPathImpl(name);
            LocalWorkspaceImpl workspace = new LocalWorkspaceImpl(user, workspaceLocation, null, null);
            LocalFolderAPI localFolderAPI = new LocalFolderAPI(zipFolder, path, workspace);
            localFolderAPI.setProps(new HashMap<String, Object>());
            ADeploymentProject project = new ADeploymentProject(localFolderAPI, user);

            // Calculate version
            RProductionRepository rRepository = repositoryFactoryProxy.getRepositoryInstance(config);
            // Wait 5 seconds for initializing networking in JGroups.
            Thread.sleep(5000);
            Collection<FolderAPI> lastDeploymentProjects = rRepository.getLastDeploymentProjects();
            int version = 0;
            for (FolderAPI folder : lastDeploymentProjects) {
                String deployed = folder.getName();
                String prefix = project.getName() + "#";
                if (deployed.startsWith(prefix)) {
                    String verStr = deployed.substring(prefix.length());
                    version = Integer.valueOf(verStr) + 1;
                    break;
                }
            }
            ((LocalFolderAPI) project.getAPI()).setCurrentVersion(new RepositoryProjectVersionImpl(version, null));

            if (version != 0 && skipExist) {
                log.info("Project [{}] exists. It has been skipped to deploy.", project.getName());
                return;
            }

            // Do deploy
            ArrayList<AProject> projects = new ArrayList<AProject>();
            projects.add(project);
            deployer.deploy(project, projects, user);
            // Wait 10 seconds for finalizing networking in JGroups.
            Thread.sleep(10000);
        } finally {
            /* Clean up */
            FileUtils.deleteQuietly(tempDirectory);
            // Close repo
            deployer.destroy();
        }
    }

    private String getProjectName(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
