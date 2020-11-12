package de.dev.eth0.libgdx.tools

import java.io.File

open class AssetEnumCreator : EnumCreator() {

  companion object Default : AssetEnumCreator()

  internal fun execute(
      config: KtxToolsPluginExtension
  ) {
    with(config.createAssetEnums) {
      execute(
          targetPackage = targetPackage,
          srcDirectory = srcDirectory,
          includeSubDirectories = includeSubDirectories,
          enumClassName = enumClassName,
          config = config
      )
    }
  }

  /**
   * Accept all .atlas files that do not contain an underscore
   */
  override fun acceptsFile(fileName: String): Boolean = fileName.endsWith(".atlas") && '_' !in fileName

  override fun processFile(file: File): Set<String> {
    //TODO: add support for page in atlas
    val entries = mutableSetOf<String>()
    var lastLine = ""
    file.forEachLine {
      if (it.trim().startsWith("rotate")) {
        entries.add(lastLine)
      }
      lastLine = it
    }
    return entries
  }
}

class AssetEnumCreatorParams : CreateEnumParams() {

}
