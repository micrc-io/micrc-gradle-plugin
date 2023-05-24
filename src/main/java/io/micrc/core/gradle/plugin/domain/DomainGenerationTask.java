package io.micrc.core.gradle.plugin.domain;

import io.micrc.core.gradle.plugin.lib.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class DomainGenerationTask {
    public static final String SRC_MAIN_RESOURCES = File.separator + "src" + File.separator + "main" + File.separator + "resources";
    public static final String SRC_MAIN_RESOURCES_AGGREGATIONS = SRC_MAIN_RESOURCES + File.separator + "aggregations";
    public static final String SRC_MAIN_RESOURCES_CASES = SRC_MAIN_RESOURCES + File.separator + "cases";
    public static final String SRC_MAIN_RESOURCES_DB_CHANGELOG = SRC_MAIN_RESOURCES + File.separator + "db" + File.separator + "changelog";

    public static final String MICRC_SCHEMA = File.separator + "micrc" + File.separator + "schema";
    public static final String MICRC_SCHEMA_AGGREGATIONS = MICRC_SCHEMA + File.separator + "aggregations";
    public static final String MICRC_SCHEMA_CASES = MICRC_SCHEMA + File.separator + "cases";

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
            // 复制build/schema到resource(aggr和case)
            String resourceAggrPath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_AGGREGATIONS;
            TemplateUtils.clearDir(Path.of(resourceAggrPath));
            String schemaAggrPath = project.getBuildDir().getAbsolutePath() + MICRC_SCHEMA_AGGREGATIONS;
            project.copy(copySpec -> copySpec.from(schemaAggrPath).into(resourceAggrPath));
            String resourceCasePath = project.getProjectDir().getAbsolutePath() + SRC_MAIN_RESOURCES_CASES;
            TemplateUtils.clearDir(Path.of(resourceCasePath));
            String schemaCasePath = project.getBuildDir().getAbsolutePath() + MICRC_SCHEMA_CASES;
            project.copy(copySpec -> copySpec.from(schemaCasePath).into(resourceCasePath));
            // 再复制changeset文件到resource/db
            Path aggregationsPath = Paths.get(project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS);
            if (!Files.exists(aggregationsPath)) {
                log.warn("Unable to find aggregation path: "
                        + project.getBuildDir() + MICRC_SCHEMA_AGGREGATIONS
                        + ", skip openapi merge, apidoc generation. "
                );
                return;
            }
            TemplateUtils.listFile(aggregationsPath).forEach(path -> {
                Path dbFilePath = Paths.get(path.toString(), "db.yaml");
                if (!dbFilePath.toFile().exists()) {
                    return;
                }
                String resourceDbPath = project.getProjectDir().getAbsolutePath()
                        + SRC_MAIN_RESOURCES_DB_CHANGELOG + File.separator
                        + project.getVersion() + File.separator + path.getFileName();
                project.copy(copySpec -> copySpec.from(dbFilePath).into(resourceDbPath));
            });
            log.info("model schema file copy complete. ");
        } catch (Exception e) {
            log.error("model schema file copy error. ", e);
        }
    }
}
