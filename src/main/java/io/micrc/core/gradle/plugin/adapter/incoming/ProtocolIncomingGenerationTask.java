package io.micrc.core.gradle.plugin.adapter.incoming;

import com.fasterxml.jackson.databind.JsonNode;
import groovy.util.Eval;
import io.micrc.core.gradle.plugin.domain.DomainGenerationTask;
import io.micrc.core.gradle.plugin.lib.*;
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
        String activeProfile = SystemEnv.getActiveProfile(project);
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
        Object defaultProfileEnv = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
                "x.content.server.profileEnv");
        String profile = activeProfile;
        if (List.of("alpha","beta","ga").contains(activeProfile)) {
            profile = "prod";
        } else  if (List.of("default","local").contains(activeProfile)) {
            profile = (String)defaultProfileEnv;
        }
        boolean isGa = "ga".equals(activeProfile);
        String envCamelcase = activeProfile.substring(0, 1).toUpperCase() + activeProfile.substring(1);
        Object profiles = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
                "x.content.server.middlewares.broker.profiles");
//        if (null != profiles) {
//            LazyMap lazyMap = JsonUtil.writeObjectAsObject(profiles, LazyMap.class);
//            lazyMap.forEach((provider,value)-> {
//                System.out.println("xxxxx....." + "x.content.server.middlewares.broker.profiles." + provider + "." +profile+ ".resources.groups");
//                Object groups = Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"),
//                        "x.content.server.middlewares.broker.profiles." + provider + "." +profile+ ".resources.groups");
//                if (null != groups) {
//                    System.out.println("groups...."+ groups);
//                    Map<String,Object> groupsMap =(Map<String,Object>)groups;
//                    List<String> groupKeys = new ArrayList<>(groupsMap.keySet());
//                    for (int i = 0; i < groupKeys.size(); i++) {
//                        String factory = "kafkaListenerContainerFactory";
//                        String kafkaInstance  = "";
//                        List<String> contextTopics = ((List<String>)groupsMap.get(groupKeys.get(i)));
//                        if (!"default".equalsIgnoreCase(activeProfile) && !"local".equalsIgnoreCase(activeProfile)) {
//                            kafkaInstance = "public";
//                            if (!"public".equalsIgnoreCase(provider)) {
//                                factory = factory + "-"  + provider;
//                            }
//                        }
//                        String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
//                                + basePackage.replace(".", "/") + "/infrastructure/message/"
//                                + "test".toLowerCase() + "/" + "Test" +provider+kafkaInstance.toLowerCase()+i+ "Listener.java";
//                        List<String> topics = contextTopics.stream().map(topic -> isGa ? topic : topic+envCamelcase).collect(Collectors.toList());
//                        Map<String,Object> listenerMap = Map.of(
//                                "topics", topics,
//                                "services", Arrays.asList(),//valueMap.get("services"),
//                                "groupId", groupKeys.get(i)+(isGa ?  "": "_"+activeProfile),//valueMap.get("groupId")+(isGa ?  "": "_"+activeProfile),
//                                "factory", factory,
//                                "basePackage", basePackage,
//                                "aggregationPackage", "test",
//                                "activeProfile", isGa? "": envCamelcase,
//                                "name", "Test" +provider+kafkaInstance.toLowerCase()+i+ "Listener"
//
//                        );
//                        FreemarkerUtil.generator("BusinessesListener", listenerMap, fileName);
//                    }
//                }
//            });
//        }
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
