package io.micrc.core.gradle.plugin.models;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class ModelGenerationTask extends DefaultTask {
    // TODO 按 classes任务前执行这个任务
    @TaskAction
    public void modelGeneration() {
        System.out.println("this is ModelGenerationTask");
    }
}
