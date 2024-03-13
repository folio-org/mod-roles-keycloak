SET SEARCH_PATH = 'test_mod_roles_keycloak';

-- Foo Item | Manage
INSERT INTO capability_set(id, name, description, resource, action, type, folio_permission, application_id, created_by)
VALUES ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'foo_item.manage', 'Capability set to manage a foo item',
        'Foo Item', 'MANAGE', 'DATA', 'foo.item.all', 'test-application-0.0.1', '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', '8d2da27c-1d56-48b6-958d-2bfae6d79dc8'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', '78d6a59f-90ab-46a1-a349-4d25d0798763'),
       ('a1002e06-a2bc-4ce4-9d71-e25db1250e09', 'ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db');

-- Foo Item | Edit
INSERT INTO capability_set(id, name, description, resource, action, type, folio_permission, application_id)
VALUES ('6532d4f8-3e97-4d8b-886f-4ec2a2adc4a3', 'foo_item.edit', 'Capability set to edit a foo item',
        'Foo Item', 'EDIT', 'DATA', 'foo.item.edit', 'test-application-0.0.1');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('6532d4f8-3e97-4d8b-886f-4ec2a2adc4a3', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2'),
       ('6532d4f8-3e97-4d8b-886f-4ec2a2adc4a3', '78d6a59f-90ab-46a1-a349-4d25d0798763');

-- Foo Item | Create
INSERT INTO capability_set(id, name, description, resource, action, type, folio_permission, application_id, created_by)
VALUES ('55a910de-cecf-4e0e-9d35-2e8e2ecf699e', 'foo_item.create', 'Capability set to create a foo item',
        'Foo Item', 'CREATE', 'DATA', 'foo.item.create',
        'test-application-0.0.1', '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('55a910de-cecf-4e0e-9d35-2e8e2ecf699e', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2'),
       ('55a910de-cecf-4e0e-9d35-2e8e2ecf699e', '8d2da27c-1d56-48b6-958d-2bfae6d79dc8');

-- Bar Item | Create
INSERT INTO capability_set(id, name, description, resource, action, type, folio_permission, application_id, created_by)
VALUES ('4fdc76d8-efdf-4ffa-b231-8c7bbbb62939', 'ui_foo_item.create', 'Capability set to create a ui foo item',
        'UI Foo Item', 'CREATE', 'DATA', 'ui-foo.item.create',
        'test-application-0.0.1', '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('4fdc76d8-efdf-4ffa-b231-8c7bbbb62939', '48e57f3b-3622-43db-b437-5d30ebe8f867'),
       ('4fdc76d8-efdf-4ffa-b231-8c7bbbb62939', 'f491047c-32eb-4736-815c-ebb8e94dffac');

-- UI Foo Item | Edit
INSERT INTO capability_set(id, name, description, resource, action, type, folio_permission, application_id, created_by)
VALUES ('ec87daa4-47e5-48da-beae-603dfaaa128a', 'ui_foo_item.edit', 'Capability set to edit a ui foo item',
        'UI Foo Item', 'EDIT', 'DATA', 'ui-foo.item.edit',
        'test-application-0.0.1', '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('ec87daa4-47e5-48da-beae-603dfaaa128a', '48e57f3b-3622-43db-b437-5d30ebe8f867'),
       ('ec87daa4-47e5-48da-beae-603dfaaa128a', 'af9a59c5-ba1d-47df-82f0-6dd3cef2b25e');
