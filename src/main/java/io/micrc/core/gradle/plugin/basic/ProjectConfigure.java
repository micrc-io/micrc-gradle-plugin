package io.micrc.core.gradle.plugin.basic;

import io.micrc.core.gradle.plugin.MicrcCompilationExtension;
import lombok.extern.java.Log;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log
public class ProjectConfigure {
    private static ProjectConfigure INSTANCE;
    private final boolean configurable;
    private final String schemaPath;

    private ProjectConfigure(boolean configurable, String schemaPath) {
        this.configurable = configurable;
        this.schemaPath = schemaPath;
    }

    public static ProjectConfigure newInstance(boolean configurable, String schemaPath) {
        if (INSTANCE == null) {
            INSTANCE = new ProjectConfigure(configurable, schemaPath);
        }
        return INSTANCE;
    }

    public void configure(Project project) {
        log.info("configure micrc project. ");
        configureIdentity(project);
        configurePlugin(project);
        configureDependencies(project);
        configurePropertiesFileFlatten(project);
        configureJunitTest(project);
    }

    private void configureIdentity(Project project) {
        project.afterEvaluate(proj -> {
            log.info("configure micrc project info. ");
            // 如果没能clone schema，并且buld.gradle中也没有配置group或version
            // 那么插件没法正常配置项目
            if (!configurable) {
                if (proj.getGroup().toString().isBlank()) {
                    throw new IllegalStateException("not schema file for obtain 'group' info"
                                                    + ". must set 'group' property in build.gradle manual.");
                }
                if (proj.getVersion().toString().isBlank()) {
                    throw new IllegalStateException("not schema file for obtain 'version' info"
                                                    + ". must set 'version' property in build.gradle manual.");
                }
            }
            // schema/domain-info.json - 子域相关信息，其中包含group和version，子域下所有上下文名称都在group表达的命名空间下
            Path schemaFile = Paths.get(schemaPath, MicrcCompilationExtension.DOMAIN_INFO_NAME);
            String domainInfo = null;
            if (schemaFile.toFile().exists()) {
                try {
                    domainInfo = Files.readString(schemaFile);
                } catch (IOException e) {
                    // leave it out
                }
            }
            if (domainInfo == null || domainInfo.isBlank()) {
                // 规范化每个子域repo中schema的domain-info.json信息后，放开这个注释
                //throw new IllegalStateException("could not obtain domain info from domain-info.json. fix domain schema and retry configure project.");
            }
            // TODO 文件里是个json，最小化设计这个json，解析并设置group和version信息
            //project.setGroup("com.xian.colibri.example");
            //project.setVersion("0.0.1");
        });
    }

    private void configurePlugin(Project project) {
        log.info("configure gradle plugins: java, spring-boot, spring-dependency-management, jib. ");
        PluginContainer plugins = project.getPlugins();
        plugins.apply("java");
        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaPluginExtension.setSourceCompatibility(JavaVersion.VERSION_11);
        javaPluginExtension.setTargetCompatibility(JavaVersion.VERSION_11);
        plugins.apply("org.springframework.boot");
        plugins.apply("io.spring.dependency-management");
        plugins.apply("com.google.cloud.tools.jib");
    }

    private void configureDependencies(Project project) {
        log.info("configure maven repositories: mavenLocal, mavenCentral. ");
        RepositoryHandler repositories = project.getRepositories();
        repositories.mavenLocal();
        repositories.mavenCentral();
        project.afterEvaluate(proj -> {
            log.info("configure dependencies: "
                     + "spring-boot-starter, jakarta.persistence-api, "
                     + "lombok, spring-boot-starter-test, micrc-core. ");
            DependencyHandler dependencies = proj.getDependencies();
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter");
            dependencies.add("implementation", "jakarta.persistence:jakarta.persistence-api:2.2.3");
            dependencies.add("compileOnly", "org.projectlombok:lombok");
            dependencies.add("annotationProcessor", "org.projectlombok:lombok");
            dependencies.add("testImplementation", "org.springframework.boot:spring-boot-starter-test");
            dependencies.add("implementation", "io.micrc.core:micrc-core:0.0.1");
        });
    }

    @SuppressWarnings("all")
    private void configurePropertiesFileFlatten(Project project) {
        //log.info("configure processResources for flatten properties file: micrc.properties. ");
        Task processResources = project.getTasks().findByName("processResources");
        if (processResources != null) {
            processResources.doFirst(new Action<>() {
                @Override
                public void execute(@Nonnull Task task) {
                    ((ProcessResources) task).filesMatching("micrc.properties", fileCopyDetails ->
                    fileCopyDetails.expand(project.getProperties()));
                }
            });
        }
    }

    private void configureJunitTest(Project project) {
        //log.info("configure junit test. ");
        Task test = project.getTasks().findByName("test");
        if (test != null) {
            test.doFirst(task -> ((Test) task).useJUnitPlatform());
        }
    }
}
