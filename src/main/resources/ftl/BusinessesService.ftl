package ${package}.${project}.application.businesses.${aggregationPackage};

import ${package}.${project}.domain.${aggregationPackage}.command.${logic}Command;
import io.micrc.core.annotations.application.businesses.BusinessesExecution;
import io.micrc.core.annotations.application.businesses.BusinessesService;
import io.micrc.core.annotations.message.DomainEvents;
import io.micrc.core.annotations.message.Event;
import io.micrc.core.annotations.message.EventMapping;
import io.micrc.core.application.businesses.ApplicationBusinessesService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@DomainEvents(events = {
<#if events??>
<#list events as event>
        @Event(topicName = "${event.topic}", eventName = "${event.event}", mappings = {
        <#if event.mappings??>
        <#list event.mappings as mapping>
                @EventMapping(mappingPath = "${mapping.mappingFile}", mappingKey = "${mapping.service}", receiverAddress = "${mapping.receiver}"),
        </#list>
        </#if>
        }),
</#list>
</#if>
})
@BusinessesService<#if custom?? && custom != ''>(custom = true)</#if>
public interface ${logic}Service extends ApplicationBusinessesService<${logic}Command> {

    @Override
    void execute(${logic}Command command);

    @Component("${logic}Service")
    public static class ${logic}ServiceImpl implements ${logic}Service {

        <#if permission?? && permission != ''>@RequiresPermissions("${permission}")</#if>
        @BusinessesExecution
        @Transactional
        @Override
        public void execute(${logic}Command command) {
            ${custom}
        }
    }
}