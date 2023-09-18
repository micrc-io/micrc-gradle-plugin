package ${basePackage}.application.derivation.${aggregationPackage};

import io.micrc.core.annotations.application.derivations.DerivationsExecution;
import io.micrc.core.annotations.application.derivations.DerivationsService;
import io.micrc.core.annotations.application.derivations.GeneralTechnology;
import io.micrc.core.annotations.application.derivations.QueryLogic;
import io.micrc.core.annotations.application.derivations.SpecialTechnology;
import io.micrc.core.annotations.application.derivations.TechnologyType;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@DerivationsService(queryLogics = {
<#if queries??>
<#list queries as query>
        @QueryLogic(repositoryFullClassName = "${query.repository}",
                paramMappingFile = {<#list query.paramMappingFiles as paramMappingFile>"${paramMappingFile}",</#list>},
                methodName = "${query.method}",
                name = "${query.concept}"<#if query.order?? && query.order != ''>,
                order = ${query.order}</#if>
        ),
</#list>
</#if>
}, specialTechnologies = {
<#if specialTechnologies??>
<#list specialTechnologies as sp>
        @SpecialTechnology(name = "${sp.name}",
                paramMappingFile = "${sp.paramMappingFile}",
                <#if sp.order?? && sp.order != ''>order = ${sp.order},
                </#if><#if ge.variableMappingFile?? && ge.variableMappingFile != ''>variableMappingFile = "${ge.variableMappingFile}",
                </#if><#if sp.scriptContentPath?? && sp.scriptContentPath != ''>scriptContentPath = "${sp.scriptContentPath}",
                </#if><#if sp.scriptFilePath?? && sp.scriptFilePath != ''>scriptFilePath = "${sp.scriptFilePath}",
                </#if>technologyType = TechnologyType.${sp.technologyType}
        ),
</#list>
</#if>
}, generalTechnologies = {
<#if generalTechnologies??>
<#list generalTechnologies as ge>
        @GeneralTechnology(name = "${ge.name}",
                <#if ge.order?? && ge.order != ''>order = ${ge.order},
                </#if><#if ge.variableMappingFile?? && ge.variableMappingFile != ''>variableMappingFile = "${ge.variableMappingFile}",
                </#if><#if ge.routeContentPath?? && ge.routeContentPath != ''>routeContentPath = "${ge.routeContentPath}",
                </#if><#if ge.routeXmlFilePath?? && ge.routeXmlFilePath != ''>routeXmlFilePath = "${ge.routeXmlFilePath}",
                </#if>paramMappingFile = "${ge.paramMappingFile}"
        ),
</#list>
</#if>
},
assembler = "${assembler}"<#if custom?? && custom != ''>,
custom = true</#if>)
public interface ${logic}Service {

    String execute(String param);

    @Component("${logic}Service")
    public static class ${logic}ServiceImpl implements ${logic}Service {

        <#if permission?? && permission != ''>@RequiresPermissions("${permission}")</#if>
        @DerivationsExecution
        @Transactional(readOnly = true)
        @Override
        public String execute(String param) {
            <#if custom?? && custom != ''>${custom}<#else>return null;</#if>
        }
    }
}