SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO role (id, name, description, type, created_by_user_id, created_date, updated_by_user_id, updated_date)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'Circulation Manager', 'Role for Circulation Manager', 'DEFAULT',
          null, '2024-02-24 12:00:00+00:00', null, '2024-02-24 12:00:00+00:00');

INSERT INTO role_loadable (id)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e');

INSERT INTO role_loadable_permission (role_loadable_id, folio_permission, capability_id, capability_set_id,
                                      created_by_user_id, created_date, updated_by_user_id, updated_date)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'technical.item.get', null, null, null, '2024-02-24 12:00:00+00:00', null, '2024-02-24 12:00:00+00:00');
