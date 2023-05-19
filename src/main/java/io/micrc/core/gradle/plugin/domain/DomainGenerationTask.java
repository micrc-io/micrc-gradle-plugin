package io.micrc.core.gradle.plugin.domain;

import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class DomainGenerationTask {
    public static final String SRC_MAIN_RESOURCES =
        File.separator + "src" + File.separator + "main" + File.separator + "resources";

    public static final String SRC_MAIN_RESOURCES_AGGREGATIONS = SRC_MAIN_RESOURCES + File.separator + "aggregations";
    public static final String MICRC_SCHEMA_AGGREGATIONS =
        File.separator + "micrc" + File.separator + "schema" + File.separator + "aggregations";

    public static final String DB = File.separator + "db";
    public static final String SRC_MAIN_RESOURCES_DB_CHANGELOG = SRC_MAIN_RESOURCES + DB + File.separator + "changelog";

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
        log.error("根据openapi schema生成model class和jpa注解: " + project.getName());
    }

    public void copyModelMeta(Project project) {
        try {
            String resourceAggrPath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_AGGREGATIONS;
            TemplateUtils.clearDir(Path.of(resourceAggrPath));
            String schemaAggrPath = project.getBuildDir().getAbsolutePath() + MICRC_SCHEMA_AGGREGATIONS;
            // 复制build/schema到resource
            project.copy(copySpec -> copySpec.from(schemaAggrPath).into(resourceAggrPath));
            // 再复制changeset文件到resource/db
            String aggregationsPath = project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS;
            TemplateUtils.listFile(Paths.get(aggregationsPath)).forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    return;
                }
                Path dbDir = Paths.get(path.toString(), DB);
                if (!dbDir.toFile().exists()) {
                    return;
                }
                TemplateUtils.listFile(dbDir).findFirst().ifPresent(changeSetPath -> {
                    String resourceDbPath = project.getProjectDir().getAbsolutePath()
                            + SRC_MAIN_RESOURCES_DB_CHANGELOG + File.separator
                            + project.getVersion() + File.separator + path.getFileName();
                    project.copy(copySpec -> copySpec.from(changeSetPath).into(resourceDbPath));
                });
            });
            log.info("model schema file copy complete. ");
        } catch (Exception e) {
            log.error("model schema file copy error. ", e);
        }
    }
}
