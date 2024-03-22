package io.micrc.core.gradle.plugin.applications;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import groovy.util.Eval;
import io.micrc.core.gradle.plugin.domain.DomainGenerationTask;
import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ApplicationGenerationTask {

    public static final String MICRC_SCHEMA = File.separator + "micrc" + File.separator + "schema";
    public static final String MICRC_SCHEMA_CASES = MICRC_SCHEMA + File.separator + "cases";

    private static final String PROTOCOL = File.separator + "protocol";
    private static final String PROTOCOL_API = PROTOCOL + File.separator + "api";
    private static final String PROTOCOL_RPC_IN = PROTOCOL + File.separator + "rpc" + File.separator + "in";
    private static ApplicationGenerationTask instance;

    private ApplicationGenerationTask() {
    }

    public static ApplicationGenerationTask newInstance() {
        if (instance == null) {
            instance = new ApplicationGenerationTask();
        }
        return instance;
    }

    public void generateBusinessService(Project project) {
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        if (!Files.exists(casesPath)) {
            return;
        }
        TemplateUtils.listFile(casesPath).forEach(path -> {
            Path apiPath = Paths.get(path.toString(), PROTOCOL_API);
            if (!Files.exists(apiPath)) {
                return;
            }
            String caseCode = path.getFileName().toString();
            TemplateUtils.listFile(apiPath).forEach(api -> {
                String s = api.getFileName().toString();
                if (!s.startsWith("BSLG")) {
                    return;
                }
                HashMap<String, Object> map = new HashMap<>();
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
                // event
                JsonNode eventsNode = metadataNode.at("/events");
                if (eventsNode.isArray()) {
                    Object eventResult = JsonUtil.writeValueAsList(eventsNode.toString(), Object.class)
                            .stream().map(even -> {
                        String evenJson = JsonUtil.writeValueAsString(even);
                        HashMap<String, Object> e = new HashMap<>();
                        e.put("event", JsonUtil.readPath(evenJson, "/event"));
                        e.put("topic", JsonUtil.readPath(evenJson, "/topic"));
                        List<Object> mappings = (List) JsonUtil.readPath(evenJson, "/mappings");
                        if (mappings == null) {
                            return null;
                        }
                        Object mappingResult = mappings.stream().map(mapp -> {
                            String mappJson = JsonUtil.writeValueAsString(mapp);
                            HashMap<String, Object> r = new HashMap<>();
                            r.put("receiver", JsonUtil.readPath(mappJson, "/receiver"));
                            r.put("service", JsonUtil.readPath(mappJson, "/service"));
                            r.put("batchModel", JsonUtil.readPath(mappJson, "/batchModel"));
                            r.put("mappingFile", spliceMappingPath((String) JsonUtil.readPath(mappJson, "/mappingFile"), aggregationCode));
                            return r;
                        }).collect(Collectors.toList());
                        e.put("mappings", mappingResult);
                        return e;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    map.put("events", eventResult);
                }
                Info info = openAPI.getInfo();
                Map<String, Object> infoExtensions = info.getExtensions();
                if (infoExtensions != null && infoExtensions.get("x-service") != null) {
                    Object service = infoExtensions.get("x-service");
                    JsonNode servicveNode = JsonUtil.readTree(service);
                    // permission
                    map.put("permission", servicveNode.at("/permission").textValue());
                    // custom
                    map.put("custom", servicveNode.at("/customContent").textValue());
                }
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/application/businesses/"
                        + aggregationPackage + "/" + logic + "Service.java";
                FreemarkerUtil.generator("BusinessesService", map, fileName);
                // command
                JsonNode commandNode = metadataNode.at("/command");
                Object comm = JsonUtil.writeObjectAsObject(commandNode, Object.class);
                if (comm == null) {
                    return;
                }
                String commJson = JsonUtil.writeValueAsString(comm);
                Object entity = JsonUtil.readPath(commJson, "/entity");
                map.put("entity", entity);
                map.put("entities", DomainGenerationTask.AGGREGATION_ENTITIES_MAP.get(aggregationCode));
                map.put("repository", spliceRepositoryClassName(map, entity));
                List<Object> logicParams = (List) JsonUtil.readPath(commJson, "/logicParams");
                if (logicParams != null) {
                    List<Object> paramResult = logicParams.stream().map(param -> {
                        String paramJson = JsonUtil.writeValueAsString(param);
                        HashMap<Object, Object> p = new HashMap<>();
                        p.put("name", JsonUtil.readPath(paramJson, "/name"));
                        p.put("mappingFile", spliceMappingPath((String) JsonUtil.readPath(paramJson, "/mappingFile"), aggregationCode));
                        return p;
                    }).collect(Collectors.toList());
                    map.put("logicParams", paramResult);
                }
                List<Object> logicResults = (List) JsonUtil.readPath(commJson, "/logicResults");
                if (logicResults != null) {
                    List<Object> resultResult = logicResults.stream().map(result -> {
                        String resultJson = JsonUtil.writeValueAsString(result);
                        HashMap<Object, Object> r = new HashMap<>();
                        r.put("path", JsonUtil.readPath(resultJson, "/path"));
                        r.put("mappingFile", spliceMappingPath((String) JsonUtil.readPath(resultJson, "/mappingFile"), aggregationCode));
                        return r;
                    }).collect(Collectors.toList());
                    map.put("logicResults", resultResult);
                }
                map.put("logicType", JsonUtil.readPath(commJson, "/logicType"));
                map.put("logicPath", JsonUtil.readPath(commJson, "/logicPath"));
                map.put("autoCreate", JsonUtil.readPath(commJson, "/autoCreate"));
                map.put("idPath", JsonUtil.readPath(commJson, "/idPath"));
                map.put("repositoryOrder", JsonUtil.readPath(commJson, "/repositoryOrder"));
                List<Object> models = (List) JsonUtil.readPath(commJson, "/models");
                if (models != null) {
                    List<Object> modelResult = models.stream().map(mode -> {
                        String modeJson = JsonUtil.writeValueAsString(mode);
                        HashMap<String, Object> m = new HashMap<>();
                        Object model = JsonUtil.readPath(modeJson, "/model");
                        Object batchFlag = JsonUtil.readPath(modeJson, "/batchFlag");
                        m.put("modelType", mapSchema2JavaType(model.toString(), DomainGenerationTask.AGGREGATION_SCHEMAS_MAP.get(aggregationCode), Boolean.parseBoolean(batchFlag != null ? batchFlag.toString() : null)));
                        m.put("protocol", spliceIntegrationProtocolPath((String) JsonUtil.readPath(modeJson, "/protocol"), caseCode));
                        m.put("responseMappingFile", spliceMappingPath((String) JsonUtil.readPath(modeJson, "/responseMappingFile"), aggregationCode));
                        m.put("requestMappingFile", spliceMappingPath((String) JsonUtil.readPath(modeJson, "/requestMappingFile"), aggregationCode));
                        m.put("concept", JsonUtil.readPath(modeJson, "/concept"));
                        m.put("order", JsonUtil.readPath(modeJson, "/order"));
                        m.put("ignoreIfParamAbsent", JsonUtil.readPath(modeJson, "/ignoreIfParamAbsent"));
                        m.put("batchEvent", JsonUtil.readPath(modeJson, "/batchEvent"));
                        m.put("batchFlag", batchFlag);
                        return m;
                    }).collect(Collectors.toList());
                    map.put("models", modelResult);
                }
                fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/domain/"
                        + aggregationPackage + "/command/" + logic + "Command.java";
                FreemarkerUtil.generator("Command", map, fileName);
            });
        });
    }
    private String mapSchema2JavaType(String refPropertyName, Map<String, Schema> allSchemas, boolean findBatchModel) {
        String modelType;
        Schema refProperty = allSchemas.get(refPropertyName);
        if ("array".equals(refProperty.getType())) {
            String itemsRefString = JsonUtil.readTree(refProperty).at("/items").toString();
            Object item$ref = JsonUtil.readPath(itemsRefString, "/$ref");
            String itemTypeName;
            if (item$ref == null) {
                String type = (String) JsonUtil.readPath(itemsRefString, "/type");
                String format = (String) JsonUtil.readPath(itemsRefString, "/format");
                itemTypeName = mapJsonType2JavaType(type, format);
            } else {
                itemTypeName = splitRefName((String) item$ref);
                itemTypeName = mapSchema2JavaType(itemTypeName, allSchemas, false);
            }
            if (findBatchModel) {
                // 批量集成使用单个模型接收
                modelType = itemTypeName;
            } else {
                modelType = "List<" + itemTypeName + ">";
            }
        } else if ("object".equals(refProperty.getType())) {
            modelType = refPropertyName;
        } else {
            modelType = mapJsonType2JavaType(refProperty.getType(), refProperty.getFormat());
        }
        return modelType;
    }

    private static String splitRefName(String $ref) {
        String[] split = $ref.split("/");
        return split[split.length - 1];
    }

    private String mapJsonType2JavaType(String jsonType, String format) {
        if ("object".equals(jsonType)) {
            return "Object";
        } else if ("string".equals(jsonType)) {
            return "String";
        } else if ("integer".equals(jsonType)) {
            if ("int64".equals(format)) {
                return "Long";
            } else {
                return "Integer";
            }
        } else if ("number".equals(jsonType)) {
            return "Double";
        } else if ("boolean".equals(jsonType)) {
            return "Boolean";
        } else if ("array".equals(jsonType)) {
            return "List";
        } else {
            return null;
        }
    }

    private String spliceMappingPath(String fileName, String aggregationCode) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        return "aggregations/" + aggregationCode + "/mapping/" + fileName;
    }

    private String spliceScriptPath(String fileName, String aggregationCode) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        return "aggregations/" + aggregationCode + "/rule/technology/" + fileName;
    }

    private String spliceIntegrationProtocolPath(String fileName, String caseCode) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return "cases/" + caseCode + "/protocol/rpc/out/" + fileName;
    }

    private static String getAggregationCodeFromApi(OpenAPI openAPI) {
        String[] urlSplit = openAPI.getServers().get(0).getUrl().split("/");
        return urlSplit[urlSplit.length - 1].replace("-", "").toUpperCase();
    }

    public void generateMainClass(Project project) {
        HashMap<String, Object> map = new HashMap<>();
        String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
        map.put("basePackage", basePackage);
        String contextName = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.contextName");
        contextName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, contextName);
        map.put("contextName", contextName);
        String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                + basePackage.replace(".", "/") + "/" + contextName + "ServiceApplication.java";
        FreemarkerUtil.generator("Main", map, fileName);
    }

    public void generatePresentationService(Project project) {
        Path casesPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_CASES);
        if (!Files.exists(casesPath)) {
            return;
        }
        TemplateUtils.listFile(casesPath).forEach(path -> {
            Path apiPath = Paths.get(path.toString(), PROTOCOL_API);
            if (!apiPath.toFile().exists()) {
                return;
            }
            String caseCode = path.getFileName().toString();
            TemplateUtils.listFile(apiPath).forEach(api -> {
                String s = api.getFileName().toString();
                if (!s.startsWith("INVE")) {
                    return;
                }
                HashMap<String, Object> map = new HashMap<>();
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
                Info info = openAPI.getInfo();
                Map<String, Object> infoExtensions = info.getExtensions();
                if (infoExtensions != null && infoExtensions.get("x-service") != null) {
                    Object service = infoExtensions.get("x-service");
                    JsonNode servicveNode = JsonUtil.readTree(service);
                    // permission
                    map.put("permission", servicveNode.at("/permission").textValue());
                    // custom
                    map.put("custom", servicveNode.at("/customContent").textValue());
                    // assembler
                    map.put("assembler", spliceMappingPath(servicveNode.at("/assembler").textValue(), aggregationCode));
                } else {
                    // assembler
                    map.put("assembler", spliceMappingPath(metadataNode.at("/assembler").textValue(), aggregationCode));
                }
                JsonNode queriesNode = metadataNode.at("/queries");
                if (queriesNode.isArray()) {
                    List<Object> queriesResult = JsonUtil.writeValueAsList(queriesNode.toString(), Object.class)
                            .stream().map(quer -> {
                        String querJson = JsonUtil.writeValueAsString(quer);
                        HashMap<String, Object> q = new HashMap<>();
                        Object entity = JsonUtil.readPath(querJson, "/entity");
                        q.put("repository", spliceRepositoryClassName(map, entity));
                        q.put("concept", JsonUtil.readPath(querJson, "/concept"));
                        q.put("method", JsonUtil.readPath(querJson, "/method"));
                        q.put("order", JsonUtil.readPath(querJson, "/order"));
                        Object o = JsonUtil.readPath(querJson, "/paramMappingFiles");
                        if (o != null) {
                            Object collect = ((List) o).stream().map(i -> spliceMappingPath((String) i, aggregationCode)).collect(Collectors.toList());
                            q.put("paramMappingFiles", collect);
                        }
                        return q;
                    }).collect(Collectors.toList());
                    map.put("queries", queriesResult);
                }
                JsonNode integrationsNode = metadataNode.at("/integrations");
                if (integrationsNode.isArray()) {
                    List<HashMap<String, Object>> integrationResult = JsonUtil.writeValueAsList(integrationsNode.toString(), Object.class)
                            .stream().map(inte -> {
                        String inteJson = JsonUtil.writeValueAsString(inte);
                        HashMap<String, Object> i = new HashMap<>();
                        i.put("protocol", spliceIntegrationProtocolPath((String) JsonUtil.readPath(inteJson, "/protocol"), caseCode));
                        i.put("requestMappingFile", spliceMappingPath((String) JsonUtil.readPath(inteJson, "/requestMappingFile"), aggregationCode));
                        i.put("responseMappingFile", spliceMappingPath((String) JsonUtil.readPath(inteJson, "/responseMappingFile"), aggregationCode));
                        i.put("concept", JsonUtil.readPath(inteJson, "/concept"));
                        i.put("order", JsonUtil.readPath(inteJson, "/order"));
                        i.put("ignoreIfParamAbsent", JsonUtil.readPath(inteJson, "/ignoreIfParamAbsent"));
                        return i;
                    }).collect(Collectors.toList());
                    map.put("integrations", integrationResult);
                }
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/application/presentations/"
                        + aggregationPackage + "/" + logic + "Service.java";
                FreemarkerUtil.generator("PresentationsService", map, fileName);
            });
        });
    }

    private Object spliceRepositoryClassName(HashMap<String, Object> map, Object entity) {
        if (entity == null) {
            return null;
        }
        return map.get("basePackage") + ".domain." + map.get("aggregationPackage") + ".integration." + entity + "Repository";
    }

    public void generateDerivationService(Project project) {
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
                Info info = openAPI.getInfo();
                Map<String, Object> infoExtensions = info.getExtensions();
                if (infoExtensions != null && infoExtensions.get("x-service") != null) {
                    Object service = infoExtensions.get("x-service");
                    JsonNode servicveNode = JsonUtil.readTree(service);
                    // permission
                    map.put("permission", servicveNode.at("/permission").textValue());
                    // custom
                    map.put("custom", servicveNode.at("/customContent").textValue());
                    // assembler
                    map.put("assembler", spliceMappingPath(servicveNode.at("/assembler").textValue(), aggregationCode));
                } else {
                    // assembler
                    map.put("assembler", spliceMappingPath(metadataNode.at("/assembler").textValue(), aggregationCode));
                }
                JsonNode queriesNode = metadataNode.at("/queries");
                if (queriesNode.isArray()) {
                    List<Object> queriesResult = JsonUtil.writeValueAsList(queriesNode.toString(), Object.class)
                            .stream().map(quer -> {
                        String querJson = JsonUtil.writeValueAsString(quer);
                        HashMap<String, Object> q = new HashMap<>();
                        Object entity = JsonUtil.readPath(querJson, "/entity");
                        q.put("repository", spliceRepositoryClassName(map, entity));
                        q.put("concept", JsonUtil.readPath(querJson, "/concept"));
                        q.put("method", JsonUtil.readPath(querJson, "/method"));
                        q.put("order", JsonUtil.readPath(querJson, "/order"));
                        Object o = JsonUtil.readPath(querJson, "/paramMappingFiles");
                        if (o != null) {
                            Object collect = ((List) o).stream().map(i -> spliceMappingPath((String) i, aggregationCode)).collect(Collectors.toList());
                            q.put("paramMappingFiles", collect);
                        }
                        return q;
                    }).collect(Collectors.toList());
                    map.put("queries", queriesResult);
                }
                JsonNode generalTechnologiesNode = metadataNode.at("/generalTechnologies");
                if (generalTechnologiesNode.isArray()) {
                    List<HashMap<String, Object>> generalTechnologiesResult = JsonUtil.writeValueAsList(generalTechnologiesNode.toString(), Object.class)
                            .stream().map(ge -> {
                        String geJson = JsonUtil.writeValueAsString(ge);
                        HashMap<String, Object> i = new HashMap<>();
                        i.put("name", JsonUtil.readPath(geJson, "/name"));
                        i.put("paramMappingFile", spliceMappingPath((String) JsonUtil.readPath(geJson, "/paramMappingFile"), aggregationCode));
                        i.put("variableMappingFile", spliceMappingPath((String) JsonUtil.readPath(geJson, "/variableMappingFile"), aggregationCode));
                        i.put("routeContentPath", JsonUtil.readPath(geJson, "/routeContentPath"));
                        i.put("routeXmlFilePath", spliceScriptPath((String) JsonUtil.readPath(geJson, "/routeXmlFilePath"), aggregationCode));
                        i.put("order", JsonUtil.readPath(geJson, "/order"));
                        return i;
                    }).collect(Collectors.toList());
                    map.put("generalTechnologies", generalTechnologiesResult);
                }
                JsonNode specialTechnologiesNode = metadataNode.at("/specialTechnologies");
                if (specialTechnologiesNode.isArray()) {
                    List<HashMap<String, Object>> specialTechnologiesResult = JsonUtil.writeValueAsList(specialTechnologiesNode.toString(), Object.class)
                            .stream().map(ge -> {
                        String geJson = JsonUtil.writeValueAsString(ge);
                        HashMap<String, Object> i = new HashMap<>();
                        i.put("name", JsonUtil.readPath(geJson, "/name"));
                        i.put("paramMappingFile", spliceMappingPath((String) JsonUtil.readPath(geJson, "/paramMappingFile"), aggregationCode));
                        i.put("variableMappingFile", spliceMappingPath((String) JsonUtil.readPath(geJson, "/variableMappingFile"), aggregationCode));
                        i.put("technologyType", JsonUtil.readPath(geJson, "/technologyType"));
                        i.put("scriptFilePath", spliceScriptPath((String) JsonUtil.readPath(geJson, "/scriptFilePath"), aggregationCode));
                        i.put("scriptContentPath", JsonUtil.readPath(geJson, "/scriptContentPath"));
                        i.put("order", JsonUtil.readPath(geJson, "/order"));
                        return i;
                    }).collect(Collectors.toList());
                    map.put("specialTechnologies", specialTechnologiesResult);
                }
                String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                        + basePackage.replace(".", "/") + "/application/derivations/"
                        + aggregationPackage + "/" + logic + "Service.java";
                FreemarkerUtil.generator("DerivationsService", map, fileName);
            });
        });
    }

}