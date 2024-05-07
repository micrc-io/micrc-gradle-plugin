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
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.apache.groovy.json.internal.LazyMap;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
        String activeProfile = getActiveProfile(project);
        String envCamelcase = activeProfile.substring(0, 1).toUpperCase() + activeProfile.substring(1);
        log.info("generate code by: " + activeProfile);
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        // basePackage
        String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
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
                    Info info = openAPI.getInfo();
                    Map<String, Object> infoExtensions = info.getExtensions();
                    if (infoExtensions != null && infoExtensions.get("x-adapter") != null) {
                        Object adapter = infoExtensions.get("x-adapter");
                        JsonNode adapterNode = JsonUtil.readTree(adapter);
                        // custom
                        map.put("custom", adapterNode.at("/customContent").textValue());
                        // mapping
                        map.put("requestMappingFile", spliceMappingPath(adapterNode.at("/requestMappingFile").textValue(), aggregationCode));
                        map.put("responseMappingFile", spliceMappingPath(adapterNode.at("/responseMappingFile").textValue(), aggregationCode));
                    }
                    map.put("rootEntityName", aggregationName);
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/command/"
                            + aggregationPackage + "/" + logic + "Adapter.java";
                    FreemarkerUtil.generator("BusinessesAdapter", map, fileName);
                } else if ("LISTENER".equals(portType)) {
//                    JsonNode listenersNode = metadataNode.at("/listeners");
//                    if (listenersNode.isArray()) {
//                        JsonUtil.writeValueAsList(listenersNode.toString(), Object.class)
//                                .forEach(listener -> {
//                            String listenerJson = JsonUtil.writeValueAsString(listener);
//                            // event
//                            Object event = JsonUtil.readPath(listenerJson, "/event");
//                            map.put("event", event);
//                            // topic
//                            Object topic = JsonUtil.readPath(listenerJson, "/topic");
//                            map.put("topic", topic);
//                            String factory = "kafkaListenerContainerFactory";
//                            if (!"default".equalsIgnoreCase(activeProfile) && !"local".equalsIgnoreCase(activeProfile)) {
//                                Object contextMeta = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
//                                        "x.content.server.middlewares.broker.topicProfile");
//                                if (null != contextMeta) {
//                                    LazyMap lazyMap = JsonUtil.writeObjectAsObject(contextMeta, LazyMap.class);
//                                    String provider = lazyMap.entrySet().stream()
//                                            .filter(entry -> JsonUtil.writeObjectAsList(entry.getValue(), Object.class).contains(topic))
//                                            .map(Map.Entry::getKey).findFirst().orElseThrow();
//                                    if (!"public".equalsIgnoreCase(provider)) {
//                                        factory = factory + "-"  + provider;
//                                    }
//                                }
//                            }
//                            map.put("factory", factory);
//                            String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
//                                    + basePackage.replace(".", "/") + "/infrastructure/message/"
//                                    + aggregationPackage + "/" + event + logic + "Listener.java";
//                            FreemarkerUtil.generator("BusinessesListener", map, fileName);
//                        });
//                    }
                } else if ("SCHEDULE".equals(portType)) {
                    // cron
                    String cron = metadataNode.at("/schedule/cron").textValue();
                    map.put("cron", cron);
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/schedule/"
                            + aggregationPackage + "/" + logic + "Schedule.java";
                    FreemarkerUtil.generator("BusinessesSchedule", map, fileName);
                } else if ("RUNNER".equals(portType)) {
                    // dataPath
                    map.put("dataPath", split[1].replace("/api/", "/runner/"));
                    // runnerOrder
                    map.put("runnerOrder", metadataNode.at("/runner/runnerOrder").textValue());
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + basePackage.replace(".", "/") + "/infrastructure/runner/"
                            + aggregationPackage + "/" + logic + "Runner.java";
                    FreemarkerUtil.generator("BusinessesRunner", map, fileName);
                }
            });
        });
        Object partition = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
                "x.content.server.middlewares.broker.logicGroup");
        if (null != partition) {
            LazyMap lazyMap = JsonUtil.writeObjectAsObject(partition, LazyMap.class);
            lazyMap.forEach((key, value) -> {
                if (value instanceof List) {
                    List<Map<String,Object>> valueList = (List<Map<String,Object>>) value;
                    int size = ((List<?>) value).size();
                    for (int i = 0; i < size; i++) {
                        Map<String,Object> valueMap = valueList.get(i);
                        String factory = "kafkaListenerContainerFactory";
                        String kafkaInstance  = "";
                        if (!"default".equalsIgnoreCase(activeProfile) && !"local".equalsIgnoreCase(activeProfile)) {
                            kafkaInstance = "public";
                            Object contextMeta = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
                                    "x.content.server.middlewares.broker.topicProfile");
                            if (null != contextMeta) {
                                LazyMap lazyMapContextMeta = JsonUtil.writeObjectAsObject(contextMeta, LazyMap.class);
                                String provider = lazyMapContextMeta.entrySet().stream()
                                        .filter(entry -> JsonUtil.writeObjectAsList(entry.getValue(), Object.class).contains(((List)valueMap.get("topics")).get(0)))
                                        .map(Map.Entry::getKey).findFirst().orElseThrow();
                                if (!"public".equalsIgnoreCase(provider)) {
                                    factory = factory + "-"  + provider;
                                }
                            }
                        }
                        String finalFactory = factory;
                        String finalKafkaInstance = kafkaInstance;
                        String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                                + basePackage.replace(".", "/") + "/infrastructure/message/"
                                + key.toLowerCase() + "/" + key +finalKafkaInstance.toLowerCase()+i+ "Listener.java";
                        List<String> topics = ((List<String>) valueMap.get("topics")).stream().map(m -> m + envCamelcase).collect(Collectors.toList());
                        Map<String,Object> listenerMap = Map.of(
                                "topics", topics,
                                "services", valueMap.get("services"),
                                "groupId", valueMap.get("groupId")+"_"+activeProfile,
                                "factory", finalFactory,
                                "basePackage", basePackage,
                                "aggregationPackage", key.toLowerCase(),
                                "activeProfile", envCamelcase,
                                "name", key +finalKafkaInstance.toLowerCase()+i+ "Listener"

                        );
                        FreemarkerUtil.generator("BusinessesListener", listenerMap, fileName);
                    }
                }
            });
        } else {
            throw  new RuntimeException("partition is null, you lost config intro.json at broker/partition prop");
        }
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

    private String getActiveProfile(Project project) {
        if (project.hasProperty("active_profile")) {
            return (String) project.property("active_profile");
        }
        return "default";
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
                Info info = openAPI.getInfo();
                Map<String, Object> infoExtensions = info.getExtensions();
                if (infoExtensions != null && infoExtensions.get("x-adapter") != null) {
                    Object adapter = infoExtensions.get("x-adapter");
                    JsonNode adapterNode = JsonUtil.readTree(adapter);
                    // custom
                    map.put("custom", adapterNode.at("/customContent").textValue());
                    // mapping
                    map.put("requestMappingFile", spliceMappingPath(adapterNode.at("/requestMappingFile").textValue(), aggregationCode));
                    map.put("responseMappingFile", spliceMappingPath(adapterNode.at("/responseMappingFile").textValue(), aggregationCode));
                }
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
                Info info = openAPI.getInfo();
                Map<String, Object> infoExtensions = info.getExtensions();
                if (infoExtensions != null && infoExtensions.get("x-adapter") != null) {
                    Object adapter = infoExtensions.get("x-adapter");
                    JsonNode adapterNode = JsonUtil.readTree(adapter);
                    // custom
                    map.put("custom", adapterNode.at("/customContent").textValue());
                    // mapping
                    map.put("requestMappingFile", spliceMappingPath(adapterNode.at("/requestMappingFile").textValue(), aggregationCode));
                    map.put("responseMappingFile", spliceMappingPath(adapterNode.at("/responseMappingFile").textValue(), aggregationCode));
                }
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/infrastructure/derivate/"
                        + aggregationPackage + "/" + logic + "Adapter.java";
                FreemarkerUtil.generator("DerivationsAdapter", map, fileName);
            });
        });
    }
}
