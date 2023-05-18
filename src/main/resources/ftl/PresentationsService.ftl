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
        @QueryLogic(repositoryFullClassName = "${query.repository}", methodName = "${query.method}", name = "${query.concept}",
                <#if integrations??>order = ${query.order}, </#if>
                paramMappingFile = {<#list query.paramMappingFiles as paramMappingFile>"${paramMappingFile}",</#list>}
        ),
</#list>
</#if>
}, integrations = {
<#if integrations??>
<#list integrations as integration>
        @Integration(
                <#if integration.requestMappingFile??>requestMappingFile = "${integration.requestMappingFile}", </#if>
                <#if integration.responseMappingFile??>responseMappingFile = "${integration.responseMappingFile}", </#if>
                <#if integrations??>order = ${integration.order}, </#if>
                protocol = "${integration.protocol}", name = "${integration.concept}"
        ),
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