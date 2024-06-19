INSERT INTO test_mod_roles_keycloak.role(id, name, description, created_date, created_by_user_id,
                                         updated_date, updated_by_user_id)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'role1', 'role1_description', '2023-01-01 12:01:01',
'11111111-2222-1111-2222-111111111111', '2023-01-02 12:01:01', '11111111-1111-2222-1111-111111111111');

-- 'Foo Item'
INSERT INTO test_mod_roles_keycloak.capability
  (id, name, description, resource, action, type, created_by_user_id, application_id, direct_parent_ids, all_parent_ids)
VALUES ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', 'foo_item.view',
        'Capability to view a foo item', 'Foo Item', 'VIEW', 'DATA',
        '11111111-1111-4011-1111-0d121a11111e', 'foo-application-1.0.0',
        '{"78d6a59f-90ab-46a1-a349-4d25d0798763", "ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db"}',
        '{"78d6a59f-90ab-46a1-a349-4d25d0798763", "ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db", "a1002e06-a2bc-4ce4-9d71-e25db1250e09"}'),
       ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo_item.create',
        'Capability to create a foo item', 'Foo Item', 'CREATE', 'DATA',
        '11111111-1111-4011-1111-0d121a11111e', 'foo-application-1.0.0',
        '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}', '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}'),
       ('78d6a59f-90ab-46a1-a349-4d25d0798763', 'foo_item.edit',
        'Capability to edit a foo item', 'Foo Item', 'EDIT', 'DATA',
        '11111111-1111-4011-1111-0d121a11111e', 'foo-application-1.0.0',
        '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}', '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}'),
       ('ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db', 'foo_item.delete',
        'Capability to delete a foo item', 'Foo Item', 'DELETE', 'DATA',
        '11111111-1111-4011-1111-0d121a11111e', 'foo-application-1.0.0',
        '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}', '{"a1002e06-a2bc-4ce4-9d71-e25db1250e09"}'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'foo_item.manage',
        'Capability to manage a foo item', 'Foo Item', 'MANAGE', 'DATA',
        '11111111-1111-4011-1111-0d121a11111e', 'foo-application-1.0.0',
        null, null);

INSERT INTO test_mod_roles_keycloak.capability_permissions(capability_id, permission)
VALUES ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', 'foo.item.get'),
       ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo.item.post'),
       ('78d6a59f-90ab-46a1-a349-4d25d0798763', 'foo.item.put'),
       ('ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db', 'foo.item.delete');

INSERT INTO test_mod_roles_keycloak.role_capability(role_id, capability_id)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2'),
       ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '8d2da27c-1d56-48b6-958d-2bfae6d79dc8');

-- 'Bar Item'
INSERT INTO test_mod_roles_keycloak.capability
  (id, name, description, resource, action, type, created_by_user_id, application_id, direct_parent_ids, all_parent_ids)
VALUES ('26909777-62b7-485d-bf03-9fe35eb29e49', 'bar_item.view',
        'Capability to view a bar item', 'Bar Item', 'VIEW', 'DATA', '11111111-1111-4011-1111-0d121a11111e',
        'foo-application-1.0.0', null, null);

INSERT INTO test_mod_roles_keycloak.capability_permissions(capability_id, permission)
VALUES ('26909777-62b7-485d-bf03-9fe35eb29e49', 'bar.item.get');
