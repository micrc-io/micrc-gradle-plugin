package io.micrc.core.gradle.plugin.domain;

import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;
import org.gradle.api.tasks.WorkResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        try {
            String resourceAggrPath = project.getProjectDir().getAbsolutePath() + MicrcCompilationConstants.SRC_MAIN_RESOURCES_AGGREGATIONS;
            TemplateUtils.clearDir(Path.of(resourceAggrPath));
            String schemaAggrPath = project.getBuildDir().getAbsolutePath() + MicrcCompilationConstants.MICRC_SCHEMA_AGGREGATIONS;
            // 复制build/schema到resource
            WorkResult result = project.copy(copySpec -> copySpec.from(schemaAggrPath).into(resourceAggrPath));
            // 再复制changeset文件到resource/db
            String aggregationsPath = project.getBuildDir() + MicrcCompilationConstants.MICRC_SCHEMA_AGGREGATIONS;
            TemplateUtils.listFile(Paths.get(aggregationsPath)).forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    return;
                }
                Path dbDir = Paths.get(path.toString(), MicrcCompilationConstants.DB);
                if (!dbDir.toFile().exists()) {
                    return;
                }
                TemplateUtils.listFile(dbDir).findFirst().ifPresent(changeSetPath -> {
                    String resourceDbPath = project.getProjectDir().getAbsolutePath()
                            + MicrcCompilationConstants.SRC_MAIN_RESOURCES_DB_CHANGELOG + File.separator
                            + project.getVersion() + File.separator + path.getFileName();
                    project.copy(copySpec -> copySpec.from(changeSetPath).into(resourceDbPath));
                });
            });
            System.out.println("复制模式文件完成，结果：" + result.getDidWork());
        } catch (Exception e) {
            log.error("复制模式文件失败");
        }
    }
}
