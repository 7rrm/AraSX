package org.telegram.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.telegram.tasks.GenerateLottieMetadataAssetFileTask
import org.telegram.tasks.GenerateStringResourceIdsAssetTask
import org.telegram.tasks.TelegramStringsTask

/**
 * Nagram 12.10.2+ application-side build pipeline.
 *
 * MeeroX adaptation: upstream imported the AGP-internal
 * `com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask` to force
 * `process<Variant>Resources` to depend on the strings task. MeeroX builds on AGP 9.3.1
 * where internal AGP classes are not a stable contract (buildSrc is compiled on every
 * build, so a broken import would break everything), therefore the dependency is wired
 * by task name instead - which is all the original code did anyway.
 *
 * Also note: in MeeroX `:TMessagesProj` *is* the application module (upstream keeps it a
 * library and puts the app shell in `:TMessagesProj_App`), so `telegramModule` resolves to
 * this same project and the file trees below simply collapse onto one another.
 */
class TelegramBuildAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val telegramModule = project.project(":TMessagesProj")
        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: error("Apply com.android.application/library before org.telegram.build-app-plugin")

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }

            val task = project.tasks.register<TelegramStringsTask>(
                "generate${suffix}TelegramStrings"
            ) {
                stringsXml.from(
                    telegramModule.fileTree("src/main/res/values") {
                        include("strings.xml")
                    },
                    project.fileTree("src/main/res/values") {
                        include("strings.xml")
                    }
                )

                localizationFiles.from(
                    telegramModule.fileTree("src/main/res") {
                        include("values-*/strings.xml")
                    },
                    project.fileTree("src/main/res") {
                        include("values-*/strings.xml")
                    }
                )

                stringsOutputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/telegramStrings/${variant.name}/res"
                    )
                )

                assetsOutputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/telegramStrings/${variant.name}/assets"
                    )
                )

                stableIdsFile.set(
                    project.layout.buildDirectory.file(
                        "generated/telegramStrings/${variant.name}/stable-ids.txt"
                    )
                )

                resourcePackageName.set((variant as ApplicationVariant).applicationId)
            }

            variant.sources.res?.addGeneratedSourceDirectory(
                task,
                TelegramStringsTask::stringsOutputDir
            )

            variant.sources.assets?.addGeneratedSourceDirectory(
                task,
                TelegramStringsTask::assetsOutputDir
            )

            (variant as ApplicationVariant).androidResources.aaptAdditionalParameters.addAll(
                task.flatMap { telegramStringsTask ->
                    telegramStringsTask.stableIdsFile.map { stableIdsFile ->
                        listOf(
                            "--stable-ids",
                            stableIdsFile.asFile.absolutePath
                        )
                    }
                }
            )

            // MeeroX adaptation: no AGP-internal class, resolved by task name.
            project.tasks.matching { it.name == "process${suffix}Resources" }.configureEach {
                dependsOn(task)
            }
        }

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }
            val task = project.tasks.register<GenerateStringResourceIdsAssetTask>("generate${suffix}StringResourceIdsAsset") {
                runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
                outputDir.set(project.layout.buildDirectory.dir("generated/stringResourceIds/${variant.name}/assets"))
            }
            variant.sources.assets?.addGeneratedSourceDirectory(task, GenerateStringResourceIdsAssetTask::outputDir)
        }

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }
            val lottieTask = project.tasks.register<GenerateLottieMetadataAssetFileTask>("generate${suffix}LottieMeta") {
                runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
                variant.sources.res?.all?.let {
                    rawResourceDirs.from(
                        telegramModule.fileTree("src/main/res") {
                            include("raw*/*.json")
                        },
                        project.fileTree("src/main/res") {
                            include("raw*/*.json")
                        }
                    )
                }
            }

            variant.sources.assets?.addGeneratedSourceDirectory(lottieTask, GenerateLottieMetadataAssetFileTask::outputDir)
        }
    }
}
