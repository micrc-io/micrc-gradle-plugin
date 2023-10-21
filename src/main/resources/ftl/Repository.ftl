package ${basePackage}.domain.${aggregationPackage}.integration;

import ${basePackage}.domain.${aggregationPackage}.${modelName};
import ${basePackage}.domain.${aggregationPackage}.valobj.*;
import io.micrc.core.persistence.MicrcJpaRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@CacheConfig(
    cacheManager = "redisCacheManager",
    cacheResolver = "redisRepositoryCacheResolver",
    keyGenerator = "repositoryQueryKeyGenerator",
    cacheNames = { "${modelName}-Repository" }
)
@Repository
public interface ${modelName}Repository extends MicrcJpaRepository<${modelName}, ${modelIdentityName}> {

<#if rules??>
    <#list rules as rule>
        ${rule.resultType} ${rule.name}(
        <#if rule.params??>
            <#list rule.params as param>
                ${param.type} param${param.index}<#if rule.params?size - 1 gt param.index>,</#if>
            </#list>
        </#if>
        );
    </#list>
</#if>

}