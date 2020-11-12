package de.dev.eth0.libgdx.tools

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

abstract class EnumCreator {

  /**
   * The logging implementation used when executed.
   * */
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  fun execute(
      targetPackage: String?,
      srcDirectory: String?,
      includeSubDirectories: Boolean,
      enumClassName: String?,
      config: KtxToolsPluginExtension
  ) {
    val generatedSourceDirectory = config.generatedSourceDirectory

    requireNotNull(targetPackage) {
      "Cannot create BundleLines if target package is not set. This can be set in the gradle build file"
    }
    val parentDirectory = srcDirectory?.let { File(it) }
    requireNotNull(parentDirectory) { "Failed to find asset directory." }

    val baseFiles = findFiles(parentDirectory, includeSubDirectories)
    val enumNamesToEnumValues = collectEnumValues(baseFiles, enumClassName)

    val outDir = File(
        "$generatedSourceDirectory${File.separator}${targetPackage.replace(".", File.separator)}"
    ).apply { mkdirs() }

    for ((enumName, entryNames) in enumNamesToEnumValues) {
      val outFile = File(outDir, "$enumName.kt")
      val sourceCode = generateKtFileContent(targetPackage, enumName, entryNames)
      outFile.writeText(sourceCode)
    }

    logger.info(
        "Created BundleLine enum class(es) for bundles in directory \"$parentDirectory\":\n" +
            enumNamesToEnumValues.keys.joinToString(separator = ",\n") { "  $it" } +
            "\nin package $targetPackage in source directory \"$generatedSourceDirectory\"."
    )
  }

  /**
   * Collects files that will be processed.
   *
   * @param parentDirectory The directory that will be searched.
   * @param includeSubDirectories Whether to include sub-directories of the parent directory in the search.
   * @return A list of all applicable properties files found.
   */
  protected open fun findFiles(
      parentDirectory: File,
      includeSubDirectories: Boolean
  ): Collection<File> {
    return Files.walk(parentDirectory.absoluteFile.toPath(), if (includeSubDirectories) Int.MAX_VALUE else 1)
      .filter { this.acceptsFile(it.fileName.toString()) }
      .map(Path::toFile)
      .toList()
  }

  /**
   * @return true if the given fileName can be processed
   */
  abstract fun acceptsFile(fileName: String): Boolean

  /**
   * Takes the target [inputFiles] and returns data for building enums from them. By default, if [commonEnumName]
   * is non-null, a single base name is output and all property keys will be merged into the single set. Otherwise, the
   * properties files' names are used as base names.
   * @param inputFiles All properties files that should be included
   * @param commonEnumName A name that should be used for a single enum that covers all input properties, or null to
   * indicate that each properties file should have its own base name.
   * @return A map of enum names to the enum entry names that should be members of those enums.
   * */
  protected open fun collectEnumValues(
      inputFiles: Collection<File>,
      commonEnumName: String?
  ): Map<String, Set<String>> {
    val outMap = mutableMapOf<String, MutableSet<String>>()
    val sanitizedCommonBaseName = commonEnumName?.let { convertToEnumName(it) }
    if (sanitizedCommonBaseName != commonEnumName)
      logger.warn("The provided enumClassName $commonEnumName was changed to $sanitizedCommonBaseName.")
    for (file in inputFiles) {
      val enumName = sanitizedCommonBaseName ?: convertToEnumName(file.nameWithoutExtension)
      val enumValues = processFile(file)
      outMap.getOrPut(enumName, ::mutableSetOf).addAll(enumValues)
    }
    return outMap
  }

  /**
   * Process the given file and return a map that contains all enum entries and their original name
   */
  abstract fun processFile(file: File): Set<String>

  /**
   * Converts the given name into an appropriate name for a Kotlin enum class. The
   * default behavior is to trim leading invalid characters (anything besides letters and underscores) and remove
   * remaining whitespace by converting to PascalCase.
   * @param name The input name
   * @return A name based on [name] that can be used as an enum class name.
   * */
  protected open fun convertToEnumName(name: String): String {
    return name.trimStart { !it.isLetter() && it != '_' }
      .split("\\s+".toRegex())
      .joinToString("", transform = String::capitalize)
      .also { require(it.isNotEmpty()) { "File name `$name` cannot be automatically converted to a valid enum name." } }
  }

  /**
   * Converts the name of a properties entry into a valid enum entry name.
   * @param name The input name, retrieved from a properties file.
   * @return A name based on [name] that is a valid enum entry name, or null if the input name cannot be used as a valid
   * enum entry.
   * */
  protected open fun convertToEntryName(name: String): String? {
    val entryName = name.trimStart { !it.isLetter() && it != '_' }
      .replace('.', '_')
      .split("\\s+".toRegex())
      .joinToString("", transform = String::toUpperCase)
      .toUpperCase()
    if (entryName.isEmpty()) {
      logger.warn("Entry name `$name` cannot be automatically converted and will be omitted.")
      return null
    }
    return entryName
  }

  /**
   * Generates a String representing the complete file content of a .kt file containing a `BundleLine` enum class. The
   * default implementation also adds suppression of improper enum entry names and a companion object property that can
   * be used to enable the `BundleLine` `nls` and `invoke` functions.
   * @param packageName The package the enum class should be a part of.
   * @param enumClassName The class name for the enum.
   * @param enumValues The values of all the enum's entries.
   * @return Source code for a complete .kt file.
   * */
  protected open fun generateKtFileContent(
      packageName: String,
      enumClassName: String,
      enumValues: Set<String>
  ): String {
    val entryNameToValue = enumValues.mapNotNull {
      val entryName = convertToEntryName(it)
      if (entryName != null) {
        entryName to it
      } else null
    }.toMap()

    val enumClassType = TypeSpec.enumBuilder(enumClassName)
      .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("value", String::class)
            .build()
      )
      .addProperty(PropertySpec.builder("value", String::class).initializer("value").build())

    for ((entryName, entryValue) in entryNameToValue) {
      enumClassType.addEnumConstant(
          entryName, TypeSpec.anonymousClassBuilder()
        .addSuperclassConstructorParameter("%S", entryValue)
        .build()
      )
    }
    generateKtFileContent(enumClassType, packageName, enumClassName)

    return FileSpec.builder(packageName, enumClassName).addType(enumClassType.build()).build().toString()
  }

  /**
   * Add custom content to the generated file
   */
  open fun generateKtFileContent(enumClassType: TypeSpec.Builder, packageName: String, enumClassName: String) {
    //Do nothing by default
  }
}

abstract class CreateEnumParams {
  /**
   * The package created enums will be placed in. Must be set, or the `createBundleLines` task cannot be used.
   * */
  var targetPackage: String? = null

  /** Directory that is searched for properties files. If null (the default), the first non-null and existing directory
   * in descending precedence of `android/assets/i18n`, `android/assets/nls`, `android/assets`, `core/assets/i18n`,
   * `core/assets/nls`, and `core/assets` is used.
   * */
  var srcDirectory: String? = null

  /**
   * Whether to search subdirectories of the parent directory. Default true.
   * */
  var includeSubDirectories: Boolean = true

  /**
   * The name of the generated enum class. If non-null, a single enum class is created for all bundles found. If null,
   * each unique base bundle's name will be used for a distinct enum class. Default null.
   * */
  var enumClassName: String? = null
}