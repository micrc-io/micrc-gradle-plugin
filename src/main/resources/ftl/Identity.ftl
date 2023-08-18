package ${basePackage}.domain.${aggregationPackage}.valobj;

import io.micrc.core.persistence.IdentityAware;
import lombok.Data;

import java.io.Serializable;

@Data
public class ${modelName} implements Serializable, IdentityAware {

    private Long id;

    @Override
    public void setIdentity(long id) {
        this.id = id;
    }
}
