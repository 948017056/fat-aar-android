package com.kezong.fataar

import com.android.build.gradle.api.LibraryVariant
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.Artifact
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskDependencyFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.Describables
import org.gradle.internal.component.local.model.PublishArtifactLocalArtifactMetadata
import org.gradle.internal.component.model.DefaultIvyArtifactName
import org.gradle.internal.model.CalculatedValueContainerFactory


import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.tasks.TaskProvider

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.artifacts.result.ResolvedVariantResult
import java.util.Optional

/**
 * FlavorArtifact
 */
class FlavorArtifact {



    static ResolvedArtifactResult createFlavorArtifact(
            Project project,
            LibraryVariant variant,
            def unResolvedArtifact,  // 避免直接写类型，使用 def
            def calculatedValueContainerFactory,
            def fileResolver,
            def taskDependencyFactory
    ) {
        Project artifactProject = getArtifactProject(project, unResolvedArtifact)
        TaskProvider<?> bundleProvider
        try {
            bundleProvider = getBundleTask(artifactProject, variant)
        } catch (Exception ex) {
            FatUtils.logError("[$variant.name]Can not resolve :$unResolvedArtifact.moduleName", ex)
            return null
        }

        if (bundleProvider == null) {
            FatUtils.logError("[$variant.name]Can not resolve :$unResolvedArtifact.moduleName")
            return null
        }

        File artifactFile = createArtifactFile(bundleProvider.get())

        // 这里不使用具体类型，返回一个匿名类实现 ResolvedArtifactResult 接口
        return new ResolvedArtifactResult() {
            @Override
            File getFile() {
                return artifactFile
            }

            @Override
            ResolvedVariantResult getVariant() {
                // 简单匿名实现，或用null如果你不需要variant信息
                return createResolvedVariantResult(artifactFile,variant,unResolvedArtifact)
            }

            @Override
            ComponentArtifactIdentifier getId() {
                return createComponentArtifactIdentifier("artifact-for-${unResolvedArtifact.moduleName}")
            }

            @Override
            Class<? extends Artifact> getType() {
                // 返回Artifact的具体Class，可以根据实际情况返回
                return Artifact.class
            }
        }
    }


// 创建 ComponentArtifactIdentifier 的匿名实现
    ComponentArtifactIdentifier createComponentArtifactIdentifier(def unResolvedArtifact) {
        return new ComponentArtifactIdentifier() {

            @Override
            ComponentIdentifier getComponentIdentifier() {
                return new ComponentIdentifier() {
                    @Override
                    String getDisplayName() {
                        return "${unResolvedArtifact.moduleGroup}:${unResolvedArtifact.moduleName}:${unResolvedArtifact.moduleVersion}"
                    }
                }
            }

            @Override
            String getDisplayName() {
                return "artifact-${unResolvedArtifact.moduleName}-${unResolvedArtifact.moduleVersion}"
            }
        }
    }

    static ResolvedVariantResult createResolvedVariantResult(Project project, LibraryVariant variant, def unResolvedArtifact) {
        return new ResolvedVariantResult() {

            @Override
            ComponentIdentifier getOwner() {
                // 简单模拟ComponentIdentifier，返回匿名实现
                return new ComponentIdentifier() {
                    @Override
                    String getDisplayName() {
                        return "${unResolvedArtifact.moduleGroup}:${unResolvedArtifact.moduleName}:${unResolvedArtifact.moduleVersion}"
                    }
                }
            }

            @Override
            AttributeContainer getAttributes() {
                // 返回空属性容器或者根据variant构造
                return project.getObjects().newInstance(AttributeContainer)
                // 你也可以用 DefaultAttributeContainer 或其它实现，若需要具体属性可填充
            }

            @Override
            String getDisplayName() {
                return "ResolvedVariantResult for ${variant.name}"
            }

            @Override
            List<Capability> getCapabilities() {
                // 默认返回空列表
                return Collections.emptyList()
            }

            @Override
            Optional<ResolvedVariantResult> getExternalVariant() {
                // 默认无外部variant
                return Optional.empty()
            }
        }
    }


    private static ModuleVersionIdentifier createModuleVersionIdentifier(ResolvedDependency unResolvedArtifact) {
        return DefaultModuleVersionIdentifier.newId(
                unResolvedArtifact.getModuleGroup(),
                unResolvedArtifact.getModuleName(),
                unResolvedArtifact.getModuleVersion()
        )
    }

    private static DefaultIvyArtifactName createArtifactName(File artifactFile) {
        return new DefaultIvyArtifactName(artifactFile.getName(), "aar", "")
    }


    private static Project getArtifactProject(Project project, ResolvedDependency unResolvedArtifact) {
        for (Project p : project.getRootProject().getAllprojects()) {
            if (unResolvedArtifact.moduleName == p.name && unResolvedArtifact.moduleGroup == p.group.toString()) {
                return p
            }
        }
        return null
    }

    private static File createArtifactFile(Task bundle) {
        return new File(bundle.getDestinationDirectory().getAsFile().get(), bundle.getArchiveFileName().get())
    }

    private static TaskProvider getBundleTask(Project project, LibraryVariant variant) {
        TaskProvider bundleTaskProvider = null
        project.android.libraryVariants.find { subVariant ->
            // 1. find same flavor
            if (variant.name == subVariant.name) {
                try {
                    bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                    return true
                } catch (Exception ignore) {
                }
            }

            // 2. find buildType
            ProductFlavor flavor = variant.productFlavors.isEmpty() ? variant.mergedFlavor : variant.productFlavors.first()
            if (subVariant.name == variant.buildType.name) {
                try {
                    bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                    return true
                } catch (Exception ignore) {
                }
            }

            // 3. find missingStrategies
            try {
                flavor.missingDimensionStrategies.find { entry ->
                    String toDimension = entry.getKey()
                    List<String> toFlavors = [entry.getValue().requested] + entry.getValue().getFallbacks()
                    ProductFlavor subFlavor = subVariant.productFlavors.isEmpty() ?
                            subVariant.mergedFlavor : subVariant.productFlavors.first()
                    toFlavors.find { toFlavor ->
                        if (toDimension == subFlavor.dimension
                                && toFlavor == subFlavor.name
                                && variant.buildType.name == subVariant.buildType.name) {
                            try {
                                bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                                return true
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }
            } catch (Exception ignore) {

            }

            return bundleTaskProvider != null
        }

        return bundleTaskProvider
    }

}
