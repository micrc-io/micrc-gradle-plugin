package ${basePackage}.infrastructure.message.${aggregationPackage};

import io.micrc.core.annotations.message.MessageAdapter;
import io.micrc.core.annotations.message.MessageExecution;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;

@MessageAdapter(
    commandServicePath = "${basePackage}.application.businesses.${aggregationPackage}.${logic}Service",
    eventName = "${event}"
)
public interface ${event}${logic}Listener {

    void adapt(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment) throws IOException;

    @Component("${event}${logic}Listener")
    class ${event}${logic}ListenerImpl implements ${event}${logic}Listener {

        @KafkaListener(
            topics = {"${topic}"}, groupId = "${event}${logic}", autoStartup = "true", concurrency = "3"
        )
        @Override
        @MessageExecution
        public void adapt(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment) throws IOException {
        }
    }
}
