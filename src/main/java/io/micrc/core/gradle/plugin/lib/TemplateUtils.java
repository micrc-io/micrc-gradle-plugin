package io.micrc.core.gradle.plugin.lib;

import groovy.text.SimpleTemplateEngine;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class TemplateUtils {
    private TemplateUtils() {}

    public static String generate(
            Map<String, String> context,
            String buildPath,
            List<String> sourcePaths,
            List<String> targetPaths) {
        String sourcePath = String.join(File.separator, sourcePaths);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourcePath);
        if (inputStream == null) {
            inputStream = TemplateUtils.class.getClassLoader().getResourceAsStream(sourcePath);
            if (inputStream == null) {
                throw new IllegalStateException("template file: " + sourcePath + " is not exists. ");
            }
        }
        try {
            String flatten = new SimpleTemplateEngine()
                    .createTemplate(IOUtils.toString(inputStream, StandardCharsets.UTF_8))
                    .make(context)
                    .toString();
            if (!targetPaths.isEmpty()) {
                String targetDirPath = String.join(File.separator, targetPaths.subList(0, targetPaths.size() - 1));
                Path dirPath = Paths.get(buildPath, targetDirPath);
                Files.createDirectories(dirPath);
                String targetPath = String.join(File.separator, targetPaths);
                Files.writeString(
                    Paths.get(buildPath, String.join(File.pathSeparator, targetPath)),
                    flatten,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return flatten;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void clearDir(Path path) {
        if (Files.notExists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("could not clean dir: " + path, e);
        }
    }

    public static void saveStringToFile(String filePath, String content) {
        try {
            Files.createDirectories(Path.of(filePath).getParent());
            Files.writeString(
                Paths.get(filePath),
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Stream<Path> listFile(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
