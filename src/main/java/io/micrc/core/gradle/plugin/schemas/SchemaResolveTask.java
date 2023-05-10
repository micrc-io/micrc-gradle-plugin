package io.micrc.core.gradle.plugin.schemas;//package io.micrc.core.gradle.plugin.schemas;
//
//import io.micrc.core.gradle.lib.JsonUtil;
//import io.micrc.core.gradle.plugin.MicrcCompilationConstants;
//import lombok.extern.slf4j.Slf4j;
//import org.gradle.api.DefaultTask;
//import org.gradle.api.Project;
//import org.gradle.api.tasks.Input;
//import org.gradle.api.tasks.TaskAction;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.util.ResourceUtils;
//
//import javax.inject.Inject;
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//public class SchemaResolveTask extends DefaultTask {
//
//    @Input
//    private String schemaPath;
//
//    @TaskAction
//    public void SchemaResolve() throws IOException {
//        Project project = this.getProject();
//        log.info("resolve schema files, the schema path is {}", schemaPath);
//        // 收集该上下文的Meta入口文件
//        resolveServiceMeta(project);
//        // 收集所有协议以及整理协议关系
//        resolveProtocols(project);
//        // 收集Usercases
//        resolveUsercases(project);
//        System.out.println("resolve schema files end");
//    }
//
//    private void resolveServiceMeta(Project project) throws IOException {
//        File serviceMetaFile = Paths.get(schemaPath, project.getName() + File.separator + MicrcCompilationConstants.SERVICE_INTRO_NAME).toFile();
//        if (!serviceMetaFile.exists()) {
//            return;
//        }
//        String serviceMetaContent = Files.readString(Paths.get(serviceMetaFile.getPath()));
//        Map<String, Object> serviceMeta = JsonUtil.writeValueAsObject(serviceMetaContent, HashMap.class);
//        serviceMeta.putAll(serviceMeta);
//    }
//
//    /**
//     * 收集所有协议以及整理协议关系
//     *
//     * @param project
//     * @throws IOException
//     */
//    private void resolveProtocols(Project project) throws IOException {
//        Path protocolsDirPath = Paths.get(schemaPath, project.getName() + File.separator + MicrcCompilationConstants.PROTOCOLS_DIR_NAME);
//        File protocolsDir = protocolsDirPath.toFile();
//        if (!protocolsDir.exists()) {
//            return;
//        }
//        if (!protocolsDir.isDirectory()) {
//            throw new RuntimeException("protocols dir is not exist, please check this dir");
//        }
//        Resource[] resources = new PathMatchingResourcePatternResolver()
//                .getResources(ResourceUtils.FILE_URL_PREFIX + protocolsDir.getAbsolutePath() + "/**/*.json");
//        Arrays.stream(resources).forEach(resource -> {
//            try {
//                File protocolFile = resource.getFile();
//                // 读内容
//                String caseContent = Files.readString(Paths.get(protocolFile.getPath()));
//                Schemas.protocols.put(protocolFile.getName().replace(".json", ""), caseContent);
//                // 造关系
//                File model = new File(new String(protocolFile.getParent().getBytes(StandardCharsets.UTF_8)));
//                if (!Schemas.protocolRelations.containsKey(model.getName())) {
//                    Schemas.protocolRelations.put(model.getName(), new ArrayList<>());
//                }
//                Schemas.protocolRelations.get(model.getName()).add(protocolFile.getName().replace(".json", ""));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
//
//    /**
//     * 收集Usercases
//     *
//     * @param project
//     */
//    private void resolveUsercases(Project project) {
//        Path usercasesDirPath = Paths.get(schemaPath, project.getName() + File.separator + MicrcCompilationConstants.USERCASES_DIR_NAME);
//        File usercasesDir = usercasesDirPath.toFile();
//        if (!usercasesDir.exists()) {
//            return;
//        }
//        if (!usercasesDir.isDirectory()) {
//            throw new RuntimeException("usercases dir is not exist, please check this dir");
//        }
//        File[] usercaseDirs = usercasesDir.listFiles(pathname -> pathname.getName().contains(MicrcCompilationConstants.USERCASE_DIR_START_NAME));
//        assert usercaseDirs != null;
//        Arrays.stream(usercaseDirs).forEach(usercaseDir -> {
//            try {
//                String caseContent = Files.readString(Paths.get(usercaseDir.getPath() + File.separator + MicrcCompilationConstants.USERCASE_INTRO_NAME));
//                Schemas.usercaseMetas.put(usercaseDir.getName(), caseContent);
//                Schemas.usercaseFiles.put(usercaseDir.getName(), usercaseDir.listFiles());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
//
//    @Inject
//    public SchemaResolveTask(String schemaPath) {
//        this.schemaPath = schemaPath;
//    }
//
//
//    public String getSchemaPath() {
//        return schemaPath;
//    }
//
//    public SchemaResolveTask() {
//    }
//}