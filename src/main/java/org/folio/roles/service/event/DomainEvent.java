package org.folio.roles.service.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.folio.spring.FolioExecutionContext;

@Getter
@ToString
@EqualsAndHashCode
public final class DomainEvent<T> {

  private final T newObject;
  private final T oldObject;
  private final DomainEventType type;
  private Long timestamp;
  @ToString.Exclude
  private FolioExecutionContext context;

  private DomainEvent(T newObject, T oldObject, DomainEventType type) {
    this.newObject = newObject;
    this.oldObject = oldObject;
    this.type = type;
    this.timestamp = System.currentTimeMillis();
  }

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

  public DomainEvent<T> withTimestamp(Long timestamp) {
    this.timestamp = timestamp;
    return this;
  }
}
