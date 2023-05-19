package io.micrc.core.gradle.plugin;

import io.micrc.core.gradle.plugin.adapter.ApidocGenerationTask;
import io.micrc.core.gradle.plugin.adapter.incoming.ProtocolIncomingGenerationTask;
import io.micrc.core.gradle.plugin.adapter.outgoing.ProtocolOutgoingGenerationTask;
import io.micrc.core.gradle.plugin.applications.ApplicationGenerationTask;
import io.micrc.core.gradle.plugin.domain.DomainGenerationTask;
import io.micrc.core.gradle.plugin.manifests.ManifestsGenerationTask;
import io.micrc.core.gradle.plugin.manifests.DeploymentConfigure;
import io.micrc.core.gradle.plugin.project.ProjectConfigure;
import io.micrc.core.gradle.plugin.project.SchemaSynchronizeConfigure;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MicrcCompilationPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
//        project.getExtensions().create("micrc", MicrcCompilationExtensions.class); // 配置参数，在task中使用
        SchemaSynchronizeConfigure schema = SchemaSynchronizeConfigure.newInstance();
        schema.configure(project);
        ProjectConfigure.newInstance(schema.isConfigurable()).configure(project);
        // 根目录生成skaffold.xml和jenkinsfile文件
        DeploymentConfigure.newInstance(schema.isConfigurable()).configure(project);

        // 注册task
        List<Task> tasks = new ArrayList<>();
        // 生成部署文件
        Task generateManifests = project
            .getTasks()
            .create("generateManifests", task -> ManifestsGenerationTask.newInstance().generateManifests(project));
        tasks.add(generateManifests);
        // 处理apidoc
        // 合并openapi
        Task mergeOpenapi = project
            .getTasks()
            .create("mergeOpenapi", task -> ApidocGenerationTask.newInstance().mergeOpenapi(project));
        tasks.add(mergeOpenapi);
        // 模拟消息发送
        Task mockMsgOpenapi = project
            .getTasks()
            .create("mockMsgOpenapi",
                task -> ApidocGenerationTask.newInstance().mockMsgOpenapi(project));
        tasks.add(mockMsgOpenapi);
        // 处理domain
        // 根据模型元数据生成模型类和映射注解
        Task generateModel = project
            .getTasks()
            .create("generateModel",
                task -> DomainGenerationTask.newInstance().generateModel(project));
        tasks.add(generateModel);
        // 拷贝元数据文件，包括schema(openapi)，changeset(liquibase)，mapping(jslt)，rule(dmn，查询接口元数据，技术端口元数据)
        Task copyModelMeta = project
            .getTasks()
            .create("copyModelMeta",
                task -> DomainGenerationTask.newInstance().copyModelMeta(project));
        tasks.add(copyModelMeta);
        // 处理adapter(出)
        // 拷贝出端口适配器协议
        Task copyProtoOutgoing = project
            .getTasks()
            .create("copyProtoOutgoing",
                task -> ProtocolOutgoingGenerationTask.newInstance().copyProtoOutgoing(project));
        tasks.add(copyProtoOutgoing);
        // 处理application和adapter(入)
        // 根据业务服务元数据生成业务服务/适配器接口和自定义实现类(包括注解)
        Task generateBusinessService = project
            .getTasks()
            .create("generateBusinessService",
                task -> ApplicationGenerationTask.newInstance().generateBusinessService(project));
        tasks.add(generateBusinessService);
        Task generateBusinessAdapter = project
            .getTasks()
            .create("generateBusinessAdapter",
                task -> ProtocolIncomingGenerationTask.newInstance().generateBusinessAdapter(project));
        tasks.add(generateBusinessAdapter);
        // 根据展示服务元数据生成展示服务/适配器接口和自定义实现类(包括注解)
        Task generatePresentationService = project
            .getTasks()
            .create("generatePresentationService",
                task -> ApplicationGenerationTask.newInstance().generatePresentationService(project));
        tasks.add(generatePresentationService);
        Task generatePresentationAdapter = project
            .getTasks()
            .create("generatePresentationAdapter",
                task -> ProtocolIncomingGenerationTask.newInstance().generatePresentationAdapter(project));
        tasks.add(generatePresentationAdapter);
        // 根据衍生服务元数据生成衍生服务/适配器接口和自定义实现类(包括注解)
        Task generateDerivationService = project
            .getTasks()
            .create("generateDerivationService",
                task -> ApplicationGenerationTask.newInstance().generateDerivationService(project));
        tasks.add(generateDerivationService);
        Task generateDerivationAdapter = project
            .getTasks()
            .create("generateDerivationAdapter",
                task -> ProtocolIncomingGenerationTask.newInstance().generateDerivationAdapter(project));
        tasks.add(generateDerivationAdapter);
        // 根据消息注册元数据生成消息监听适配器和业务服务接口及自定义实现类(包括注解)
        Task generateListenerService = project
            .getTasks()
            .create("generateListenerService",
                task -> ApplicationGenerationTask.newInstance().generateListenerService(project));
        tasks.add(generateListenerService);
        Task generateListenerAdapter = project
            .getTasks()
            .create("generateListenerAdapter",
                task -> ProtocolIncomingGenerationTask.newInstance().generateListenerAdapter(project));
        tasks.add(generateListenerAdapter);

        // 加入micrc组并调整执行顺序
        Task starter = project.getTasks().getByName("processResources");
        Collections.reverse(tasks);
        for (Task task : tasks) {
            task.setGroup("micrc");
            starter.dependsOn(task);
            starter = task;
        }
    }
}
