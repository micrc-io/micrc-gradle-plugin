package io.micrc.core.gradle.plugin.applications;

import io.micrc.core.gradle.plugin.lib.FreemarkerUtil;
import io.micrc.core.gradle.plugin.lib.JsonUtil;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
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
            String metaFilePath = project.getBuildDir() + File.separator + "micrc" + File.separator + "schema" + File.separator + "backendMetadata.json";
            String meta = TemplateUtils.readFile(Path.of(metaFilePath));
            HashMap<String, Object> map = new HashMap<>();
            map.put("package", JsonUtil.readPath(meta, "/package"));
            map.put("project", JsonUtil.readPath(meta, "/project"));
            List aggregations = (List) JsonUtil.readPath(meta, "/aggregations");
            aggregations.forEach(aggr -> {
                String aggrJson = JsonUtil.writeValueAsString(aggr);
                Object aggregation = JsonUtil.readPath(aggrJson, "/aggregation");
                map.put("aggregation", aggregation);
                map.put("aggregationPackage", aggregation.toString().toLowerCase());
                List businesses = (List) JsonUtil.readPath(aggrJson, "/application/businesses");
                businesses.forEach(busi -> {
                    String busiJson = JsonUtil.writeValueAsString(busi);
                    map.put("logic", JsonUtil.readPath(busiJson, "/logic"));
                    // businessService
                    // todo, map.put("events", );
                    map.put("permission", JsonUtil.readPath(busiJson, "/permission"));
                    map.put("custom", JsonUtil.readPath(busiJson, "/custom"));
                    String fileName = project.getProjectDir().getAbsolutePath() + "/src/main/java/"
                            + map.get("package") + "/" + map.get("project") + "/application/businesses/"
                            + map.get("aggregationPackage") + "/" + map.get("logic") + "Service.java";
                    FreemarkerUtil.generator("BusinessesService", map, fileName);
                    // command
                    // todo, fix command
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
        log.error("根据展示服务元数据生成服务接口和实现类，及其注解");
    }

    public void generateDerivationService(Project project) {
        log.error("根据衍生服务元数据生成服务接口和实现类，及其注解");
    }

}