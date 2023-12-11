package org.folio.roles.repository.generators;

import java.lang.reflect.Member;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.id.uuid.UuidGenerator;

public class FolioUuidGeneratorImpl extends UuidGenerator {

  /**
   * Default constructor for {@link FolioUuidGeneratorImpl}, used by hibernate.
   *
   * @param config - {@link FolioUuidGenerator} annotation configuration
   * @param idMember - id member as {@link Member}
   * @param creationContext - creation context as {@link CustomIdGeneratorCreationContext}
   */
  @SuppressWarnings("unused")
  public FolioUuidGeneratorImpl(FolioUuidGenerator config,
    Member idMember, CustomIdGeneratorCreationContext creationContext) {
    super(config.uuidGenerator(), idMember, creationContext);
  }

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
    var id = session.getEntityPersister(null, object).getIdentifier(object, session);
    return id == null ? super.generate(session, object) : id;
  }
}
