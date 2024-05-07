package ${basePackage}.infrastructure.message.${aggregationPackage};

import io.micrc.core.annotations.message.MessageAdapter;
import io.micrc.core.annotations.message.MessageExecution;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import io.micrc.core.annotations.message.Adapter;
import java.io.IOException;

@MessageAdapter({
<#list services as service>
    @Adapter(commandServicePath="${basePackage}.application.businesses.${aggregationPackage}.${service.logic}Service",eventName="${service.event}",topicName="${service.topic}${activeProfile}")<#if service_has_next>,</#if>
</#list>
})
public interface ${name} {

    void adapt(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment) throws IOException;

    @Component("${name}")
    class ${name}Impl implements ${name} {

        @KafkaListener(
            topics = {<#list topics as topic>"${topic}"<#if topic_has_next>,</#if></#list>}, groupId = "${groupId}", autoStartup = "true", concurrency = "3", containerFactory = "${factory}"
        )
        @Override
        @MessageExecution
        public void adapt(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment) throws IOException {
        }
    }
}
