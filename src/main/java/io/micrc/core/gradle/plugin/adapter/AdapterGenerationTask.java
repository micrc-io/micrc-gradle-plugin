// package io.micrc.core.gradle.plugin.adapter;

// import io.micrc.core.gradle.plugin.schemas.Schemas;
// import lombok.extern.slf4j.Slf4j;
// import org.gradle.api.DefaultTask;
// import org.gradle.api.tasks.TaskAction;

// import java.io.File;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.Arrays;
// import java.util.Optional;

// @Slf4j
// public class AdapterGenerationTask extends DefaultTask {

//     // 生成所有适配器接口和注解，附带实现类并处理自定义实现时的覆盖问题。依赖application任务
//     @TaskAction
//     public void adapterGeneration() {
//         Schemas.usercaseFiles.keySet().stream().forEach(usercase -> {
//             File[] usercaseFiles = Schemas.usercaseFiles.get(usercase);
//             Optional<File> businessFileOptions = Arrays.stream(usercaseFiles).filter(file -> AdapterConstants.BUSINESS_FILE_NAME.equals(file.getName())).findFirst();
//             if (businessFileOptions.isEmpty()) {
//                 return;
//             }
//             File businessFile = businessFileOptions.get();
//             String businessContent;
//             try {
//                 businessContent = Files.readString(Paths.get(businessFile.getPath()));
//             } catch (IOException e) {
//                 throw new RuntimeException(e);
//             }
//         });
//         // 1.获取所有用例下的business.json文件,解析其implementation节点下的protocol节点
//         // 2.获取衍生  展示 并抽取其协议
//         //
//     }

// }