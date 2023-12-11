package org.folio.roles.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.mapstruct.Mapping;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "createdDate", ignore = true)
@Mapping(target = "createdBy", ignore = true)
@Mapping(target = "updatedDate", ignore = true)
@Mapping(target = "updatedBy", ignore = true)
public @interface AuditableEntityMapping {}
