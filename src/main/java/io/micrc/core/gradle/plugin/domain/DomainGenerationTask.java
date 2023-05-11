package io.micrc.core.gradle.plugin.domain;

import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;
import org.gradle.api.tasks.WorkResult;

import java.nio.file.Path;

@Slf4j
public class DomainGenerationTask {
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
        log.error("根据openapi schema生成model class和jpa注解");
    }

    public void copyModelMeta(Project project) {
//        log.error("将openapi schema，liquibase changeset，jslt，dmn，查询规则元数据.json，技术规则元数据.json拷贝到resource对应目录下");
        String resourceAggrPath = project.getProjectDir().getAbsolutePath() + MicrcCompilationConstants.SRC_MAIN_RESOURCES_AGGREGATIONS;
        TemplateUtils.clearDir(Path.of(resourceAggrPath));
        String schemaAggrPath = project.getBuildDir().getAbsolutePath() + MicrcCompilationConstants.MICRC_SCHEMA_AGGREGATIONS;
        WorkResult result = project.copy(copySpec -> copySpec.from(schemaAggrPath).into(resourceAggrPath));
        System.out.println("复制模式文件完成，结果：" + result.getDidWork());
    }
}
