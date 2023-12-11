INSERT INTO test_mod_roles_keycloak.role(id, name, description, created_date, created_by, updated_date, updated_by)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'role1', 'role1_description', '2023-01-01 12:01:01',
        '11111111-2222-1111-2222-111111111111', '2023-01-02 12:01:01', '11111111-1111-2222-1111-111111111111');

INSERT INTO test_mod_roles_keycloak.capability
  (id, name, description, resource, action, type, application_id, created_by)
VALUES ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo_item.create',
        'Capability to create a foo item', 'Foo Item', 'CREATE', 'DATA',
        'foo-application-1.0.0', '11111111-1111-4011-1111-0d121a11111e'),
        ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', 'foo_item.view',
        'Capability to view a foo item', 'Foo Item', 'VIEW', 'DATA',
         'foo-application-1.0.0', '11111111-1111-4011-1111-0d121a11111e'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'foo_item.edit',
        'Capability to edit a foo item', 'Foo Item', 'EDIT', 'DATA',
        'foo-application-1.0.0', '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO test_mod_roles_keycloak.capability_permissions(capability_id, permission)
VALUES ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo.item.post'),
       ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', 'foo.item.get'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'foo.item.put');

INSERT INTO test_mod_roles_keycloak.role_capability(role_id, capability_id)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '8d2da27c-1d56-48b6-958d-2bfae6d79dc8'),
       ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2');
