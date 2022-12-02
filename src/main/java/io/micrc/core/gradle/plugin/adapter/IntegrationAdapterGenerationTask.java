package io.micrc.core.gradle.plugin.adapter;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class IntegrationAdapterGenerationTask extends DefaultTask {

    // 生成所有适配器接口和注解，附带实现类并处理自定义实现时的覆盖问题。依赖application任务
    @TaskAction
    public void integrationAdapterGeneration() {
        System.out.println("this is IntegrationAdapterGenerationTask");
    }

}