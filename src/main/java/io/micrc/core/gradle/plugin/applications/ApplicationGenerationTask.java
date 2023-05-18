package io.micrc.core.gradle.plugin.applications;

import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ApplicationGenerationTask {
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
        try {
            String meta = (String) SchemaSynchronizeConfigure.metaData.get("backendMeta");
            if (meta == null) {
                System.out.println("根据业务服务元数据生成服务接口和实现类，及其注解跳过");
                return;
            }
            HashMap<String, Object> map = new HashMap<>();
            map.put("package", JsonUtil.readPath(meta, "/package"));
            map.put("project", JsonUtil.readPath(meta, "/project"));
            List aggregations = (List) JsonUtil.readPath(meta, "/aggregations");
            if (aggregations == null) {
                return;
            }
            aggregations.forEach(aggr -> {
                String aggrJson = JsonUtil.writeValueAsString(aggr);
                Object aggregation = JsonUtil.readPath(aggrJson, "/aggregation");
                map.put("aggregation", aggregation);
                map.put("aggregationPackage", aggregation.toString().toLowerCase());
                List businesses = (List) JsonUtil.readPath(aggrJson, "/application/businesses");
                if (businesses == null) {
                    return;
                }
                businesses.forEach(busi -> {
                    String busiJson = JsonUtil.writeValueAsString(busi);
                    map.put("logic", JsonUtil.readPath(busiJson, "/logic"));
                    // businessService
                    List<Object> events = (List) JsonUtil.readPath(busiJson, "/events");
                    if (events == null) {
                        return;
                    }
                    Object eventResult = events.stream().map(even -> {
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
                            r.put("mappingFile", JsonUtil.readPath(mappJson, "/mappingFile"));
                            return r;
                        }).collect(Collectors.toList());
                        e.put("mappings", mappingResult);
                        return e;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    map.put("events", eventResult);
                    map.put("permission", JsonUtil.readPath(busiJson, "/permission"));
                    map.put("custom", JsonUtil.readPath(busiJson, "/custom"));
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + map.get("package") + "/" + map.get("project") + "/application/businesses/"
                            + map.get("aggregationPackage") + "/" + map.get("logic") + "Service.java";
                    FreemarkerUtil.generator("BusinessesService", map, fileName);
                    // command
                    Object comm = JsonUtil.readPath(busiJson, "/command");
                    if (comm == null) {
                        return;
                    }
                    String commJson = JsonUtil.writeValueAsString(comm);
                    Object entity = JsonUtil.readPath(commJson, "/entity");
                    map.put("entity", entity);
                    map.put("repository", spliceRepositoryClassName(map, entity));
                    List<Object> logicParams = (List) JsonUtil.readPath(commJson, "/logicParams");
                    if (logicParams != null) {
                        List<Object> paramResult = logicParams.stream().map(param -> {
                            String paramJson = JsonUtil.writeValueAsString(param);
                            HashMap<Object, Object> p = new HashMap<>();
                            p.put("name", JsonUtil.readPath(paramJson, "/name"));
                            p.put("mappingFile", JsonUtil.readPath(paramJson, "/mappingFile"));
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
                            r.put("mappingFile", JsonUtil.readPath(resultJson, "/mappingFile"));
                            return r;
                        }).collect(Collectors.toList());
                        map.put("logicResults", resultResult);
                    }
                    map.put("logicType", JsonUtil.readPath(commJson, "/logicType"));
                    map.put("logicPath", JsonUtil.readPath(commJson, "/logicPath"));
                    map.put("autoCreate", JsonUtil.readPath(commJson, "/autoCreate"));
                    map.put("idPath", JsonUtil.readPath(commJson, "/idPath"));
                    List<Object> models = (List) JsonUtil.readPath(commJson, "/models");
                    if (models != null) {
                        HashSet<Object> valobjs = new HashSet<>();
                        List<Object> modelResult = models.stream().map(mode -> {
                            String modeJson = JsonUtil.writeValueAsString(mode);
                            HashMap<String, Object> m = new HashMap<>();
                            Object model = JsonUtil.readPath(modeJson, "/model");
                            valobjs.add(model);
                            m.put("model", model);
                            m.put("protocol", JsonUtil.readPath(modeJson, "/protocol"));
                            m.put("responseMappingFile", JsonUtil.readPath(modeJson, "/responseMappingFile"));
                            m.put("requestMappingFile", JsonUtil.readPath(modeJson, "/requestMappingFile"));
                            m.put("concept", JsonUtil.readPath(modeJson, "/concept"));
                            m.put("order", JsonUtil.readPath(modeJson, "/order"));
                            m.put("batchEvent", JsonUtil.readPath(modeJson, "/batchEvent"));
                            m.put("batchFlag", JsonUtil.readPath(modeJson, "/batchFlag"));
                            return m;
                        }).collect(Collectors.toList());
                        map.put("valobjs", valobjs);
                        map.put("models", modelResult);
                    }
                    fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + map.get("package") + "/" + map.get("project") + "/domain/"
                            + map.get("aggregationPackage") + "/command/" + map.get("logic") + "Command.java";
                    FreemarkerUtil.generator("Command", map, fileName);
                });
            });
            System.out.println("根据业务服务元数据生成服务接口和实现类，及其注解完成");
        } catch (Exception e) {
            log.error("根据业务服务元数据生成服务接口和实现类，及其注解异常");
        }
    }

    public void generateListenerService(Project project) {
        log.error("根据业务服务(消息)元数据生成服务接口和实现类，及其注解");
    }

    public void generatePresentationService(Project project) {
        try {
            String meta = (String) SchemaSynchronizeConfigure.metaData.get("backendMeta");
            if (meta == null) {
                System.out.println("根据业务服务元数据生成服务接口和实现类，及其注解跳过");
                return;
            }
            HashMap<String, Object> map = new HashMap<>();
            map.put("package", JsonUtil.readPath(meta, "/package"));
            map.put("project", JsonUtil.readPath(meta, "/project"));
            List aggregations = (List) JsonUtil.readPath(meta, "/aggregations");
            if (aggregations == null) {
                return;
            }
            aggregations.forEach(aggr -> {
                String aggrJson = JsonUtil.writeValueAsString(aggr);
                Object aggregation = JsonUtil.readPath(aggrJson, "/aggregation");
                map.put("aggregation", aggregation);
                map.put("aggregationPackage", aggregation.toString().toLowerCase());
                List presentations = (List) JsonUtil.readPath(aggrJson, "/application/presentations");
                if (presentations == null) {
                    return;
                }
                presentations.forEach(pres -> {
                    String presJson = JsonUtil.writeValueAsString(pres);
                    map.put("logic", JsonUtil.readPath(presJson, "/logic"));
                    List<Object> queries = (List) JsonUtil.readPath(presJson, "/queries");
                    if (queries != null) {
                        List<Object> queriesResult = queries.stream().map(quer -> {
                            String querJson = JsonUtil.writeValueAsString(quer);
                            HashMap<String, Object> q = new HashMap<>();
                            Object entity = JsonUtil.readPath(querJson, "/entity");
                            q.put("repository", spliceRepositoryClassName(map, entity));
                            q.put("concept", JsonUtil.readPath(querJson, "/concept"));
                            q.put("method", JsonUtil.readPath(querJson, "/method"));
                            q.put("order", JsonUtil.readPath(querJson, "/order"));
                            q.put("paramMappingFiles", JsonUtil.readPath(querJson, "/paramMappingFiles"));
                            return q;
                        }).collect(Collectors.toList());
                        map.put("queries", queriesResult);
                    }
                    List<Object> integrations = (List) JsonUtil.readPath(presJson, "/integrations");
                    if (integrations != null) {
                        List<HashMap<String, Object>> integrationResult = integrations.stream().map(inte -> {
                            String inteJson = JsonUtil.writeValueAsString(inte);
                            HashMap<String, Object> i = new HashMap<>();
                            i.put("protocol", JsonUtil.readPath(inteJson, "/protocol"));
                            i.put("requestMappingFile", JsonUtil.readPath(inteJson, "/requestMappingFile"));
                            i.put("responseMappingFile", JsonUtil.readPath(inteJson, "/responseMappingFile"));
                            i.put("concept", JsonUtil.readPath(inteJson, "/concept"));
                            i.put("order", JsonUtil.readPath(inteJson, "/order"));
                            return i;
                        }).collect(Collectors.toList());
                        map.put("integrations", integrationResult);
                    }
                    map.put("assembler", JsonUtil.readPath(presJson, "/assembler"));
                    map.put("permission", JsonUtil.readPath(presJson, "/permission"));
                    map.put("custom", JsonUtil.readPath(presJson, "/custom"));
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + map.get("package") + "/" + map.get("project") + "/application/presentations/"
                            + map.get("aggregationPackage") + "/" + map.get("logic") + "Service.java";
                    FreemarkerUtil.generator("PresentationsService", map, fileName);
                });
            });
            System.out.println("根据展示服务元数据生成服务接口和实现类，及其注解完成");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("根据展示服务元数据生成服务接口和实现类，及其注解异常");
        }
    }

    private Object spliceRepositoryClassName(HashMap<String, Object> map, Object entity) {
        if (entity == null) {
            return null;
        }
        return map.get("package") + "." + map.get("project") + ".domain."
                + map.get("aggregationPackage") + ".integration." + entity + "Repository";
    }

    public void generateDerivationService(Project project) {
        log.error("根据衍生服务元数据生成服务接口和实现类，及其注解");
    }

}