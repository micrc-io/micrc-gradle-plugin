package io.micrc.core.gradle.plugin;

import groovy.text.SimpleTemplateEngine;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class TemplateUtils {
    /**
     * 生成模版
     *
     * @param context
     * @param buildPath
     * @param sourcePaths
     * @param targetPaths
     */
    public static void generate(
            Map<String, String> context,
            String buildPath,
            List<String> sourcePaths,
            List<String> targetPaths) {
        String sourcePath = String.join(File.separator, sourcePaths);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("template file: " + sourcePath + " is not exists. ");
        }
        try {
            String flatten = new SimpleTemplateEngine()
                    .createTemplate(IOUtils.toString(inputStream, StandardCharsets.UTF_8))
                    .make(context)
                    .toString();
            String targetDirPath = String.join(File.separator, targetPaths.subList(0, targetPaths.size() - 1));
            Path dirPath = Paths.get(buildPath, targetDirPath);
            Files.createDirectories(dirPath);
            String targetPath = String.join(File.separator, targetPaths);
            Files.writeString(
                    Paths.get(buildPath, String.join(File.pathSeparator, targetPath)),
                    flatten,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
