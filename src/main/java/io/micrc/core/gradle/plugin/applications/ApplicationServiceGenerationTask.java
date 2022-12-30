package io.micrc.core.gradle.plugin.applications;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class ApplicationServiceGenerationTask extends DefaultTask {
    // TODO 生成所有application接口和注解，内部附带实现类，并处理自定义实现时的覆盖问题。依赖model任务执行
    @TaskAction
    public void applicationServiceGeneration() {
        System.out.println("this is applicationServiceGenerationTask");
    }

}