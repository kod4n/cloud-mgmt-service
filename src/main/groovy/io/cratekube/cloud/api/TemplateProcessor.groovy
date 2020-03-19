package io.cratekube.cloud.api

/**
 * Interface for parsing templates from files available on the classpath.
 */
interface TemplateProcessor {
  /**
   * Parses a file available on the classpath with the provided data.
   * It is recommended to provide a {@link Map} or strongly typed data structure for the {@code data}
   * parameter. Both parameters are required to be {@code non-null}/{@code non-empty}.
   *
   * <p>The {@code filePath} should be a relative path from the root of the classpath.  For example, if
   * you are trying to process a file in the resources directory at {@code src/main/resources/templates/example.hbs}
   * you would provide the string {@code templates/example} for the {@code filePath} param.</p>
   *
   * @param filePath {@code non-empty} path to file on the classpath
   * @param data {@code non-null} data to apply to the template
   * @return the parsed template after the data object has been applied
   */
  String parseFile(String filePath, Object data)
}
