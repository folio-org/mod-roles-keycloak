package org.folio.roles.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.mapstruct.Mapping;

@Retention(RetentionPolicy.CLASS)
@Mapping(target = "createdDate", ignore = true)
@Mapping(target = "createdByUserId", ignore = true)
@Mapping(target = "updatedDate", ignore = true)
@Mapping(target = "updatedByUserId", ignore = true)
public @interface AuditableEntityMapping {}
