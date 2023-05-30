package io.micrc.core.gradle.plugin.adapter;

import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class ApidocGenerationTask {
    private static final String SRC_MAIN_RESOURCES_APIDOC =
            File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "apidoc";

    private static final String MICRC_SCHEMA = File.separator + "micrc" + File.separator + "schema";
    private static final String MICRC_SCHEMA_AGGREGATIONS = MICRC_SCHEMA + File.separator + "aggregations";
    private static final String MICRC_SCHEMA_CASES = MICRC_SCHEMA + File.separator + "cases";

    private static final String PROTOCOL = File.separator + "protocol";
    private static final String PROTOCOL_API = PROTOCOL + File.separator + "api";
    private static final String PROTOCOL_RPC_IN = PROTOCOL + File.separator + "rpc" + File.separator + "in";
    private static final String PROTOCOL_REST_IN = PROTOCOL + File.separator + "rest" + File.separator + "in";

    private static final HashMap<String, OpenAPI> API_HASH_MAP = new HashMap<>();
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
            Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
            if (!Files.exists(casesPath)) {
                log.warn("Unable to find case path: "
                        + project.getBuildDir() + MICRC_SCHEMA_CASES
                        + ", skip openapi merge, apidoc generation. "
                );
                return;
            }
            TemplateUtils.listFile(aggregationsPath).forEach(path -> {
                String aggregationName = formatName(path.getFileName().toString());
                String modelContent = TemplateUtils.readFile(Paths.get(path.toString(), "model.json"));
                OpenAPI modelAPI = SwaggerUtil.readOpenApi(modelContent);
                API_HASH_MAP.put(aggregationName, modelAPI);
            });
            TemplateUtils.listFile(casesPath).forEach(path -> {
                String caseName = formatName(path.getFileName().toString());
                addAllPath2ModelAPI(caseName, Paths.get(path + PROTOCOL_API));
                addAllPath2ModelAPI(caseName, Paths.get(path + PROTOCOL_RPC_IN));
                addAllPath2ModelAPI(caseName, Paths.get(path + PROTOCOL_REST_IN));
            });
            API_HASH_MAP.forEach((aggregationName, modelAPI) -> {
                String result = JsonUtil.writeValueAsString(modelAPI);
                TemplateUtils.saveStringToFile(apiDocPath + File.separator + aggregationName + ".json", result);
            });
            log.info("openapi merged, apidoc generation complete.");
        } catch (Exception e) {
            log.error("openapi merge error. ", e);
        }
    }

    private void addAllPath2ModelAPI(String caseName, Path apiPath) {
        if (!Files.exists(apiPath)) {
            return;
        }
        TemplateUtils.listFile(apiPath).forEach(api -> {
            String content = TemplateUtils.readFile(api);
            OpenAPI currentAPI = SwaggerUtil.readOpenApi(content);
            currentAPI.getPaths().keySet().forEach(name -> {
                OpenAPI modelAPI = API_HASH_MAP.get(formatName(getAggregationName(currentAPI)));
                if (modelAPI == null) {
                    return;
                }
                modelAPI.getServers().clear();
                modelAPI.getServers().addAll(currentAPI.getServers());
                if (modelAPI.getPaths() == null) {
                    modelAPI.setPaths(new io.swagger.v3.oas.models.Paths());
                }
                PathItem pathItem = currentAPI.getPaths().get(name);
                if (pathItem.getPost().getTags() == null) {
                    pathItem.getPost().setTags(new ArrayList<>());
                }
                pathItem.getPost().getTags().add(caseName);
                modelAPI.getPaths().addPathItem(name, pathItem);
            });
        });
    }

    private String getAggregationName(OpenAPI currentAPI) {
        String[] split = currentAPI.getServers().get(0).getUrl().split("/");
        return split[split.length - 1];
    }

    private String formatName(String name) {
        if (name == null) {
            return null;
        }
        return name.replace("-", "").toUpperCase();
    }

    public void mockMsgOpenapi(Project project) {
        // 运行时工具完成
    }
}
