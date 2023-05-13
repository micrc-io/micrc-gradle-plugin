package io.micrc.core.gradle.plugin.adapter.outgoing;

import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ProtocolOutgoingGenerationTask {
    private static ProtocolOutgoingGenerationTask instance;

    private static final OpenAPIParser PARSER = new OpenAPIParser();

    private static final ParseOptions OPTIONS = new ParseOptions();

    static {
        OPTIONS.setResolveFully(true); // 替换$ref
    }

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
            String resourceAggrPath = project.getProjectDir().getAbsolutePath() + MicrcCompilationConstants.SRC_MAIN_RESOURCES_AGGREGATIONS;
            TemplateUtils.listFile(Paths.get(resourceAggrPath)).forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    return;
                }
                Path restDir = Paths.get(path + MicrcCompilationConstants.PROTOCOL_REST);
                TemplateUtils.listFile(restDir).forEach(protocolPath -> {
                    String protocolContent = TemplateUtils.readFile(protocolPath);
                    OpenAPI protocolAPI = PARSER.readContents(protocolContent, null, OPTIONS).getOpenAPI();
                    TemplateUtils.saveStringToFile(protocolPath.toString(), JsonUtil.writeValueAsString(protocolAPI));
                });
            });
            System.out.println("还原REST协议完成");
        } catch (Exception e) {
            log.error("还原REST协议失败");
        }
    }
}
