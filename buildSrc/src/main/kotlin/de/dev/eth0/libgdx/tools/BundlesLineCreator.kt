package de.dev.eth0.libgdx.tools

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import java.util.Properties

/**
 * Searches for properties files and generates Ktx BundleLine class files for them when executed. The companion object
 * instance can be used directly or behavior can be customized by subclassing.
 * */
open class BundleLinesCreator : EnumCreator() {

  companion object Default : BundleLinesCreator()

  internal fun execute(
      config: KtxToolsPluginExtension
  ) {
    with(config.createBundleLines) {
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
   * Accept all .properties files that do not contain an underscore
   */
  override fun acceptsFile(fileName: String): Boolean = fileName.endsWith(".properties") && '_' !in fileName

  override fun processFile(file: File): Set<String> {
    return Properties().run {
      load(file.inputStream())
      stringPropertyNames()
    }
  }

  override fun generateKtFileContent(enumClassType: TypeSpec.Builder, packageName: String, enumClassName: String) {
    enumClassType.addSuperinterface(ClassName("ktx.i18n", "BundleLine"))
    enumClassType.addProperty(
        PropertySpec.builder("bundle", ClassName("com.badlogic.gdx.utils", "I18NBundle"), KModifier.OVERRIDE)
          .getter(
              FunSpec.getterBuilder()
                .addStatement("return i18nBundle")
                .build()
          )
          .build()
    )
    enumClassType.addFunction(
        FunSpec.builder("toString")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return value")
          .returns(String::class)
          .build()
    )

    enumClassType.addType(
        TypeSpec.companionObjectBuilder()
          .addKdoc(
              CodeBlock.of(
                  "The bundle used for [BundleLine.nls] and [BundleLine.invoke] for this enum's values. Must\n " +
                      "be set explicitly before extracting the translated texts.\n"
              )
          )
          .addProperty(
              PropertySpec.builder("i18nBundle", ClassName("com.badlogic.gdx.utils", "I18NBundle"), KModifier.LATEINIT)
                .mutable(true)
                .build()
          )
          .build()
    )
  }
}

/**
 * Parameters for running [BundleLinesCreator], intended for the `createBundleLines` Gradle task.
 * */
class BundleLinesCreatorParams : CreateEnumParams() {

}
