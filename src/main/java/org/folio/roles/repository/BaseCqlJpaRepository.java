package org.folio.roles.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseCqlJpaRepository<T, I> extends JpaCqlRepository<T, I> {

  /**
   * Returns all records if query is empty or searches using it otherwise.
   *
   * @param query - CQL query as {@link String}
   * @param pageable - {@link Pageable} object for pagination
   * @return {@link Page} containing {@link T} records
   */
  default Page<T> findByQuery(String query, Pageable pageable) {
    return isBlank(query) ? findAll(pageable) : findByCql(query, pageable);
  }
}
