SET SEARCH_PATH = 'test_mod_roles_keycloak';

-- role1
INSERT INTO role(id, name, description)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'role1', 'a role1 description');

--- with users: user1, user2, user4
INSERT INTO user_role(user_id, role_id)
VALUES ('cf078e4a-5d9c-45f1-9c1d-f87003790d9f', '1e985e76-e9ca-401c-ad8e-0d121a11111e'),
       ('9d30bb2b-8c6d-47da-9726-0e067b65f30b', '1e985e76-e9ca-401c-ad8e-0d121a11111e'),
       ('c2bdde31-e216-43f7-abe3-54b6415d7472', '1e985e76-e9ca-401c-ad8e-0d121a11111e');

--- with capabilities: foo_item.delete, ui_foo_item.delete
INSERT INTO role_capability(role_id, capability_id)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db'),
       ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '5d764bb8-b5e5-4f33-8640-23eb9732b438');

--- with capability_sets: foo_item.edit, foo_item.create
INSERT INTO role_capability_set(role_id, capability_set_id)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '6532d4f8-3e97-4d8b-886f-4ec2a2adc4a3'),
       ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '55a910de-cecf-4e0e-9d35-2e8e2ecf699e');

--
--user1
--- with capabilities: ui_foo_item.edit
INSERT INTO user_capability (user_id, capability_id)
VALUES ('cf078e4a-5d9c-45f1-9c1d-f87003790d9f', 'af9a59c5-ba1d-47df-82f0-6dd3cef2b25e');

--- with capability sets: ui_foo_item.create
INSERT INTO user_capability_set(user_id, capability_set_id)
VALUES ('cf078e4a-5d9c-45f1-9c1d-f87003790d9f', '4fdc76d8-efdf-4ffa-b231-8c7bbbb62939');

--
-- role2
INSERT INTO role(id, name, description)
VALUES ('e7545202-0c1c-4a64-9b45-747c452e0129', 'role2', 'a role2 description');

--- with users: user2, user5
INSERT INTO user_role(user_id, role_id)
VALUES ('9d30bb2b-8c6d-47da-9726-0e067b65f30b', 'e7545202-0c1c-4a64-9b45-747c452e0129'),
       ('1cc7f14f-28f9-4110-be6c-51d782d32ba4', 'e7545202-0c1c-4a64-9b45-747c452e0129');

--- with capabilities: foo_item.delete, ui_foo_item.view
INSERT INTO role_capability(role_id, capability_id)
VALUES ('e7545202-0c1c-4a64-9b45-747c452e0129', 'ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db'),
       ('e7545202-0c1c-4a64-9b45-747c452e0129', '48e57f3b-3622-43db-b437-5d30ebe8f867');

--- with capability_sets: foo_item.manage, ui_foo_item.edit
INSERT INTO role_capability_set(role_id, capability_set_id)
VALUES ('e7545202-0c1c-4a64-9b45-747c452e0129', 'a1002e06-a2bc-4ce4-9d71-e25db1250e09'),
       ('e7545202-0c1c-4a64-9b45-747c452e0129', 'ec87daa4-47e5-48da-beae-603dfaaa128a');

--
-- user3 (has only directly assigned capabilities)
--- with capabilities: ui_foo_item.delete, ui_foo_item.create
INSERT INTO user_capability (user_id, capability_id)
VALUES ('bd41e413-21c6-4755-a11c-18da7780e02f', '5d764bb8-b5e5-4f33-8640-23eb9732b438'),
       ('bd41e413-21c6-4755-a11c-18da7780e02f', 'f491047c-32eb-4736-815c-ebb8e94dffac');

--- with capability sets: ui_foo_item.edit, ui_foo_item.create
INSERT INTO user_capability_set(user_id, capability_set_id)
VALUES ('bd41e413-21c6-4755-a11c-18da7780e02f', 'ec87daa4-47e5-48da-beae-603dfaaa128a'),
       ('bd41e413-21c6-4755-a11c-18da7780e02f', '4fdc76d8-efdf-4ffa-b231-8c7bbbb62939');
