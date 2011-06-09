package org.openl.ruleservice.loader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

public class LocalTemporaryProjectsStorageTest {
    //@Test
    public void testLoadDeloyment() {
        IDataSource jcrDataSource = new JcrDataSource();
        List<Deployment> deployments = jcrDataSource.getDeployments();
        assertTrue(deployments.size() > 0);
        Deployment deployment = deployments.get(0);
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.removeDeployment(deployment.getDeploymentName(), deployment.getCommonVersion());
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.clear();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }
}