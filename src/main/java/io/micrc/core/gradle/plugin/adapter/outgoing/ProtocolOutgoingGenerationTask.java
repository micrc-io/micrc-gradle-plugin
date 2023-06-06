package io.micrc.core.gradle.plugin.adapter.outgoing;

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
import java.util.HashMap;

@Slf4j
public class ProtocolOutgoingGenerationTask {

    public static final String SRC_MAIN_RESOURCES = File.separator + "src" + File.separator + "main" + File.separator + "resources";
    public static final String SRC_MAIN_RESOURCES_AGGREGATIONS = SRC_MAIN_RESOURCES + File.separator + "aggregations";
    public static final String SRC_MAIN_RESOURCES_CASES = SRC_MAIN_RESOURCES + File.separator + "cases";

    private static final String PROTOCOL = File.separator + "protocol";
    private static final String PROTOCOL_RPC_OUT = PROTOCOL + File.separator + "rpc" + File.separator + "out";
    private static final String PROTOCOL_REST_OUT = PROTOCOL + File.separator + "rest" + File.separator + "out";

//    private static final HashMap<String, OpenAPI> MODEL_HASH_MAP = new HashMap<>();

    /**
     * 使用swagger-parser合并的openapi内容会在以下问题，暂简单实现
     * 1.因为openapi模型定义问题，多个层级下出现exampleSetFlag，在运行时调用时出现：
     *      |WARN| Found unexpected data model property: exampleSetFlag
     * 2.所有x-开头的协议内容都会被extensions包装，失去原有协议结构，同样在运行时调用时出现：
     *      |WARN| Found unexpected data model property: extensions
     */
    private static final HashMap<String, String> MODEL_CONTENT_HASH_MAP = new HashMap<>();

    private static ProtocolOutgoingGenerationTask instance;

    private ProtocolOutgoingGenerationTask() {
    }

    public static ProtocolOutgoingGenerationTask newInstance() {
        if (instance == null) {
            instance = new ProtocolOutgoingGenerationTask();
        }
        return instance;
    }

    public void copyProtoOutgoing(Project project) {
        try {
            Path aggregationsPath = Paths.get(project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_AGGREGATIONS);
            if (!Files.exists(aggregationsPath)) {
                log.warn("Unable to find aggregation path: "
                        + project.getBuildDir() + SRC_MAIN_RESOURCES_AGGREGATIONS
                        + ", skip outgoing protocol fix. "
                );
                return;
            }
            Path casesPath = Paths.get(project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_CASES);
            if (!Files.exists(casesPath)) {
                log.warn("Unable to find case path: "
                        + project.getBuildDir() + SRC_MAIN_RESOURCES_CASES
                        + ", skip skip outgoing protocol fix. "
                );
                return;
            }
            TemplateUtils.listFile(aggregationsPath).forEach(path -> {
                String aggregationName = formatName(path.getFileName().toString());
                String modelContent = TemplateUtils.readFile(Paths.get(path.toString(), "model.json"));

//                MODEL_HASH_MAP.put(aggregationName, SwaggerUtil.readOpenApi(modelContent));
                MODEL_CONTENT_HASH_MAP.put(aggregationName, modelContent);

            });
            TemplateUtils.listFile(casesPath).forEach(path -> {
                copyModelSchemas2outAPI(Paths.get(path + PROTOCOL_RPC_OUT));
                copyModelSchemas2outAPI(Paths.get(path + PROTOCOL_REST_OUT));
            });
            log.info("outgoing protocol fix complete.");
        } catch (Exception e) {
            log.error("outgoing protocol fix error. ", e);
        }
    }

    private void copyModelSchemas2outAPI(Path apiPath) {
        if (!Files.exists(apiPath)) {
            return;
        }
        TemplateUtils.listFile(apiPath).forEach(api -> {
            String content = TemplateUtils.readFile(api);
            OpenAPI currentAPI = SwaggerUtil.readOpenApi(content);
            String aggregationName = formatName(getAggregationName(currentAPI));

//            MODEL_HASH_MAP.get(aggregationName).getComponents().getSchemas()
//                    .forEach((key, value) -> currentAPI.getComponents().addSchemas(key, value));
//            String result = JsonUtil.writeValueAsString(currentAPI);
            String modelContent = MODEL_CONTENT_HASH_MAP.get(aggregationName);
            Object schemas = JsonUtil.readPath(modelContent, "/components");
            String result = JsonUtil.patch(content, "/components", JsonUtil.writeValueAsString(schemas));

            TemplateUtils.saveStringToFile(api.toString(), result);
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
}
