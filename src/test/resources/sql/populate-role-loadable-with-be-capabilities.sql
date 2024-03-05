SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO role (id, name, description, created_by, created_date, updated_by, updated_date)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'Circulation Manager', 'Role for Circulation Manager',
          null, '2024-02-24 12:00:00+00:00', null, null),
         ('c14cfe6f-b971-4117-884c-7b5efd1cf076', 'Circulation Student', 'Role for Circulation Student',
          null, '2024-02-24 12:00:00+00:00', null, null);

INSERT INTO role_loadable (id, type)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'DEFAULT'),
         ('c14cfe6f-b971-4117-884c-7b5efd1cf076', 'DEFAULT');

INSERT INTO role_loadable_permission (role_loadable_id, folio_permission, capability_id, capability_set_id,
                                      created_by, created_date, updated_by, updated_date)
  VALUES ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'notes.item.post', null, null, null, '2024-02-24 12:00:00+00:00', null, null),
        ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'notes.item.get', null, null, null, '2024-02-24 12:00:00+00:00', null, null),
        ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'notes.item.put', null, null, null, '2024-02-24 12:00:00+00:00', null, null),
        ('5a3a3b6d-ea37-4faf-98fe-91ded163a89e', 'notes.item.delete', null, null, null, '2024-02-24 12:00:00+00:00', null, null),
        ('c14cfe6f-b971-4117-884c-7b5efd1cf076', 'notes.item.get', null, null, null, '2024-02-24 12:00:00+00:00', null, null);
