package io.micrc.core.gradle.plugin.applications;

import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    ArrayList<Object> eventResult = new ArrayList<>();
                    List events = (List) JsonUtil.readPath(busiJson, "/events");
                    if (events == null) {
                        return;
                    }
                    events.forEach(even -> {
                        String evenJson = JsonUtil.writeValueAsString(even);
                        HashMap<String, Object> e = new HashMap<>();
                        e.put("event", JsonUtil.readPath(evenJson, "/event"));
                        e.put("topic", JsonUtil.readPath(evenJson, "/topic"));
                        ArrayList<Object> mappingResult = new ArrayList<>();
                        List mappings = (List) JsonUtil.readPath(evenJson, "/mappings");
                        if (mappings == null) {
                            return;
                        }
                        mappings.forEach(mapp -> {
                            String mappJson = JsonUtil.writeValueAsString(mapp);
                            HashMap<String, Object> r = new HashMap<>();
                            r.put("receiver", JsonUtil.readPath(mappJson, "/receiver"));
                            r.put("service", JsonUtil.readPath(mappJson, "/service"));
                            r.put("mappingFile", JsonUtil.readPath(mappJson, "/mappingFile"));
                            mappingResult.add(r);
                        });
                        e.put("mappings", mappingResult);
                        eventResult.add(e);
                    });
                    map.put("events", eventResult);
                    map.put("permission", JsonUtil.readPath(busiJson, "/permission"));
                    map.put("custom", JsonUtil.readPath(busiJson, "/custom"));
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + map.get("package") + "/" + map.get("project") + "/application/businesses/"
                            + map.get("aggregationPackage") + "/" + map.get("logic") + "Service.java";
                    FreemarkerUtil.generator("BusinessesService", map, fileName);
                    // command todo,fill the command ftl
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
                    List queries = (List) JsonUtil.readPath(presJson, "/queries");
                    if (queries != null) {
                        ArrayList<Object> queriesResult = new ArrayList<>();
                        queries.forEach(quer -> {
                            String querJson = JsonUtil.writeValueAsString(quer);
                            HashMap<String, Object> q = new HashMap<>();
                            q.put("repositoryClassPath", JsonUtil.readPath(querJson, "/repositoryClassPath"));
                            q.put("concept", JsonUtil.readPath(querJson, "/concept"));
                            q.put("method", JsonUtil.readPath(querJson, "/method"));
                            q.put("order", JsonUtil.readPath(querJson, "/order"));
                            q.put("paramMappingFiles", JsonUtil.readPath(querJson, "/paramMappingFiles"));
                            queriesResult.add(q);
                        });
                        map.put("queries", queriesResult);
                    }
                    List integrations = (List) JsonUtil.readPath(presJson, "/integrations");
                    if (integrations != null) {
                        ArrayList<Object> integrationResult = new ArrayList<>();
                        integrations.forEach(inte -> {
                            String inteJson = JsonUtil.writeValueAsString(inte);
                            HashMap<String, Object> i = new HashMap<>();
                            i.put("protocol", JsonUtil.readPath(inteJson, "/protocol"));
                            i.put("requestMappingFile", JsonUtil.readPath(inteJson, "/requestMappingFile"));
                            i.put("responseMappingFile", JsonUtil.readPath(inteJson, "/responseMappingFile"));
                            i.put("concept", JsonUtil.readPath(inteJson, "/concept"));
                            i.put("order", JsonUtil.readPath(inteJson, "/order"));
                            integrationResult.add(i);
                        });
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

    public void generateDerivationService(Project project) {
        log.error("根据衍生服务元数据生成服务接口和实现类，及其注解");
    }

}