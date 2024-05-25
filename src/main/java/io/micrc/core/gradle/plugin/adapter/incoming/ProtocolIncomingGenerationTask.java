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
import java.util.function.Function;
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
                }  else if ("SCHEDULE".equals(portType)) {
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

        JsonPathContext jsonPathContext = new JsonPathContext(SchemaSynchronizeConfigure.metaData.get("contextMeta"));
        LazyMap profiles = jsonPathContext.getMap("server.middlewares.broker.profiles");
        String env = IntroJsonParser.getIntroJsonProfile(project);
        profiles.forEach((provider,instance) -> {
            // in default env, only provider eq public support
            if (activeProfile.equals("default") && !"public".equals(provider)) {
                return;
            }

            Map<String,List<Map<String,Object>>> groups = transferGroupsAndApplications(jsonPathContext,provider,env);
            //LazyMap groups = jsonPathContext.getMap("server.middlewares.broker.profiles.{provider}.{env}.applications.groups", provider,env);
            groups.forEach((key, value)-> {
                int i = 0;
                for (Map<String,Object> valueMap : value) {
                    String factory = "kafkaListenerContainerFactory";
                    if (!"public".equalsIgnoreCase(provider)) {
                        factory = factory + "-"  + provider;
                    }
                    String fileName = JsonPathContext.path(
                            "{buildProjectDir}/src/main/java/{basePackage}/infrastructure/message/{eventTopic}/{upperEventTopic}{provider}{i}Listener.java",
                            project.getProjectDir().getAbsolutePath(),
                            basePackage.replace(".", "/"),
                            key.toLowerCase(),
                            IntroJsonParser.uppercase(key),
                            IntroJsonParser.uppercase(provider),
                            i+"");
                    List<String> topics = ((List<String>) valueMap.get("topics")).stream().map(topic -> IntroJsonParser.topicNameSuffixProfile(topic,activeProfile)).collect(Collectors.toList());
                    List<Map<String,Object>> services = ((List<Map<String,Object>>) valueMap.get("services")).stream().map(service -> {
                        Map<String, Object> newMap = new HashMap<>(service);
                        newMap.put("topic",IntroJsonParser.topicNameSuffixProfile((String)service.get("topic"),activeProfile));

                        return newMap;
                    }).collect(Collectors.toList());
                    Map<String,Object> listenerMap = Map.of(
                            "topics", topics,
                            "services", services,
                            "groupId", IntroJsonParser.nameSuffix((String)valueMap.get("groupId"),activeProfile,true,"_"),
                            "factory", factory,
                            "basePackage", basePackage,
                            "aggregationPackage", key.toLowerCase(),
                            "activeProfile", IntroJsonParser.uppercase(activeProfile),
                            "name", IntroJsonParser.uppercase(key) +IntroJsonParser.uppercase(provider)+i+ "Listener"

                    );
                    FreemarkerUtil.generator("BusinessesListener", listenerMap, fileName);
                    i++;
                }
            });
        });

    }

    private Map<String,List<Map<String,Object>>> transferGroupsAndApplications(JsonPathContext jsonPathContext, String provider, String env) {
        LazyMap applications = jsonPathContext.getMap("server.middlewares.broker.profiles.{provider}.{env}.applications", provider,env);

        Function<String, String> aggrFn = groupPath -> groupPath.split("_")[1];

        Function<String,Map<String,String>> logicFn = logicPath -> {
            String[] logic = logicPath.split("/");
            return Map.of(
                    "event", logic[0],
                    "groupId", logic[1],
                    "topic",logic[2],
                    "aggr",aggrFn.apply(logic[1])
            );
        };

        Function<String,List<Map<String,String>>> findLogicFn = path -> {
            String servicePath = path.split("\\|")[1];
            return Arrays.stream(servicePath.split(",")).map(logicFn).collect(Collectors.toList());
        };

        Map<String,List<Map<String,Object>>> newGroups = new HashMap<>();

        applications.keySet().forEach(f -> {
            String service = f.split(":")[1];
            List<Map<String,String>> logicList = findLogicFn.apply((String)applications.get(f));
            String aggr = logicList.get(0).get("aggr");

            Function<String, Map<String,List<Map<String,Object>>>> initAggrContext = (t) -> {
                Map<String,Map<String,Object>> groupMap = new HashMap<>();
                logicList.forEach(logic -> {
                    String groupId = logic.get("groupId");
                    Map<String,String> serviceMap = Map.of(
                            "topic", logic.get("topic"),
                            "logic", service,
                            "event",logic.get("event")
                    );
                    if (groupMap.containsKey(groupId)) {
                        Map<String,Object> targetAggr = groupMap.get(groupId);
                        List<String> topics = (List<String>)targetAggr.get("topics");
                        if (!topics.contains(logic.get("topic"))) {
                            topics.add(logic.get("topic"));
                        }
                        List<Map<String,String>> services = (List<Map<String,String>>)targetAggr.get("services");
                        services.add(serviceMap);
                    } else {
                        List<String> topics = new ArrayList<>();
                        topics.add(logic.get("topic"));
                        List<Map<String,String>> services = new ArrayList<>();
                        services.add(serviceMap);
                        Map<String,Object> aggrMap = new HashMap<>();
                        aggrMap.put("topics",topics);
                        aggrMap.put("groupId", groupId);
                        aggrMap.put("services",services);
                        groupMap.put(logic.get("groupId"), aggrMap);
                    }
                });
                return Map.of(
                        aggr, new ArrayList<Map<String, Object>>(groupMap.values())
                );
            };

            Map<String,List<Map<String,Object>>> initValues = initAggrContext.apply(null);
            if (newGroups.containsKey(aggr)) {
                List<Map<String,Object>> aggrs = newGroups.get(aggr);
                List<Map<String,Object>> initValuesList = initValues.get(aggr);
                initValuesList.forEach(initContext -> {
                    String groupId = (String)initContext.get("groupId");
                    Optional<Map<String,Object>> optionalAgrr = aggrs.stream().filter(aggrMap -> aggrMap.get("groupId").equals(groupId)).findFirst();
                    if (optionalAgrr.isPresent()) {
                        Map<String,Object> target = optionalAgrr.get();
                        List<String> initTopics = (List<String>)initContext.get("topics");
                        List<Map<String,String>> initServices = (List<Map<String,String>>)initContext.get("services");
                        List<String> targetTopics = (List<String>)target.get("topics");
                        List<Map<String,String>> targetServices = (List<Map<String,String>>)target.get("services");
                        initTopics.forEach(t -> {
                            if (!targetTopics.contains(t)) {
                                targetTopics.add(t);
                            }
                        });
                        targetServices.addAll(initServices);
                    } else {
                        aggrs.add(initContext);
                    }
                });
            } else {
                newGroups.putAll(initValues);
            }
        });

        return newGroups;
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
