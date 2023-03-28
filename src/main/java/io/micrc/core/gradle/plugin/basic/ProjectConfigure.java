package io.micrc.core.gradle.plugin.basic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrc.core.gradle.plugin.MicrcCompilationExtension;
import lombok.SneakyThrows;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log
public class ProjectConfigure {
    private static ProjectConfigure INSTANCE;
    private final boolean configurable;
    private final String schemaPath;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DOMAIN_GROUP_POINTER = "/group";

    private static final String DOMAIN_CONTEXTS_POINTER = "/contexts";

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

    @SneakyThrows
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
            Path schemaFile = Paths.get(schemaPath, MicrcCompilationExtension.DOMAIN_DIR_NAME + File.separator + MicrcCompilationExtension.DOMAIN_INFO_NAME);
            String domainInfo = null;
            if (schemaFile.toFile().exists()) {
                try {
                    domainInfo = Files.readString(schemaFile);
                } catch (IOException e) {
                    // leave it out
                }
            }
            if (domainInfo == null || domainInfo.isBlank()) {
                throw new IllegalStateException("could not obtain domain info from domain-info.json. fix domain schema and retry configure project.");
            }
            String domainGroup = null;
            String serviceVersion = null;
            try {
                domainGroup = MAPPER.readTree(domainInfo).at(DOMAIN_GROUP_POINTER).asText();
                ArrayNode contextNode = (ArrayNode) MAPPER.readTree(domainInfo).at(DOMAIN_CONTEXTS_POINTER);
                List<Map<String, Object>> contexts = MAPPER.readerForListOf(HashMap.class).readValue(contextNode);
                System.out.println("this domain have contexts is --->");
                contexts.stream().forEach( context -> {
                    System.out.println(context.get("contextName"));
                });
                Optional<Map<String, Object>> contextOptional = contexts.stream().filter(context -> context.get("contextName").equals(project.getName())).findFirst();
                if (contextOptional.isEmpty()) {
                    throw new RuntimeException("could not resolve server version, please check contexts");
                }
                serviceVersion = contextOptional.get().get("version").toString();
            } catch (IOException e) {
                throw new RuntimeException("could not resolve domain info, please check domain-info.json");
            }
            project.setGroup(domainGroup);
            project.setVersion(serviceVersion);
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
        repositories.maven(
                repository -> {
                    repository.setUrl("https://repo.it.ouxxa.com/repository/maven-hub/");
                }
        );
        repositories.mavenCentral();
        project.afterEvaluate(proj -> {
            log.info("configure dependencies: "
                    + "spring-boot-starter, jakarta.persistence-api, "
                    + "lombok, spring-boot-starter-test, micrc-core. ");
            DependencyHandler dependencies = proj.getDependencies();
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter");
            // runtime core
            dependencies.add("implementation", "io.micrc.core:micrc-core:v0.0.1-20230328-5");
            dependencies.add("implementation", "io.micrc.core:micrc-annotations:v0.0.1-20230328-2");
            // persistence annotations
            dependencies.add("implementation", "jakarta.persistence:jakarta.persistence-api:2.2.3");
            // spring data jpa
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa");
            // kafka
            dependencies.add("implementation", "org.springframework.kafka:spring-kafka");
            // hibernate-types
            dependencies.add("implementation", "com.vladmihalcea:hibernate-types-55:2.18.0");
            // lombok
            dependencies.add("compileOnly", "org.projectlombok:lombok");
            dependencies.add("annotationProcessor", "org.projectlombok:lombok");
            // springboot test
            dependencies.add("testImplementation", "org.springframework.boot:spring-boot-starter-test");
            // temp
            dependencies.add("implementation", "org.apache.camel.springboot:camel-spring-boot-starter:3.18.1");
            dependencies.add("implementation", "org.springframework.boot:spring-boot-starter-web");
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
