package ${basePackage}.infrastructure.persistence.${aggregationPackage};

import io.micrc.core.annotations.integration.presentations.PresentationsAdapter;
import org.springframework.stereotype.Component;

@PresentationsAdapter(serviceName = "${logic}Service",<#if custom?? && custom != ''>custom = true,</#if>
    <#if requestMappingFile?? && requestMappingFile != ''>requestMappingFile = "${requestMappingFile}",</#if><#if responseMappingFile?? && responseMappingFile != ''>responseMappingFile = "${responseMappingFile}",</#if>
    protocolPath = "${protocolPath}"
)
public interface ${logic}Adapter {

    String adapt(String body);

    @Component("${logic}Adapter")
    public static class ${logic}AdapterImpl implements ${logic}Adapter {

        @Override
        public String adapt(String body) {
            <#if custom?? && custom != ''>${custom}<#else>return null;</#if>
        }
    }
}
