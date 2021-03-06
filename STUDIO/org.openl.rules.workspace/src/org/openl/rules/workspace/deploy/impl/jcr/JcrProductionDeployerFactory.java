package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerFactory;

public class JcrProductionDeployerFactory implements ProductionDeployerFactory {
    private ProductionRepositoryFactoryProxy repositoryFactoryProxy;

    @Override
    public ProductionDeployer getDeployerInstance(String repositoryConfigName, boolean deploymentFormatOld) {
        return new JcrProductionDeployer(repositoryFactoryProxy, repositoryConfigName, deploymentFormatOld);
    }

    public void setRepositoryFactoryProxy(ProductionRepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

}
