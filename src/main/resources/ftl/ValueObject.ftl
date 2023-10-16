package ${basePackage}.domain.${aggregationPackage}.valobj;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ${modelName} implements Serializable {

<#if properties??>
    <#list properties as property>
    private ${property.modelType} ${property.name};

    </#list>
</#if>
}
