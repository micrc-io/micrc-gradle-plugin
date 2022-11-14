package io.micrc.core.gradle.plugin;

import groovy.text.SimpleTemplateEngine;
import io.micrc.core.gradle.plugin.basic.ProjectConfigure;
import io.micrc.core.gradle.plugin.manifests.SkaffoldConfigure;
import io.micrc.core.gradle.plugin.schemas.SchemaSynchronizeConfigure;
import org.apache.commons.io.IOUtils;
import org.gradle.api.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MicrcCompilationPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        SchemaSynchronizeConfigure schema = SchemaSynchronizeConfigure.instance();
        schema.configure(project);
        ProjectConfigure.newInstance(schema.isConfigurable(), schema.getSchemaPath()).configure(project);
        //SkaffoldConfigure.newInstance().configure(project);

        project.getExtensions().create("micrc", MicrcCompilationExtension.class);

        //System.out.println("根据同步回来的schema，配置group和version");
        //project.setGroup("com.xian.colibri.example");
        //project.setVersion("0.0.1");

        //System.out.println("配置部署描述符");
        //String projectName = project.getName();
        //String buildDirPath = project.getBuildDir().getAbsolutePath();
        //Map<String, String> properties = new HashMap<>();
        //properties.put("name", project.getName());
        //properties.put("build_relative_path", "../../../../");
        //properties.put("desc", String.format("%s chart for kubernetes", project.getName()));
        //properties.put("version", project.getVersion().toString());
        //String[] versions = project.getVersion().toString().split("\\.");
        //properties.put("chart_version",
        //               String.format(
        //                       "%d.%d.%d",
        //                       Integer.valueOf(versions[0]),
        //                       Integer.valueOf(versions[1] + versions[2]),
        //                       0
        //               )
        //);
        //
        //// skaffold.yaml
        //List<String> skaffoldPaths = List.of("manifest", "skaffold.yaml");
        //List<String> skaffoldTargetPaths = List.of("skaffold.yaml");
        //generateManifest(properties, project.getProjectDir().getAbsolutePath(), skaffoldPaths, skaffoldTargetPaths);
        //// helm Chart.yaml
        //List<String> chartPaths = List.of("manifest", "k8s", "helm", "Chart.yaml");
        //List<String> chartTargetPaths = List.of("manifest", "k8s", "helm", projectName, "Chart.yaml");
        //generateManifest(properties, buildDirPath, chartPaths, chartTargetPaths);
        //// helm values.yaml
        //List<String> valuesPaths = List.of("manifest", "k8s", "helm", "values.yaml");
        //List<String> valuesTargetPaths = List.of("manifest", "k8s", "helm", projectName, "values.yaml");
        //generateManifest(properties, buildDirPath, valuesPaths, valuesTargetPaths);
        //// helm .helmignore
        //List<String> helmignorePaths = List.of("manifest", "k8s", "helm", ".helmignore");
        //List<String> helmignoreTargetPaths = List.of("manifest", "k8s", "helm", projectName, ".helmignore");
        //generateManifest(properties, buildDirPath, helmignorePaths, helmignoreTargetPaths);
        //// helm templates _helpers.tpl
        //List<String> helpersPaths = List.of("manifest", "k8s", "helm", "templates", "_helpers.tpl");
        //List<String> helpersTargetPaths = List.of("manifest", "k8s", "helm", projectName, "templates", "_helpers.tpl");
        //generateManifest(properties, buildDirPath, helpersPaths, helpersTargetPaths);
        //// helm templates serviceaccount.yaml
        //List<String> serviceaccountPaths = List.of("manifest", "k8s", "helm", "templates", "serviceaccount.yaml");
        //List<String> serviceaccountTargetPaths = List.of("manifest", "k8s", "helm", projectName, "templates", "serviceaccount.yaml");
        //generateManifest(properties, buildDirPath, serviceaccountPaths, serviceaccountTargetPaths);
        //// helm templates hpa.yaml
        //List<String> hpaPaths = List.of("manifest", "k8s", "helm", "templates", "hpa.yaml");
        //List<String> hpaTargetPaths = List.of("manifest", "k8s", "helm", projectName, "templates", "hpa.yaml");
        //generateManifest(properties, buildDirPath, hpaPaths, hpaTargetPaths);
        //// helm templates service.yaml
        //List<String> servicePaths = List.of("manifest", "k8s", "helm", "templates", "service.yaml");
        //List<String> serviceTargetPaths = List.of("manifest", "k8s", "helm", projectName, "templates", "service.yaml");
        //generateManifest(properties, buildDirPath, servicePaths, serviceTargetPaths);
        //// helm templates deployment.yaml
        //List<String> deploymentPaths = List.of("manifest", "k8s", "helm", "templates", "deployment.yaml");
        //List<String> deploymentTargetPaths = List.of("manifest", "k8s", "helm", projectName, "templates", "deployment.yaml");
        //generateManifest(properties, buildDirPath, deploymentPaths, deploymentTargetPaths);
        //// kustomize base kustomization.yaml
        //List<String> kustomizeBasePaths = List.of("manifest", "k8s", "kustomize", "base", "kustomization.yaml");
        //generateManifest(properties, buildDirPath, kustomizeBasePaths, kustomizeBasePaths);
        //// kustomize local kustomization.yaml
        //List<String> kustomizeLocalPaths = List.of("manifest", "k8s", "kustomize", "local", "kustomization.yaml");
        //generateManifest(properties, buildDirPath, kustomizeLocalPaths, kustomizeLocalPaths);
    }

    private void generateManifest(
            Map<String, String> context,
            String buildPath,
            List<String> sourcePaths,
            List<String> targetPaths) {
        String sourcePath = String.join(File.separator, sourcePaths);
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("template file: " + sourcePath + " is not exists. ");
        }
        try {
            String flatten = new SimpleTemplateEngine()
                .createTemplate(IOUtils.toString(inputStream, StandardCharsets.UTF_8))
                .make(context)
                .toString();
            String targetDirPath = String.join(File.separator, targetPaths.subList(0, targetPaths.size() - 1));
            Path dirPath = Paths.get(buildPath, targetDirPath);
            Files.createDirectories(dirPath);
            String targetPath = String.join(File.separator, targetPaths);
            Files.writeString(
                    Paths.get(buildPath, String.join(File.pathSeparator, targetPath)),
                    flatten,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
