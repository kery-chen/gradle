/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization;

import com.google.common.collect.ImmutableSortedSet;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.CallableBuildOperation;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.util.CollectionUtils;

import java.util.Comparator;
import java.util.Set;

public class NotifyingBuildLoader implements BuildLoader {
    private final BuildLoader buildLoader;
    private final BuildOperationExecutor buildOperationExecutor;

    public NotifyingBuildLoader(BuildLoader buildLoader, BuildOperationExecutor buildOperationExecutor) {
        this.buildLoader = buildLoader;
        this.buildOperationExecutor = buildOperationExecutor;
    }

    @Override
    public void load(final ProjectDescriptor rootProjectDescriptor, final ProjectDescriptor defaultProject, final GradleInternal gradle, final ClassLoaderScope classLoaderScope) {
        try {
            buildOperationExecutor.call(new CallableBuildOperation<Void>() {
                @Override
                public BuildOperationDescriptor.Builder description() {
                    return BuildOperationDescriptor.displayName("Loading Build").
                        progressDisplayName("Loading Build").
                        details(new BuildStructureOperationDetails());
                }

                @Override
                public Void call(BuildOperationContext context) {
                    buildLoader.load(rootProjectDescriptor, defaultProject, gradle, classLoaderScope);
                    context.setResult(createOperationResult(gradle));
                    return null;
                }
            });
        } finally {
            gradle.getBuildListenerBroadcaster().projectsLoaded(gradle);
        }
    }

    private BuildStructureOperationResult createOperationResult(GradleInternal gradle) {
        String buildPath = gradle.getIdentityPath().toString();
        LoadBuildStructureBuildOperationType.Result.Project rootProject = convert(gradle.getRootProject());
        return new BuildStructureOperationResult(rootProject, buildPath);
    }

    private LoadBuildStructureBuildOperationType.Result.Project convert(org.gradle.api.Project project) {
        return new BuildStructureOperationProject(
            project.getName(),
            project.getPath(),
            ((ProjectInternal) project).getIdentityPath().toString(),
            project.getProjectDir().getAbsolutePath(),
            project.getBuildFile().getAbsolutePath(),
            convert(project.getSubprojects()));
    }

    private Set<LoadBuildStructureBuildOperationType.Result.Project> convert(Set<org.gradle.api.Project> children) {
        Set<LoadBuildStructureBuildOperationType.Result.Project> childProjects = CollectionUtils.collect(children, new Transformer<LoadBuildStructureBuildOperationType.Result.Project, Project>() {
            @Override
            public LoadBuildStructureBuildOperationType.Result.Project transform(Project project) {
                return convert(project);
            }
        });
        return ImmutableSortedSet.copyOf(PROJECT_DESCRIPTION_COMPARATOR, childProjects);
    }

    private static class BuildStructureOperationDetails implements LoadBuildStructureBuildOperationType.Details {
    }

    private static class BuildStructureOperationResult implements LoadBuildStructureBuildOperationType.Result {
        private final Project rootProject;
        private final String buildPath;

        public BuildStructureOperationResult(Project rootProject, String buildPath) {
            this.rootProject = rootProject;
            this.buildPath = buildPath;
        }

        @Override
        public Project getRootProject() {
            return rootProject;
        }

        @Override
        public String getBuildPath() {
            return buildPath;
        }
    }

    private static class BuildStructureOperationProject implements LoadBuildStructureBuildOperationType.Result.Project {
        private final String name;
        private final String path;
        private final String identityPath;
        private final String projectDir;
        private final String buildFile;
        final Set<LoadBuildStructureBuildOperationType.Result.Project> children;

        public BuildStructureOperationProject(String name, String path, String identityPath, String projectDir, String buildFile, Set<LoadBuildStructureBuildOperationType.Result.Project> children){
            this.name = name;
            this.path = path;
            this.identityPath = identityPath;
            this.projectDir = projectDir;
            this.buildFile = buildFile;
            this.children = children;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getIdentityPath() {
            return identityPath;
        }

        @Override
        public String getProjectDir() {
            return projectDir;
        }

        @Override
        public String getBuildFile() {
            return buildFile;
        }

        @Override
        public Set<LoadBuildStructureBuildOperationType.Result.Project> getChildren() {
            return children;
        }
    }

    private static final Comparator<LoadBuildStructureBuildOperationType.Result.Project> PROJECT_DESCRIPTION_COMPARATOR = new Comparator<LoadBuildStructureBuildOperationType.Result.Project>() {
        @Override
        public int compare(LoadBuildStructureBuildOperationType.Result.Project o1, LoadBuildStructureBuildOperationType.Result.Project o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

}