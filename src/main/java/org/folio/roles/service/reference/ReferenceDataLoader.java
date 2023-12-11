package org.folio.roles.service.reference;

/**
 * Loads reference data.  This interface should be implemented by any class that loads reference data.
 */
public interface ReferenceDataLoader {

  /**
   * Base directory for reference data.
   */
  String BASE_DIR = "reference-data/";

  /**
   * Loads reference data. This method should be idempotent.
   * If the data already exists, it should be updated.
   * If it does not exist, it should be created.
   */
  void loadReferenceData();
}
