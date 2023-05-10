package io.micrc.core.gradle.plugin.adapter;

import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
public class ApidocGenerationTask {
    private static ApidocGenerationTask instance;

    private static final OpenAPIParser PARSER = new OpenAPIParser();

    private static final ParseOptions OPTIONS = new ParseOptions();

    private ApidocGenerationTask() {
    }

    public static ApidocGenerationTask newInstance() {
        if (instance == null) {
            instance = new ApidocGenerationTask();
        }
        return instance;
    }

    public void mergeOpenapi(Project project) {
        String apiDocPath = project.getProjectDir().getAbsolutePath() + File.separator + MicrcCompilationConstants.RESOURCE_DIR_PATH + File.separator + "apidoc";
        TemplateUtils.clearDir(Path.of(apiDocPath));
        String aggregationsPath = project.getBuildDir() + File.separator + "micrc" + File.separator + "schema" + File.separator + "aggregations";
        listFile(Paths.get(aggregationsPath)).forEach(path -> {
            File file = path.toFile();
            if (file.isFile()) {
                return;
            }
            // 获取聚合模型名称
            Path modelDir = Paths.get(path + File.separator + "model");
            Path modelPath = listFile(modelDir).findFirst().orElseThrow();
            Path fileName = modelPath.getFileName();
            String modelContent = readFile(modelPath);
            OpenAPI modelAPI = PARSER.readContents(modelContent, null, OPTIONS).getOpenAPI();
            String title = modelAPI.getInfo().getTitle();
            // 获取rest协议并合并
            AtomicReference<OpenAPI> baseAPIReference = new AtomicReference<>();
            AtomicReference<String> result = new AtomicReference<>();
            Path restDir = Paths.get(path + File.separator + "protocol" + File.separator + "rest");
            listFile(restDir).forEach(protocolPath -> {
                String protocolContent = readFile(protocolPath);
                OpenAPI protocolAPI = PARSER.readContents(protocolContent, null, OPTIONS).getOpenAPI();
                OpenAPI baseAPI = baseAPIReference.get();
                if (null == baseAPI) {
                    baseAPIReference.set(protocolAPI);
                    return;
                }
                baseAPI.getInfo().setTitle(title);
                if (baseAPI.getComponents().getSchemas() == null) {
                    baseAPI.getComponents().setSchemas(new HashMap<>());
                }
                protocolAPI.getPaths().keySet().forEach(name -> {
                    baseAPI.getPaths().addPathItem(name, protocolAPI.getPaths().get(name));
                    if (protocolAPI.getComponents().getSchemas() == null) {
                        return;
                    }
                    protocolAPI.getComponents().getSchemas().forEach((key, schema) -> {
                        baseAPI.getComponents().addSchemas(key, schema);
                    });
                });
                result.set(JsonUtil.writeValueAsString(baseAPI));
            });
            // 合并结果写入文件
            String filePath = apiDocPath + File.separator + fileName;
            TemplateUtils.saveStringToFile(filePath, result.get());
        });
        System.out.println("合并openapi协议，生成apidoc完成");
    }

    private Stream<Path> listFile(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void mockMsgOpenapi(Project project) {
        // 由运行时工具完成
    }
}
