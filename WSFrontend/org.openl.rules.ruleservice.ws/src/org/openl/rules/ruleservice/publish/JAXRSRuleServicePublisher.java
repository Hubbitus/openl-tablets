package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.logging.CollectOpenLServiceIntercepror;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSInterfaceEnhancerHelper;
import org.openl.rules.ruleservice.servlet.AvailableServicesGroup;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * DeploymentAdmin to expose services via HTTP using JAXRS.
 *
 * @author Nail Samatov, Marat Kamalov
 */
public class JAXRSRuleServicePublisher extends AbstractRuleServicePublisher implements AvailableServicesGroup {
    public static final String REST_PREFIX = "REST/";

    private final Logger log = LoggerFactory.getLogger(JAXRSRuleServicePublisher.class);

    private ObjectFactory<? extends JAXRSServerFactoryBean> serverFactory;
    private Map<OpenLService, Server> runningServices = new HashMap<OpenLService, Server>();
    private String baseAddress;
    private List<ServiceInfo> availableServices = new ArrayList<ServiceInfo>();
    private boolean loggingStoreEnable = false;
    private ObjectFactory<? extends Feature> storeLoggingFeatureFactoryBean;

    public ObjectFactory<? extends Feature> getStoreLoggingFeatureFactoryBean() {
        return storeLoggingFeatureFactoryBean;
    }

    public void setStoreLoggingFeatureFactoryBean(ObjectFactory<? extends Feature> storeLoggingFeatureFactoryBean) {
        this.storeLoggingFeatureFactoryBean = storeLoggingFeatureFactoryBean;
    }

    public void setLoggingStoreEnable(boolean loggingStoreEnable) {
        this.loggingStoreEnable = loggingStoreEnable;
    }

    public boolean isLoggingStoreEnable() {
        return loggingStoreEnable;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public ObjectFactory<?> getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ObjectFactory<? extends JAXRSServerFactoryBean> serverFactory) {
        this.serverFactory = serverFactory;
    }

    /* internal for test */JAXRSServerFactoryBean getServerFactoryBean() {
        if (serverFactory != null) {
            return serverFactory.getObject();
        }
        throw new IllegalArgumentException("serverFactory doesn't defined");
    }

    /* internal for test */Feature getStoreLoggingFeatureBean() {
        if (storeLoggingFeatureFactoryBean != null) {
            return storeLoggingFeatureFactoryBean.getObject();
        }
        throw new IllegalArgumentException("loggingInfoStoringService doesn't defined");
    }

    protected Class<?> enhanceServiceClassWithJAXRSAnnotations(Class<?> serviceClass,
            OpenLService service,
            String url) throws Exception {
        return JAXRSInterfaceEnhancerHelper.decorateInterface(serviceClass, service, true);
    }

    protected Object createWrappedBean(Object targetBean,
            OpenLService service,
            Class<?> proxyInterface,
            Class<?> targetInterface) throws Exception {
        return JAXRSInterfaceEnhancerHelper.decorateBean(targetBean, service, proxyInterface, targetInterface);
    }

    @Override
    protected void deployService(final OpenLService service) throws RuleServiceDeployException {
        if (service.getServiceClass().getMethods().length == 0){ //Skip deploy if service doesn't have methods.
            if (log.isWarnEnabled()){
                log.warn("Service \"{}\" with URL \"{}{}\" doens't have method and wasn't deployed.",
                    service.getName(),
                    getBaseAddress(),
                    service.getUrl());
            }
            return;
        }
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(service.getClassLoader());
        try {
            JAXRSServerFactoryBean svrFactory = getServerFactoryBean();
            String url = URLHelper.processURL(service.getUrl());
            if (service.getPublishers().size() != 1) {
                url = getBaseAddress() + REST_PREFIX + url;
            } else {
                url = getBaseAddress() + url;
            }
            svrFactory.setAddress(url);
            if (isLoggingStoreEnable()){
                svrFactory.getFeatures().add(getStoreLoggingFeatureBean());
                svrFactory.getInInterceptors().add(new CollectOpenLServiceIntercepror(service));
            }
            Class<?> serviceClass = enhanceServiceClassWithJAXRSAnnotations(service.getServiceClass(), service, url);
            svrFactory.setResourceClasses(serviceClass);
            Object target = createWrappedBean(service.getServiceBean(), service, serviceClass, service.getServiceClass());
            svrFactory.setResourceProvider(serviceClass, new SingletonResourceProvider(target));
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                Server wsServer = svrFactory.create();
                runningServices.put(service, wsServer);
                availableServices.add(createServiceInfo(service));
                log.info("Service \"{}\" with URL \"{}{}\" succesfully deployed.",
                    service.getName(),
                    getBaseAddress(),
                    service.getUrl());
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Throwable t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service \"%s\"", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public Collection<OpenLService> getServices() {
        return new ArrayList<OpenLService>(runningServices.keySet());
    }

    public OpenLService getServiceByName(String name) {
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    @Override
    protected void undeployService(String serviceName) throws RuleServiceUndeployException {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name \"%s\"", serviceName));
        }
        try {
            runningServices.get(service).destroy();
            log.info("Service \"{}\" with URL \"{}{}\" succesfully undeployed.",
                serviceName,
                baseAddress,
                service.getUrl());
            runningServices.remove(service);
            removeServiceInfo(serviceName);
            service.destroy();
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service \"%s\"", serviceName), t);
        }
    }

    @Override
    public String getGroupName() {
        return "RESTful";
    }

    @Override
    public List<ServiceInfo> getAvailableServices() {
        List<ServiceInfo> services = new ArrayList<ServiceInfo>(availableServices);
        Collections.sort(services, new Comparator<ServiceInfo>() {
            @Override
            public int compare(ServiceInfo o1, ServiceInfo o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return services;
    }

    private ServiceInfo createServiceInfo(OpenLService service) {
        List<String> methodNames = new ArrayList<String>();
        for (Method method : service.getServiceClass().getMethods()) {
            methodNames.add(method.getName());
        }
        Collections.sort(methodNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        String url = URLHelper.processURL(service.getUrl());
        url = url + "?_wadl";
        if (service.getPublishers().size() != 1) {
            url = REST_PREFIX + url;
        }
        return new ServiceInfo(new Date(), service.getName(), methodNames, url, "WADL");
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }

    private void removeServiceInfo(String serviceName) {
        for (Iterator<ServiceInfo> iterator = availableServices.iterator(); iterator.hasNext();) {
            ServiceInfo serviceInfo = iterator.next();
            if (serviceInfo.getName().equals(serviceName)) {
                iterator.remove();
                break;
            }
        }
    }
}
