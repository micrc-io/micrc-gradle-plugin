package io.micrc.core.gradle.plugin.adapter.incoming;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

@Slf4j
public class ProtocolIncomingGenerationTask {
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
        log.error("根据业务适配器元数据生成适配器接口及实现类，及其注解");
    }

    public void generateListenerAdapter(Project project) {
        log.error("根据业务消息监听适配器元数据生成适配器接口及实现类，及其注解");
    }

    public void generatePresentationAdapter(Project project) {
        log.error("根据业务适配器元数据生成适配器接口及实现类，及其注解");
    }

    public void generateDerivationAdapter(Project project) {
        log.error("根据业务适配器元数据生成适配器接口及实现类，及其注解");
    }
}
