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
    @Event(topicName = "a", eventName = "b", mappings = {
        @EventMapping(mappingPath = "c", mappingKey = "d", receiverAddress = "e")
    })
})
@BusinessesService(custom = <#if custom?? && custom != ''>true<#else>false</#if>)
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