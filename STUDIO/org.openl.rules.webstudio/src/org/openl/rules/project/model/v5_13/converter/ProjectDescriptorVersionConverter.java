package org.openl.rules.project.model.v5_13.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_12.converter.ProjectDependencyDescriptorVersionConverter;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.v5_13.Module_v5_13;
import org.openl.rules.project.model.v5_13.ProjectDescriptor_v5_13;

/**
 * @author nsamatov.
 */
public class ProjectDescriptorVersionConverter
        implements ObjectVersionConverter<ProjectDescriptor, ProjectDescriptor_v5_13> {
    private final ModuleVersionConverter moduleVersionConverter = new ModuleVersionConverter();
    private final ProjectDependencyDescriptorVersionConverter dependencyConverter = new ProjectDependencyDescriptorVersionConverter();

    @Override
    public ProjectDescriptor fromOldVersion(ProjectDescriptor_v5_13 oldVersion) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(oldVersion.getName());
        descriptor.setComment(oldVersion.getComment());
        descriptor.setClasspath(oldVersion.getClasspath());

        List<Module> modules = new ArrayList<Module>(
                CollectionUtils.collect(oldVersion.getModules(),
                        new Transformer<Module_v5_13, Module>() {
                            @Override public Module transform(Module_v5_13 input) {
                                return moduleVersionConverter.fromOldVersion(input);
                            }
                        })
        );
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor> dependencies = new ArrayList<ProjectDependencyDescriptor>(
                CollectionUtils.collect(oldVersion.getDependencies(),
                        new Transformer<ProjectDependencyDescriptor_v5_12, ProjectDependencyDescriptor>() {
                            @Override public ProjectDependencyDescriptor transform(ProjectDependencyDescriptor_v5_12 input) {
                                return dependencyConverter.fromOldVersion(input);
                            }
                        })
        );
        if (!dependencies.isEmpty()) {
            descriptor.setDependencies(dependencies);
        }

        descriptor.setPropertiesFileNamePattern(oldVersion.getPropertiesFileNamePattern());
        descriptor.setPropertiesFileNameProcessor(oldVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }

    @Override
    public ProjectDescriptor_v5_13 toOldVersion(ProjectDescriptor currentVersion) {
        ProjectDescriptor_v5_13 descriptor = new ProjectDescriptor_v5_13();

        descriptor.setName(currentVersion.getName());
        descriptor.setComment(currentVersion.getComment());
        descriptor.setClasspath(currentVersion.getClasspath());

        List<Module_v5_13> modules = new ArrayList<Module_v5_13>(
                CollectionUtils.collect(currentVersion.getModules(),
                        new Transformer<Module, Module_v5_13>() {
                            @Override public Module_v5_13 transform(Module input) {
                                return moduleVersionConverter.toOldVersion(input);
                            }
                        })
        );
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor_v5_12> dependencies = new ArrayList<ProjectDependencyDescriptor_v5_12>(
                CollectionUtils.collect(currentVersion.getDependencies(),
                        new Transformer<ProjectDependencyDescriptor, ProjectDependencyDescriptor_v5_12>() {
                            @Override public ProjectDependencyDescriptor_v5_12 transform(ProjectDependencyDescriptor input) {
                                return dependencyConverter.toOldVersion(input);
                            }
                        })
        );
        if (!dependencies.isEmpty()) {
            descriptor.setDependencies(dependencies);
        }

        descriptor.setPropertiesFileNamePattern(currentVersion.getPropertiesFileNamePattern());
        descriptor.setPropertiesFileNameProcessor(currentVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }
}
