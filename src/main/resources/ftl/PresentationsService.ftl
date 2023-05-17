package ${package}.${project}.application.presentations.${aggregationPackage};

import io.micrc.core.annotations.application.presentations.Integration;
import io.micrc.core.annotations.application.presentations.PresentationsExecution;
import io.micrc.core.annotations.application.presentations.PresentationsService;
import io.micrc.core.annotations.application.presentations.QueryLogic;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@PresentationsService(assembler = "${assembler}", queryLogics = {
<#if queries??>
<#list queries as query>
        @QueryLogic(repositoryFullClassName = "${query.repositoryClassPath}", methodName = "${query.method}", name = "${query.concept}", order = ${query.order}, paramMappingFile = {
        <#list query.paramMappingFiles as paramMappingFile>
            "${paramMappingFile}",
        </#list>
        }),
</#list>
</#if>
}, integrations = {
<#if integrations??>
<#list integrations as integration>
        @Integration(protocol = "${integration.protocol}", requestMappingFile = "${integration.requestMappingFile}", responseMappingFile = "${integration.responseMappingFile}", name = "${integration.concept}", order = ${integration.order}),
</#list>
</#if>
}<#if custom?? && custom != ''>, custom = true</#if>)
public interface ${logic}Service {

    String execute(String param);

    @Component("${logic}Service")
    public static class ${logic}ServiceImpl implements ${logic}Service {

        <#if permission?? && permission != ''>@RequiresPermissions("${permission}")</#if>
        @PresentationsExecution
        @Transactional(readOnly = true)
        @Override
        public String execute(String param) {
            <#if custom?? && custom != ''>${custom}<#else>return null;</#if>
        }
    }
}