package io.micrc.core.gradle.plugin.adapter;

import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ApidocGenerationTask {
    private static final String SRC_MAIN_RESOURCES_APIDOC =
            File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "apidoc";

    private static final String MICRC_SCHEMA_AGGREGATIONS =
            File.separator + "micrc" + File.separator + "schema" + File.separator + "aggregations";

    private static final String PROTOCOL_REST = File.separator + "protocol" + File.separator + "rest";

    private static ApidocGenerationTask instance;

    private ApidocGenerationTask() {
    }

    public static ApidocGenerationTask newInstance() {
        if (instance == null) {
            instance = new ApidocGenerationTask();
        }
        return instance;
    }

    public void mergeOpenapi(Project project) {
        try {
            String apiDocPath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_APIDOC;
            TemplateUtils.clearDir(Path.of(apiDocPath));
            Files.createDirectories(Path.of(apiDocPath));
            Path aggregationsPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS);
            if (!Files.exists(aggregationsPath)) {
                log.warn("Unable to find aggregation path: "
                        + project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS
                        + ", skip openapi merge, apidoc generation. "
                );
                return;
            }
            TemplateUtils.listFile(aggregationsPath).forEach(path -> {
                String modelContent = TemplateUtils.readFile(Paths.get(path.toString(), "model.json"));
                OpenAPI modelAPI = SwaggerUtil.readOpenApi(modelContent);
                // 获取rest协议并合并
                AtomicReference<OpenAPI> baseAPIReference = new AtomicReference<>();
                AtomicReference<String> result = new AtomicReference<>();
                Path restDir = Paths.get(path + PROTOCOL_REST);
                TemplateUtils.listFile(restDir).forEach(protocolPath -> {
                    String protocolContent = TemplateUtils.readFile(protocolPath);
                    OpenAPI protocolAPI = SwaggerUtil.readOpenApi(protocolContent);
                    OpenAPI baseAPI = baseAPIReference.get();
                    if (null == baseAPI) {
                        protocolAPI.getInfo().setTitle(modelAPI.getInfo().getTitle());
                        protocolAPI.getComponents().setSchemas(modelAPI.getComponents().getSchemas());
                        baseAPIReference.set(protocolAPI);
                        result.set(JsonUtil.writeValueAsString(protocolAPI));
                        return;
                    }
                    protocolAPI.getPaths().keySet().forEach(name ->
                            baseAPI.getPaths().addPathItem(name, protocolAPI.getPaths().get(name))
                    );
                    result.set(JsonUtil.writeValueAsString(baseAPI));
                });
                // 合并结果写入文件
                TemplateUtils.saveStringToFile(apiDocPath + File.separator + path.getFileName() + ".json", result.get());
            });
            log.info("openapi merged, apidoc generation complete.");
        } catch (Exception e) {
            log.error("openapi merge error. ", e);
        }
    }

    public void mockMsgOpenapi(Project project) {
        // 运行时工具完成
    }
}
