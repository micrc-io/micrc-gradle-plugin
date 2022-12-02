package io.micrc.core.gradle.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrc.core.gradle.plugin.adapter.IntegrationAdapterGenerationTask;
import io.micrc.core.gradle.plugin.applications.ApplicationServiceGenerationTask;
import io.micrc.core.gradle.plugin.basic.ProjectConfigure;
import io.micrc.core.gradle.plugin.manifests.ManifestsGenerationTask;
import io.micrc.core.gradle.plugin.manifests.SkaffoldConfigure;
import io.micrc.core.gradle.plugin.models.ModelGenerationTask;
import io.micrc.core.gradle.plugin.schemas.SchemaSynchronizeConfigure;
import lombok.SneakyThrows;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import javax.annotation.Nonnull;

public class MicrcCompilationPlugin implements Plugin<Project> {

    private static final String MICRC_GROUP_NAME = "micrc";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    @SneakyThrows
    public void apply(@Nonnull Project project) {
        SchemaSynchronizeConfigure schema = SchemaSynchronizeConfigure.instance();
        schema.configure(project);
        ProjectConfigure.newInstance(schema.isConfigurable(), schema.getSchemaLocation()).configure(project);
        // 生成根目录skaffold.xml文件
        SkaffoldConfigure.newInstance(schema.getSchemaLocation()).configure(project);
        // 处理提前预写参数
        project.getExtensions().create("micrc", MicrcCompilationExtension.class);
        // 注册Task
        project.getTasks().create("integrationAdapterGeneration", IntegrationAdapterGenerationTask.class);
        project.getTasks().create("manifestsGeneration", ManifestsGenerationTask.class);
        project.getTasks().create("applicationServiceGeneration", ApplicationServiceGenerationTask.class);
        project.getTasks().create("modelGeneration", ModelGenerationTask.class);

        // 切入执行task以及调整task执行顺序
        project.afterEvaluate(pj -> {
            this.taskRegister(pj);
        });
    }

    private void taskRegister(Project pj) {
        Task compileJavaTask = pj.getTasks().findByName("compileJava");
        System.out.println("execute micrc tasks before compileJavaTask");
        // 设置执行编译
        compileJavaTask.dependsOn("manifestsGeneration");
        Task integrationAdapterGenerationTask = pj.getTasks().findByName("integrationAdapterGeneration");
        Task manifestsGenerationTask = pj.getTasks().findByName("manifestsGeneration");
        Task applicationServiceGenerationTask = pj.getTasks().findByName("applicationServiceGeneration");
        Task modelGenerationTask = pj.getTasks().findByName("modelGeneration");
        integrationAdapterGenerationTask.setGroup(MICRC_GROUP_NAME);
        manifestsGenerationTask.setGroup(MICRC_GROUP_NAME);
        applicationServiceGenerationTask.setGroup(MICRC_GROUP_NAME);
        modelGenerationTask.setGroup(MICRC_GROUP_NAME);
    }
}
