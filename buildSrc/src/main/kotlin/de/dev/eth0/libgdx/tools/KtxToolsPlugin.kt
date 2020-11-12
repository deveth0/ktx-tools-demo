package de.dev.eth0.libgdx.tools

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke

internal const val TASK_GROUP = "ktx"
internal const val PROJECT_EXTENSION = "ktx"
internal const val BUNDLE_LINES_CREATOR_TASK = "createBundleLines"
internal const val ASSET_CREATOR_TASK = "createAssetEnums"

internal const val PROJECT_DIR_PLACEHOLDER = "\$projectDir"

/**
 * The Gradle plugin that sets up all tasks of ktx-tools, placing them in the `ktx` group.
 */
class KtxToolsPlugin : Plugin<Project> {

  override fun apply(project: Project): Unit = project.run {
    val ktxToolsExtension = project.extensions
      .create(PROJECT_EXTENSION, KtxToolsPluginExtension::class.java)
      .apply {
        generatedSourceDirectory =
          generatedSourceDirectory.replace(PROJECT_DIR_PLACEHOLDER, project.projectDir.absolutePath)
      }

    tasks {
      register(BUNDLE_LINES_CREATOR_TASK) {
        group = TASK_GROUP
        doLast {
          BundleLinesCreator.execute(ktxToolsExtension)
        }
      }
      register(ASSET_CREATOR_TASK) {
        group = TASK_GROUP
        doLast {
          AssetEnumCreator.execute(ktxToolsExtension)
        }
      }
    }
  }
}

/**
 * Container for all `ktx` task parameters.
 */
open class KtxToolsPluginExtension {
  var generatedSourceDirectory: String = "\$projectDir/build/generated-sources"

  /**
   * Parameters for the `createBundleLines` task.
   * */
  var createBundleLines = BundleLinesCreatorParams()

  /**
   * Params for the `createAssetEnums` task
   */
  var createAssetEnums = AssetEnumCreatorParams()
}
