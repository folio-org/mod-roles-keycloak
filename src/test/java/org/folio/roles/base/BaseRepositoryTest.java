package org.folio.roles.base;

import static org.folio.test.TestUtils.readString;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

import lombok.extern.log4j.Log4j2;
import org.folio.roles.configuration.JpaAuditingConfig;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.cql.JpaCqlConfiguration;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.extensions.LogTestMethod;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Log4j2
@IntegrationTest
@EnablePostgres
@DataJpaTest
@LogTestMethod
@DirtiesContext(classMode = AFTER_CLASS)
@Import({JpaCqlConfiguration.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseRepositoryTest {

  @Autowired protected TestEntityManager entityManager;
  @MockitoBean protected FolioExecutionContext folioExecutionContext;

  @AfterAll
  static void afterAll(@Autowired JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(readString("sql/drop-tables.sql"));
  }
}
