package io.micrc.core.gradle.plugin.manifests;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.text.SimpleTemplateEngine;
import io.micrc.core.gradle.plugin.MicrcCompilationExtension;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
public class SkaffoldConfigure {
    private static SkaffoldConfigure INSTANCE;

    private final String schemaPath;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DOMAIN_NAME_POINTER = "/domainName";

    private static final String DOMAIN_REGISTRY_POINTER = "/registry";

    private SkaffoldConfigure(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public static SkaffoldConfigure newInstance(String schemaPath) {
        if (INSTANCE == null) {
            INSTANCE = new SkaffoldConfigure(schemaPath);
        }
        return INSTANCE;
    }

    public void configure(Project project) {
        log.info("configure micrc project deployment manifests. ");
        Path schemaFile = Paths.get(schemaPath, MicrcCompilationExtension.DOMAIN_DIR_NAME + File.separator + MicrcCompilationExtension.DOMAIN_INFO_NAME);
        String domainInfo = null;
        if (schemaFile.toFile().exists()) {
            try {
                domainInfo = Files.readString(schemaFile);
            } catch (IOException e) {
                // leave it out
            }
        }
        if (domainInfo == null || domainInfo.isBlank()) {
            throw new IllegalStateException("could not obtain domain info from domain-info.json. fix domain schema and retry configure project.");
        }
        String domainName = null;
        String registry = null;
        try {
            domainName = MAPPER.readTree(domainInfo).at(DOMAIN_NAME_POINTER).asText();
            registry = MAPPER.readTree(domainInfo).at(DOMAIN_REGISTRY_POINTER).asText();
        } catch (IOException e) {
            throw new RuntimeException("could not resolve domain info, please check domain-info.json");
        }

        // TODO 读取schema中上下文信息，domain-info.json。获取子域名称(镜像repo使用：[registry]/[domain]/[context]:[tag])
        // 配置skaffold.xml文件，写入project根目录
        Map<String, String> properties = new HashMap<>();
        properties.put("name", project.getName());
        properties.put("domainName", domainName);
        properties.put("registry", registry);
        // skaffold.yaml
        List<String> skaffoldPaths = List.of("manifest", "skaffold.yaml");
        List<String> skaffoldTargetPaths = List.of("skaffold.yaml");
        generateManifest(properties, project.getProjectDir().getAbsolutePath(), skaffoldPaths, skaffoldTargetPaths);
    }
    private String appVersionToChartVersion(String appVersion) {
        String[] versions = appVersion.split("\\.");
        return String.format("%d.%d.%d", Integer.valueOf(versions[0]), Integer.valueOf(versions[1] + versions[2]), 0);
    }

    private void generateManifest(
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
