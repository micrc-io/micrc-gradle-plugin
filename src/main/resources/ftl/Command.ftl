package ${basePackage}.domain.${aggregationPackage}.command;

<#list entities as e>
import ${basePackage}.domain.${aggregationPackage}.${e};
</#list>
import ${basePackage}.domain.${aggregationPackage}.valobj.*;
import io.micrc.core.annotations.application.businesses.BatchProperty;
import io.micrc.core.annotations.application.businesses.CommandLogic;
import io.micrc.core.annotations.application.businesses.DeriveIntegration;
import io.micrc.core.annotations.application.businesses.LogicMapping;
import io.micrc.core.annotations.application.businesses.LogicType;
import io.micrc.core.annotations.application.businesses.RepositoryIntegration;
import io.micrc.core.annotations.application.businesses.TargetMapping;
import io.micrc.core.application.businesses.EventInfo;
import io.micrc.core.rpc.ErrorInfo;
import lombok.Data;

import java.util.List;

@Data
@CommandLogic(
    toLogicMappings = {
        <#if logicParams??>
            <#list logicParams as logicParam>
        @LogicMapping(name = "${logicParam.name}", paramMappingFile = "${logicParam.mappingFile}"),
            </#list>
        </#if>
    },
    toTargetMappings = {
        <#if logicResults??>
            <#list logicResults as logicResult>
        @TargetMapping(path = "${logicResult.path}", paramMappingFile = "${logicResult.mappingFile}"),
            </#list>
        </#if>
    },
    <#if logicType?? && logicType != ''>logicType = LogicType.${logicType},
    </#if><#if logicPath?? && logicPath != ''>logicPath = "${logicPath}",
    </#if>repositoryFullClassName = "${repository}"
)
public class ${logic}Command {

    <#if idPath?? && idPath != ''>@RepositoryIntegration(
        idPath = "${idPath}"<#if autoCreate?? && autoCreate>,
        ignoreIfParamAbsent = true</#if><#if repositoryOrder?? && repositoryOrder != ''>,
        order = ${repositoryOrder}</#if>)
    </#if>private ${entity} source = new ${entity}();

    private ${entity} target;

    private ErrorInfo error = new ErrorInfo();

    private EventInfo event = new EventInfo();

    <#if models??>
    <#list models as model>

    <#if model.protocol??>@DeriveIntegration(
        <#if model.requestMappingFile?? && model.requestMappingFile != ''>requestMappingFile = "${model.requestMappingFile}",
        </#if><#if model.responseMappingFile?? && model.responseMappingFile != ''>responseMappingFile = "${model.responseMappingFile}",
        </#if><#if model.order?? && model.order != ''>order = ${model.order},
        </#if><#if model.batchFlag?? && model.batchFlag>batchFlag = true,
        </#if><#if model.ignoreIfParamAbsent?? && model.ignoreIfParamAbsent>ignoreIfParamAbsent = true,
        </#if>protocolPath = "${model.protocol}"
    )
    </#if><#if model.batchEvent?? && model.batchEvent>@BatchProperty
    </#if>private ${model.modelType} ${model.concept};
    </#list>
    </#if>
}