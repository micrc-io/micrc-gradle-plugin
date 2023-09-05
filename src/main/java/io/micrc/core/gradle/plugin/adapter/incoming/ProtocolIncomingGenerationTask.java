package io.micrc.core.gradle.plugin.adapter.incoming;

import com.fasterxml.jackson.databind.JsonNode;
import groovy.util.Eval;
import io.micrc.core.gradle.plugin.domain.DomainGenerationTask;
import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProtocolIncomingGenerationTask {

    public static final String MICRC_SCHEMA = File.separator + "micrc" + File.separator + "schema";
    public static final String MICRC_SCHEMA_CASES = MICRC_SCHEMA + File.separator + "cases";

    private static final String PROTOCOL = File.separator + "protocol";
    private static final String PROTOCOL_API = PROTOCOL + File.separator + "api";
    private static final String PROTOCOL_RPC_IN = PROTOCOL + File.separator + "rpc" + File.separator + "in";
    private static ProtocolIncomingGenerationTask instance;
    private ProtocolIncomingGenerationTask() {
    }
    public static ProtocolIncomingGenerationTask newInstance() {
        if (instance == null) {
            instance = new ProtocolIncomingGenerationTask();
        }
        return instance;
    }

    public void generateBusinessAdapter(Project project) {
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        if (!Files.exists(casesPath)) {
            return;
        }
        TemplateUtils.listFile(casesPath).forEach(path -> {
            Path apiPath = Paths.get(path.toString(), PROTOCOL_API);
            if (!apiPath.toFile().exists()) {
                return;
            }
            TemplateUtils.listFile(apiPath).forEach(api -> {
                String s = api.getFileName().toString();
                if (!s.startsWith("BSLG")) {
                    return;
                }
                HashMap<String, Object> map = new HashMap<>();
                String[] split = api.toString().split(MICRC_SCHEMA + File.separator);
                map.put("protocolPath", split[1]);
                // basePackage
                String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
                map.put("basePackage", basePackage);
                // aggregationPackage
                OpenAPI openAPI = SwaggerUtil.readOpenApi(TemplateUtils.readFile(api));
                String aggregationCode = getAggregationCodeFromApi(openAPI);
                String aggregationName = DomainGenerationTask.AGGREGATION_NAME_MAP.get(aggregationCode);
                if (aggregationName == null) {
                    return;
                }
                String aggregationPackage = aggregationName.toLowerCase();
                map.put("aggregationPackage", aggregationPackage);
                // logic name
                String logic = openAPI.getInfo().getTitle();
                map.put("logic", logic);
                Map<String, Object> extensions = openAPI.getExtensions();
                if (extensions == null) {
                    return;
                }
                Object metadata = extensions.get("x-metadata");
                JsonNode metadataNode = JsonUtil.readTree(metadata);
                String portType = metadataNode.at("/portType").textValue();
                if (portType == null || "ADAPTER".equals(portType)) {
                    // custom
                    map.put("custom", metadataNode.at("/customAdapter").textValue());
                    // mapping
                    map.put("requestMappingFile", spliceMappingPath(metadataNode.at("/requestMappingFile").textValue(), aggregationCode));
                    map.put("responseMappingFile", spliceMappingPath(metadataNode.at("/responseMappingFile").textValue(), aggregationCode));
                    map.put("rootEntityName", aggregationName);
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/command/"
                            + aggregationPackage + "/" + logic + "Adapter.java";
                    FreemarkerUtil.generator("BusinessesAdapter", map, fileName);
                } else if ("LISTENER".equals(portType)) {
                    List<Object> listeners = JsonUtil.writeValueAsList(metadataNode.at("/listeners").toString(), Object.class);
                    listeners.forEach(listener -> {
                        String listenerJson = JsonUtil.writeValueAsString(listener);
                        // event
                        Object event = JsonUtil.readPath(listenerJson, "/event");
                        map.put("event", event);
                        // topic
                        map.put("topic", JsonUtil.readPath(listenerJson, "/topic"));
                        String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                                + basePackage.replace(".", "/") + "/infrastructure/message/"
                                + aggregationPackage + "/" + event + "Listener.java";
                        FreemarkerUtil.generator("BusinessesListener", map, fileName);
                    });
                } else if ("SCHEDULE".equals(portType)) {
                    // cron
                    String cron = metadataNode.at("/cron").textValue();
                    map.put("cron", cron);
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/schedule/"
                            + aggregationPackage + "/" + logic + "Schedule.java";
                    FreemarkerUtil.generator("BusinessesSchedule", map, fileName);
                } else if ("RUNNER".equals(portType)) {
                    // dataPath
                    map.put("dataPath", metadataNode.at("/dataPath").textValue());
                    // runnerOrder
                    map.put("runnerOrder", metadataNode.at("/runnerOrder").textValue());
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/runner/"
                            + aggregationPackage + "/" + logic + "Runner.java";
                    FreemarkerUtil.generator("BusinessesRunner", map, fileName);
                }
            });
        });
    }

    private String spliceMappingPath(String fileName, String aggregationCode) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        return "aggregations/" + aggregationCode + "/mapping/" + fileName;
    }

    private static String getAggregationCodeFromApi(OpenAPI openAPI) {
        String[] urlSplit = openAPI.getServers().get(0).getUrl().split("/");
        String aggregationCode = urlSplit[urlSplit.length - 1].replace("-", "").toUpperCase();
        return aggregationCode;
    }

    public void generatePresentationAdapter(Project project) {
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        if (!Files.exists(casesPath)) {
            return;
        }
        TemplateUtils.listFile(casesPath).forEach(path -> {
            Path apiPath = Paths.get(path.toString(), PROTOCOL_API);
            if (!apiPath.toFile().exists()) {
                return;
            }
            TemplateUtils.listFile(apiPath).forEach(api -> {
                String s = api.getFileName().toString();
                if (!s.startsWith("INVE")) {
                    return;
                }
                HashMap<String, Object> map = new HashMap<>();
                String[] split = api.toString().split(MICRC_SCHEMA + File.separator);
                map.put("protocolPath", split[1]);
                // basePackage
                String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
                map.put("basePackage", basePackage);
                // aggregationPackage
                OpenAPI openAPI = SwaggerUtil.readOpenApi(TemplateUtils.readFile(api));
                String aggregationCode = getAggregationCodeFromApi(openAPI);
                String aggregationName = DomainGenerationTask.AGGREGATION_NAME_MAP.get(aggregationCode);
                if (aggregationName == null) {
                    return;
                }
                String aggregationPackage = aggregationName.toLowerCase();
                map.put("aggregationPackage", aggregationPackage);
                // logic name
                String logic = openAPI.getInfo().getTitle();
                map.put("logic", logic);
                Map<String, Object> extensions = openAPI.getExtensions();
                if (extensions == null) {
                    return;
                }
                Object metadata = extensions.get("x-metadata");
                JsonNode metadataNode = JsonUtil.readTree(metadata);
                // custom
                map.put("custom", metadataNode.at("/customAdapter").textValue());
                // mapping
                map.put("requestMappingFile", spliceMappingPath(metadataNode.at("/requestMappingFile").textValue(), aggregationCode));
                map.put("responseMappingFile", spliceMappingPath(metadataNode.at("/responseMappingFile").textValue(), aggregationCode));
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/infrastructure/persistence/"
                        + aggregationPackage + "/" + logic + "Adapter.java";
                FreemarkerUtil.generator("PresentationsAdapter", map, fileName);
            });
        });
    }

    public void generateDerivationAdapter(Project project) {
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        if (!Files.exists(casesPath)) {
            return;
        }
        TemplateUtils.listFile(casesPath).forEach(path -> {
            Path apiPath = Paths.get(path.toString(), PROTOCOL_RPC_IN);
            if (!apiPath.toFile().exists()) {
                return;
            }
            TemplateUtils.listFile(apiPath).forEach(api -> {
                String s = api.getFileName().toString();
                if (!s.startsWith("DL")) {
                    return;
                }
                HashMap<String, Object> map = new HashMap<>();
                String[] split = api.toString().split(MICRC_SCHEMA + File.separator);
                map.put("protocolPath", split[1]);
                // basePackage
                String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
                map.put("basePackage", basePackage);
                // aggregationPackage
                OpenAPI openAPI = SwaggerUtil.readOpenApi(TemplateUtils.readFile(api));
                String aggregationCode = getAggregationCodeFromApi(openAPI);
                String aggregationName = DomainGenerationTask.AGGREGATION_NAME_MAP.get(aggregationCode);
                if (aggregationName == null) {
                    return;
                }
                String aggregationPackage = aggregationName.toLowerCase();
                map.put("aggregationPackage", aggregationPackage);
                // logic name
                String logic = openAPI.getInfo().getTitle();
                map.put("logic", logic);
                Map<String, Object> extensions = openAPI.getExtensions();
                if (extensions == null) {
                    return;
                }
                Object metadata = extensions.get("x-metadata");
                JsonNode metadataNode = JsonUtil.readTree(metadata);
                // custom
                map.put("custom", metadataNode.at("/customAdapter").textValue());
                // mapping
                map.put("requestMappingFile", spliceMappingPath(metadataNode.at("/requestMappingFile").textValue(), aggregationCode));
                map.put("responseMappingFile", spliceMappingPath(metadataNode.at("/responseMappingFile").textValue(), aggregationCode));
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/infrastructure/derivate/"
                        + aggregationPackage + "/" + logic + "Adapter.java";
                FreemarkerUtil.generator("DerivationsAdapter", map, fileName);
            });
        });
    }
}
