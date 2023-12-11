package org.folio.roles.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.mapstruct.Mapping;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "metadata.createdDate", source = "entity.createdDate")
@Mapping(target = "metadata.createdBy", source = "entity.createdBy")
@Mapping(target = "metadata.modifiedDate", source = "entity.updatedDate")
@Mapping(target = "metadata.modifiedBy", source = "entity.updatedBy")
public @interface AuditableMapping {
}

