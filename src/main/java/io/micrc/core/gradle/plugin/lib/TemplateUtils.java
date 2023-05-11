package io.micrc.core.gradle.plugin.lib;

import com.google.common.base.CaseFormat;
import groovy.text.SimpleTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class TemplateUtils {
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

    public static void main(String[] args) {
        // 变量小写连接线转小写驼峰
        System.out.println(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, "user-name"));//userName
        // 变量小写连接线转小写下划线
        System.out.println(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_UNDERSCORE, "user-name"));//user_name
        // 变量小写下划线转小写驼峰
        System.out.println(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "user_name"));//userName
        // 变量下划线转大写驼峰
        System.out.println(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, "user_name"));//UserName
        // 变量小写驼峰转大写驼峰
        System.out.println(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, "userName"));//UserName
        // 变量小写驼峰转小写下划线
        System.out.println(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, "userName"));//user_name
        // 变量小写驼峰转小写下划线
        System.out.println(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, "UserName"));//user_name
        // 变量小写驼峰转小写连接线
        System.out.println(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, "userName"));//user-name
    }

    public static void saveStringToFile(String filePath, String content) {
        byte[] sourceByte = content.getBytes(StandardCharsets.UTF_8);
        try {
            File file = new File(filePath);        //文件路径（路径+文件名）
            if (file.exists() && !file.isDirectory()) {
                file.deleteOnExit();
            }
            if (!file.exists()) {    //文件不存在则创建文件，先创建目录
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file);    //文件输出流用于将数据写入文件
            outStream.write(sourceByte);
            outStream.close();    //关闭文件输出流
        } catch (Exception e) {
            e.printStackTrace();
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
