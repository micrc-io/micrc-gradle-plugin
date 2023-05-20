package io.micrc.core.gradle.plugin.adapter.outgoing;

import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ProtocolOutgoingGenerationTask {
    private static final String SRC_MAIN_RESOURCES_AGGREGATIONS =
        File.separator + "src" + File.separator + "main" + File.separator + "resources"
        + File.separator + "aggregations";

    private static final String PROTOCOL_REST = File.separator + "protocol" + File.separator + "rest";

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
        // 逻辑问题: 为什么从src/main/resources/aggregations拷贝
        // rest目录下读入openapi未进行处理又写回是为了校验?
        // try {
        //     String resourceAggrPath =
        //         project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_AGGREGATIONS;
        //     TemplateUtils.listFile(Paths.get(resourceAggrPath)).forEach(path -> {
        //         File file = path.toFile();
        //         if (file.isFile()) {
        //             return;
        //         }
        //         Path restDir = Paths.get(path + PROTOCOL_REST);
        //         TemplateUtils.listFile(restDir).forEach(protocolPath -> {
        //             String protocolContent = TemplateUtils.readFile(protocolPath);
        //             OpenAPI protocolAPI = SwaggerUtil.readOpenApi(protocolContent);
        //             TemplateUtils.saveStringToFile(protocolPath.toString(), JsonUtil.writeValueAsString(protocolAPI));
        //         });
        //     });
        //     log.info("protocols of rpc outgoing copy complete. ");
        // } catch (Exception e) {
        //     log.error("protocols of rpc outgoing copy error. ", e);
        // }
    }
}
