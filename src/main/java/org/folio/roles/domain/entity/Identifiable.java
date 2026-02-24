package org.folio.roles.domain.entity;

import org.springframework.lang.Nullable;

public interface Identifiable<K> {

  @Nullable
  K getId();

  void setId(@Nullable K id);
}
