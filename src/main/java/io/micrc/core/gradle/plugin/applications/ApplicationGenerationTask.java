package io.micrc.core.gradle.plugin.applications;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

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
        log.error("根据业务服务元数据生成服务接口和实现类，及其注解");
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