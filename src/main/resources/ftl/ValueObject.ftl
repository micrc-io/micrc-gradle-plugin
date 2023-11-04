package ${basePackage}.domain.${aggregationPackage}.valobj;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

import java.util.List;

@Data
public class ${modelName} implements Serializable {

<#if properties??>
    <#list properties as property>
    <#if property.dataType?? && property.dataType == 'text'>@Column(columnDefinition = "text")
    </#if>private ${property.modelType} ${property.name};

    </#list>
</#if>
}
