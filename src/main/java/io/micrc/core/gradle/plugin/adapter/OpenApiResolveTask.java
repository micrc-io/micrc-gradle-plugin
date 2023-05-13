//package io.micrc.core.gradle.plugin.adapter;
//
//import io.micrc.core.gradle.plugin.lib.TemplateUtils;
//import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
//import io.micrc.core.gradle.plugin.schemas.Schemas;
//import lombok.extern.slf4j.Slf4j;
//import org.gradle.api.DefaultTask;
//import org.gradle.api.Project;
//import org.gradle.api.tasks.Input;
//import org.gradle.api.tasks.TaskAction;
//
//import javax.inject.Inject;
//import java.io.File;
//import java.util.Arrays;
//import java.util.Objects;
//
//@Slf4j
//public class OpenApiResolveTask extends DefaultTask {
//
//    @Input
//    private String schemaPath;
//
//    @TaskAction
//    public void OpenApiResolve() {
//        log.info("resolve openapi files, the schema path is {}", schemaPath);
//        if (Schemas.protocols.isEmpty()) {
//            return;
//        }
//        Project project = this.getProject();
//        File resourcesDir = new File(project.getProjectDir().getAbsolutePath() + File.separator + MicrcCompilationConstants.RESOURCE_DIR_PATH);
//        Schemas.protocolRelations.keySet().stream().forEach(
//                domainName -> {
//                    File domainDir = new File(resourcesDir + File.separator +
//                            AdapterConstants.ADAPTERS_DIR_NAME + File.separator +
//                            AdapterConstants.RPC_DIR_NAME + File.separator +
//                            domainName);
//                    if (domainDir.exists()) {
//                        Arrays.stream(Objects.requireNonNull(domainDir.listFiles())).forEach(File::delete);
//                        domainDir.delete();
//                    }
//                    domainDir.mkdir();
//                    Schemas.protocolRelations.get(domainName).forEach(protocolName -> {
//                        String filePath = domainDir + File.separator + protocolName + AdapterConstants.PROTOCOL_FILE_SUFFIX;
//                        AdapterGenerationResults.protocolsLocation.put(protocolName, filePath);
//                        TemplateUtils.saveStringToFile(filePath, Schemas.protocols.get(protocolName));
//                    });
//                }
//        );
//        log.info("resolve openapi files end...");
//    }
//
//    @Inject
//    public OpenApiResolveTask(String schemaPath) {
//        this.schemaPath = schemaPath;
//    }
//
//
//    public String getSchemaPath() {
//        return schemaPath;
//    }
//
//    public OpenApiResolveTask() {
//    }
//}