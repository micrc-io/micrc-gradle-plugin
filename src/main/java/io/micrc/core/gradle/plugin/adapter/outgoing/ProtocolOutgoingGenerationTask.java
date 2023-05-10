package io.micrc.core.gradle.plugin.adapter.outgoing;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Project;

@Slf4j
public class ProtocolOutgoingGenerationTask {
    private static ProtocolOutgoingGenerationTask instance;

    private ProtocolOutgoingGenerationTask() {
    }

    public static ProtocolOutgoingGenerationTask newInstance() {
        if (instance == null) {
            instance = new ProtocolOutgoingGenerationTask();
        }
        return instance;
    }

    public void copyProtoOutgoing(Project project) {
        log.error("将出适配器协议拷贝到resource对应目录下，注意：需要deRef对应模型schema");
    }
}
