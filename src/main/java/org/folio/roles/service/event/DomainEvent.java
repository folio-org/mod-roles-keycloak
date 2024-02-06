package org.folio.roles.service.event;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.folio.spring.FolioExecutionContext;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainEvent<T> {

  private final T newObject;
  private final T oldObject;
  private final DomainEventType type;
  private FolioExecutionContext context;

  public static <T> DomainEvent<T> created(T newObject) {
    return new DomainEvent<>(newObject, null, DomainEventType.CREATE);
  }

  public static <T> DomainEvent<T> updated(T newObject, T oldObject) {
    return new DomainEvent<>(newObject, oldObject, DomainEventType.UPDATE);
  }

  public static <T> DomainEvent<T> deleted(T oldObject) {
    return new DomainEvent<>(null, oldObject, DomainEventType.DELETE);
  }

  public DomainEvent<T> withContext(FolioExecutionContext context) {
    this.context = context;
    return this;
  }
}
