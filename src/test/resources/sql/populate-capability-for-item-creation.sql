INSERT INTO test_mod_roles_keycloak.capability_set
  (id, name, description, resource, action, type, application_id, created_by)
VALUES ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo_item.create',
        'Capability to create a foo item', 'Foo Item', 'CREATE', 'DATA', 'foo-application-1.0.0',
        '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO test_mod_roles_keycloak.capability_permissions(capability_id, permission)
VALUES ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo.item.post');
