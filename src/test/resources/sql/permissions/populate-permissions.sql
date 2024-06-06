SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO permission(id, name, display_name, description, sub_permissions)
VALUES ('6872d8c2-e17b-4dfd-b24e-3e8c34b3558a', 'foo.item.view', 'foo_item.view - display name',
        'Capability set to view a foo item', '{"foo.item.get"}'),
       ('ec7d16bd-62c9-411d-8195-ac6dc1f96f37', 'foo.item.edit', 'foo_item.edit - display name',
        'Capability set to create a foo item', '{"foo.item.view", "foo.item.put"}'),
       ('2a29cc30-57c5-44a3-acff-6cad5e52037b', 'foo.item.create', 'foo_item.create - display name',
        'Capability set to create a foo item', '{"foo.item.edit", "foo.item.post"}'),
       ('e08d0ffb-5f12-49f0-b57f-84a2d68eddd8', 'foo.item.delete', 'foo_item.delete - display name',
        'Capability to delete a foo item', '{"foo.item.delete", "foo.item.post"}'),
       ('8dfa7465-1575-4c63-bc2b-de5afd30a36b', 'foo.item.all', 'foo_item.manage - display name',
        'Capability set to manage a foo item', '{"foo.item.view", "foo.item.edit", "foo.item.create", "foo.item.delete"}')
