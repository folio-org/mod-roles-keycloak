package org.folio.roles.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Guards a native-image constraint that <b>no JVM test can reproduce</b>.
 *
 * <p>The native build runs Hibernate with the {@code none} bytecode provider (ByteBuddy is excluded because it
 * defines classes at runtime, which GraalVM forbids). Under {@code none}, Hibernate cannot create a
 * {@code HibernateProxy} — and a <em>lazy to-one</em> association makes it try to do exactly that whenever the
 * owning entity is <em>loaded</em>, not merely navigated. The result is a runtime
 * {@code JpaSystemException: Generation of HibernateProxy instances at runtime is not allowed …} that only ever
 * appears in the native image (on the JVM a real bytecode provider is always present, and Hibernate 7 ignores the
 * {@code hibernate.bytecode.provider} property, so it cannot be forced off in tests).</p>
 *
 * <p>This has already cost two production round-trips, so it is asserted statically instead. Lazy
 * <b>collections</b> are deliberately allowed: they use {@code PersistentSet}/{@code PersistentBag} wrappers rather
 * than generated proxies and work fine under {@code none}.</p>
 */
@UnitTest
class LazyToOneAssociationGuardTest {

  private static final String ENTITY_PACKAGE = "org.folio.roles.domain.entity";

  @Test
  void entityMappings_positive_haveNoLazyToOneAssociations() {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
    scanner.addIncludeFilter(new AnnotationTypeFilter(MappedSuperclass.class));

    var violations = new ArrayList<String>();
    for (var candidate : scanner.findCandidateComponents(ENTITY_PACKAGE)) {
      var type = ClassUtils.resolveClassName(candidate.getBeanClassName(), null);
      for (Field field : type.getDeclaredFields()) {
        if (isLazyToOne(field)) {
          violations.add(type.getSimpleName() + "." + field.getName());
        }
      }
    }

    assertThat(violations)
      .as("lazy @ManyToOne/@OneToOne associations break the native image (BytecodeProvider 'none' cannot generate "
        + "a HibernateProxy when the owning entity is loaded) — use FetchType.EAGER, or adopt build-time Hibernate "
        + "bytecode enhancement for the whole model")
      .isEmpty();
  }

  /**
   * Sanity check that the scan actually resolves the entity model, so the guard above cannot pass vacuously if the
   * package is renamed or the scanner stops finding candidates.
   */
  @Test
  void entityScan_positive_findsEntityClasses() {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

    List<String> entities = scanner.findCandidateComponents(ENTITY_PACKAGE).stream()
      .map(candidate -> ClassUtils.resolveClassName(candidate.getBeanClassName(), null).getSimpleName())
      .toList();

    assertThat(entities).contains("LoadablePermissionEntity", "LoadableRoleEntity", "RoleEntity");
  }

  private static boolean isLazyToOne(Field field) {
    var manyToOne = field.getAnnotation(ManyToOne.class);
    if (manyToOne != null && manyToOne.fetch() == FetchType.LAZY) {
      return true;
    }

    var oneToOne = field.getAnnotation(OneToOne.class);
    return oneToOne != null && oneToOne.fetch() == FetchType.LAZY;
  }
}
