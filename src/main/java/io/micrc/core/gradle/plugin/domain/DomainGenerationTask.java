package io.micrc.core.gradle.plugin.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import groovy.util.Eval;
import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.SwaggerUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class DomainGenerationTask {
    public static final Map<String, String> AGGREGATION_NAME_MAP = new HashMap<>();
    public static final Map<String, Map<String, Schema>> AGGREGATION_SCHEMAS_MAP = new HashMap<>();
    private static final String SRC_MAIN_RESOURCES = File.separator + "src" + File.separator + "main" + File.separator + "resources";
    private static final String SRC_MAIN_RESOURCES_AGGREGATIONS = SRC_MAIN_RESOURCES + File.separator + "aggregations";
    private static final String SRC_MAIN_RESOURCES_CASES = SRC_MAIN_RESOURCES + File.separator + "cases";
    private static final String SRC_MAIN_RESOURCES_DB_CHANGELOG = SRC_MAIN_RESOURCES + File.separator + "db" + File.separator + "changelog";

    private static final String MICRC_SCHEMA = File.separator + "micrc" + File.separator + "schema";
    private static final String MICRC_SCHEMA_AGGREGATIONS = MICRC_SCHEMA + File.separator + "aggregations";
    private static final String MICRC_SCHEMA_CASES = MICRC_SCHEMA + File.separator + "cases";

    private static DomainGenerationTask instance;

    private DomainGenerationTask() {
    }

    public static DomainGenerationTask newInstance() {
        if (instance == null) {
            instance = new DomainGenerationTask();
        }
        return instance;
    }
    public void generateModel(Project project) {
        Path aggregationsPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS);
        if (!Files.exists(aggregationsPath)) {
            log.warn("Unable to find aggregation path: "
                    + project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS
                    + ", skip model generate. "
            );
            return;
        }
        TemplateUtils.listFile(aggregationsPath).forEach(path -> {
            Path modelFilePath = Paths.get(path.toString(), "model.json");
            if (!modelFilePath.toFile().exists()) {
                return;
            }
            HashMap<String, Object> map = new HashMap<>();
            // get basePackage
            String basePackage = (String) Eval.x(SchemaSynchronizeConfigure.metaData.get("contextMeta"), "x.content.server.basePackages");
            map.put("basePackage", basePackage);
            OpenAPI openAPI = SwaggerUtil.readOpenApi(TemplateUtils.readFile(modelFilePath));
            Map<String, Schema> allSchemas = openAPI.getComponents().getSchemas();
            // get aggregationPackage
            String aggregationName = allSchemas.entrySet().stream().map(entry -> {
                Schema schema = entry.getValue();
                Map extensions = schema.getExtensions();
                if ("object".equals(schema.getType()) && extensions != null && "entity".equals(extensions.get("x-model-type"))) {
                    return entry.getKey();
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).findFirst().orElse(null);
            if (aggregationName == null) {
                return;
            }
            AGGREGATION_NAME_MAP.put(path.toFile().getName(), aggregationName);
            AGGREGATION_SCHEMAS_MAP.put(path.toFile().getName(), allSchemas);
            String aggregationPackage = aggregationName.toLowerCase();
            map.put("aggregationPackage", aggregationPackage);
            String fileNamePrefix = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                    + basePackage.replace(".", "/") + "/domain/"
                    + aggregationPackage;
            // parse
            allSchemas.forEach((modelName, schema) -> {
                Map modelExtensions = schema.getExtensions();
                if (!"object".equals(schema.getType()) || modelExtensions == null) {
                    return;
                }
                // model name
                map.put("modelName", modelName);
                Map<String, Schema<?>> properties = schema.getProperties();
                if ("entity".equals(modelExtensions.get("x-model-type"))) {
                    // table name
                    String tableName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, modelName);
                    map.put("tableName", tableName);
                    parseProperties2Map(map, allSchemas, properties, true);
                    // one <-> many
                    parseReference2Map(map, allSchemas, modelExtensions.get("x-one-many"), modelExtensions.get("x-many-one"));
                    FreemarkerUtil.generator("Entity", map, fileNamePrefix + "/" + modelName + ".java");
                    // query rules
                    parseRules2Map(map, allSchemas, modelExtensions.get("x-query-rules"), modelName);
                    FreemarkerUtil.generator("Repository", map, fileNamePrefix + "/integration/" + modelName + "Repository.java");
                } else if ("valobj".equals(modelExtensions.get("x-model-type"))) {
                    String dataType = (String) modelExtensions.get("x-data-type");
                    if ("identity".equals(dataType)) {
                        FreemarkerUtil.generator("Identity", map, fileNamePrefix + "/valobj/" + modelName + ".java");
                    } else {
                        parseProperties2Map(map, allSchemas, properties, false);
                        FreemarkerUtil.generator("ValueObject", map, fileNamePrefix + "/valobj/" + modelName + ".java");
                    }
                }
            });
        });
    }

    private void parseReference2Map(HashMap<String, Object> map, Map<String, Schema> allSchemas, Object oneMany, Object manyOne) {
        if (oneMany != null) {
            String oneManyString = (String) oneMany;
            String manyType = splitRefName(oneManyString);
            String manyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, manyType) + "s";
            String mappedBy = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, map.get("modelName").toString());
            map.put("manyType", manyType);
            map.put("manyName", manyName);
            map.put("mappedBy", mappedBy);
        }
        if (manyOne != null) {
            String manyOneString = (String) manyOne;
            String oneType = splitRefName(manyOneString);
            String oneName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, oneType);
            String joinColumn = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, oneType) + "_id";
            map.put("oneType", oneType);
            map.put("oneName", oneName);
            map.put("joinColumn", joinColumn);
        }
    }

    private void parseRules2Map(HashMap<String, Object> map, Map<String, Schema> allSchemas, Object queryRules, String entityName) {
        if (queryRules == null) {
            return;
        }
        List<Object> list = (List) queryRules;
        List<HashMap<String, Object>> ruleMapList = list.stream().map(o -> {
            JsonNode jsonNode = JsonUtil.readTree(o);
            HashMap<String, Object> ruleMap = new HashMap<>();
            String name = jsonNode.at("/name").textValue();
            ruleMap.put("name", name);
            JsonNode paramTypesNode = jsonNode.at("/paramTypes");
            boolean hasPageable = false;
            if (paramTypesNode.isArray()) {
                List<String> paramTypes = JsonUtil.writeValueAsList(paramTypesNode.toString(), String.class);
                ArrayList<Object> paramList = new ArrayList<>();
                for (int i = 0; i < paramTypes.size(); i++) {
                    HashMap<String, Object> paramMap = new HashMap<>();
                    String paramType = getParamType(allSchemas, paramTypes.get(i));
                    if (!hasPageable) {
                        hasPageable = "Pageable".equals(paramType);
                    }
                    paramMap.put("type", paramType);
                    paramMap.put("index", i);
                    paramList.add(paramMap);
                }
                ruleMap.put("params", paramList);
            }
            String resultType = getParamType(allSchemas, jsonNode.at("/resultType").textValue());
            if (hasPageable) {
                if ("Page".equals(resultType)) {
                    resultType = "Page<" + entityName + ">";
                } else {
                    resultType = "Page<" + resultType + ">";
                }
            }
            ruleMap.put("resultType", resultType);
            return ruleMap;
        }).collect(Collectors.toList());
        map.put("rules", ruleMapList);
    }

    private void parseProperties2Map(HashMap<String, Object> map, Map<String, Schema> allSchemas, Map<String, Schema<?>> properties, boolean findIdentityName) {
        List<HashMap<String, String>> propertyMapList = properties.entrySet().stream().map(entry -> {
            String name = entry.getKey();
            Schema<?> property = entry.getValue();
            String $ref = property.get$ref();
            // 类型
            String modelType = null;
            String dataType = null;
            Map<String, Object> propertyExtensions = null;
            if (null == $ref) {
                // 基本类型
                modelType = mapJsonType2JavaType(property.getType(), property.getFormat());
                propertyExtensions = property.getExtensions();
            } else {
                // 值对象
                String refPropertyName = splitRefName($ref);
                modelType = mapSchema2JavaType(refPropertyName, allSchemas);
                propertyExtensions = allSchemas.get(refPropertyName).getExtensions();
            }
            if (propertyExtensions != null) {
                dataType = (String) propertyExtensions.get("x-data-type");
            }
            HashMap<String, String> propertyMap = new HashMap<>();
            propertyMap.put("name", name);
            propertyMap.put("modelType", modelType);
            propertyMap.put("dataType", dataType);
            if (findIdentityName && "identity".equals(dataType)) {
                map.put("modelIdentityName", modelType);
            }
            return propertyMap;
        }).collect(Collectors.toList());
        map.put("properties", propertyMapList);
    }

    private String getParamType(Map<String, Schema> allSchemas, String paramTypeString) {
        if (!paramTypeString.startsWith("#")) {
            String[] split = paramTypeString.split(",");
            return mapJsonType2JavaType(split[0], split.length == 2 ? split[1] : null);
        } else {
            String refPropertyName = splitRefName(paramTypeString);
            return mapSchema2JavaType(refPropertyName, allSchemas);
        }
    }

    private String mapSchema2JavaType(String refPropertyName, Map<String, Schema> allSchemas) {
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
                itemTypeName = mapSchema2JavaType(itemTypeName, allSchemas);
            }
            modelType = "List<" + itemTypeName + ">";
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

    public void copyModelMeta(Project project) {
        try {
            // 复制build/schema到resource(aggr和case)
            String resourceAggrPath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_AGGREGATIONS;
            TemplateUtils.clearDir(Path.of(resourceAggrPath));
            String schemaAggrPath = project.getBuildDir().getAbsolutePath() + MICRC_SCHEMA_AGGREGATIONS;
            project.copy(copySpec -> copySpec.from(schemaAggrPath).into(resourceAggrPath));
            String resourceCasePath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_CASES;
            TemplateUtils.clearDir(Path.of(resourceCasePath));
            String schemaCasePath = project.getBuildDir().getAbsolutePath() + MICRC_SCHEMA_CASES;
            project.copy(copySpec -> copySpec.from(schemaCasePath).into(resourceCasePath));
            Path aggregationsPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS);
            if (!Files.exists(aggregationsPath)) {
                log.warn("Unable to find aggregation path: "
                        + project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS
                        + ", skip model meta copy. "
                );
                return;
            }
            // 初始化DMN目录
            Path dmnPath = Paths.get(project.getProjectDir().getParent(),
                    project.getName().replace("-service", "-logic"), "src", "main", "resources", "dmn");
            TemplateUtils.clearDir(dmnPath);
            TemplateUtils.listFile(aggregationsPath).forEach(currentAggregations -> {
                // 复制changeset文件到resource/db
                Path dbFilePath = Paths.get(currentAggregations.toString(), "db.yaml");
                if (!dbFilePath.toFile().exists()) {
                    return;
                }
                String resourceDbPath = project.getProjectDir().getAbsolutePath()
                        + SRC_MAIN_RESOURCES_DB_CHANGELOG + File.separator
                        + project.getVersion() + File.separator + currentAggregations.getFileName();
                project.copy(copySpec -> copySpec.from(dbFilePath).into(resourceDbPath));
                // 复制dmn文件到xxx-logic下
                Path businessesRulePath = Paths.get(currentAggregations.toString(), "rule", "businesses");
                if (!businessesRulePath.toFile().exists()) {
                    return;
                }
                TemplateUtils.listFile(businessesRulePath).forEach(dmnFilePath -> {
                    if (!dmnFilePath.getFileName().toString().endsWith("dmn")) {
                        return;
                    }
                    Path logicResourceDmnPath = Paths.get(dmnPath.toString(), currentAggregations.getFileName().toString());
                    project.copy(copySpec -> copySpec.from(dmnFilePath).into(logicResourceDmnPath));
                });
            });
            log.info("model schema file copy complete. ");
        } catch (Exception e) {
            log.error("model schema file copy error. ", e);
        }
    }
}
