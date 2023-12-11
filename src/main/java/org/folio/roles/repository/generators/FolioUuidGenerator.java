package org.folio.roles.repository.generators;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

@Target({FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
@IdGeneratorType(FolioUuidGeneratorImpl.class)
public @interface FolioUuidGenerator {

  /**
   * Uuid generator definition.
   *
   * @return {@link UuidGenerator} annotation
   */
  UuidGenerator uuidGenerator() default @UuidGenerator(style = Style.AUTO);
}
