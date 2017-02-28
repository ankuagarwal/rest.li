package com.linkedin.pegasus.gradle.tasks


import com.linkedin.pegasus.gradle.PegasusPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.JavaExecAction

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles


/**
 * Generate the data template source files from data schema files.
 *
 * To use this plugin, add these three lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * The plugin will scan the source set's pegasus directory, e.g. "src/main/pegasus"
 * for data schema (.pdsc) files.
 */
class GenerateDataTemplateTask extends DefaultTask
{
  /**
   * Directory to write the generated data template source files.
   */
  @OutputDirectory
  File destinationDir

  /**
   * Directory containing the data schema files.
   */
  @InputDirectory
  @SkipWhenEmpty
  File inputDir

  /**
   * The resolver path.
   */
  @InputFiles
  FileCollection resolverPath

  @InputFiles
  FileCollection codegenClasspath

  @TaskAction
  protected void generate()
  {
    final FileTree inputDataSchemaFiles = getSuffixedFiles(project, inputDir, PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES)
    final String[] inputDataSchemaFilenames = inputDataSchemaFiles.collect { it.path } as String[]
    if (inputDataSchemaFilenames.length == 0)
    {
      throw new StopExecutionException("There are no data schema input files. Skip generating data template.")
    }

    project.logger.info('Generating data templates ...')
    project.logger.lifecycle("There are ${inputDataSchemaFilenames.length} data schema input files. Using input root folder: ${inputDir}")
    destinationDir.mkdirs()

    final String resolverPathStr = (resolverPath + project.files(inputDir)).asPath

    project.javaexec { JavaExecAction it ->
      it.main = 'com.linkedin.pegasus.generator.PegasusDataTemplateGenerator'
      it.classpath codegenClasspath
      it.jvmArgs '-Dgenerator.resolver.path=' + resolverPathStr
      it.args destinationDir.path
      it.args inputDataSchemaFilenames
    }
  }
}