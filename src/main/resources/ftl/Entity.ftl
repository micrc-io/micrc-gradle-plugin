package ${basePackage}.domain.${aggregationPackage};

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.vladmihalcea.hibernate.type.json.JsonType;
import ${basePackage}.domain.${aggregationPackage}.valobj.*;
import lombok.Data;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@OptimisticLocking
@Table(name = "${tableName}")
@TypeDef(name = "json", typeClass = JsonType.class)
public class ${modelName} implements Serializable {

<#if properties??>
    <#list properties as property>
    <#if property.dataType?? && property.dataType == 'identity'>@EmbeddedId
    </#if><#if property.dataType?? && property.dataType == 'json'>@Column(columnDefinition = "json")
    @Type(type = "json")
    </#if><#if property.dataType?? && property.dataType == 'embedded'>@Embedded
    </#if><#if property.dataType?? && property.dataType == 'version'>@Version
    </#if><#if property.dataType?? && property.dataType == 'transient'>@Transient
    </#if>private ${property.modelType} ${property.name}<#if property.dataType?? && property.dataType == 'identity'> = new ${property.modelType}()</#if><#if property.isList?? && property.isList == 'true'> = new ArrayList<>()</#if>;
<#if property.dataType?? && property.dataType == 'version'>    @PrePersist
    public void prePersist() {
        version = 0;
    }</#if>
    </#list>
</#if>
<#if oneType??>

    @ManyToOne
    @JoinColumn(name = "${joinColumn}")
    @JsonBackReference
    private ${oneType} ${oneName};
</#if>
<#if manyType??>

    @OneToMany(mappedBy = "${mappedBy}", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<${manyType}> ${manyName};
</#if>
<#if manyTypeUnidirectional??>

    @OneToMany(targetEntity = ${manyTypeUnidirectional}.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "${joinColumnUnidirectional}")
    private List<${manyTypeUnidirectional}> ${manyNameUnidirectional};
</#if>
}