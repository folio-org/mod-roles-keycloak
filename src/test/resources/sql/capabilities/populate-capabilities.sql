SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO capability(id, name, description, resource, action, type, application_id, folio_permission)
VALUES ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', 'foo_item.view',
        'Capability to view a foo item', 'Foo Item', 'VIEW', 'DATA',
        'test-application-0.0.1', 'foo.item.get'),

       ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', 'foo_item.create',
        'Capability to create a foo item', 'Foo Item', 'CREATE', 'DATA',
        'test-application-0.0.1', 'foo.item.post'),

       ('78d6a59f-90ab-46a1-a349-4d25d0798763', 'foo_item.edit',
        'Capability to edit a foo item', 'Foo Item', 'EDIT', 'DATA',
        'test-application-0.0.1', 'foo.item.put'),

       ('ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db', 'foo_item.delete',
        'Capability to delete a foo item', 'Foo Item', 'DELETE', 'DATA',
        'test-application-0.0.1', 'foo.item.delete'),

       ('e1a5683a-fece-43fb-bbaa-52a438af9807', 'foo_item.manage',
        'Capability to manage a foo item', 'Foo Item', 'MANAGE', 'DATA',
        'test-application-0.0.1', 'foo.item.all');

INSERT INTO capability_endpoint(capability_id, path, method)
VALUES ('e2628d7d-059a-46a1-a5ea-10a5a37b1af2', '/foo/items/{id}', 'GET'),
       ('8d2da27c-1d56-48b6-958d-2bfae6d79dc8', '/foo/items', 'POST'),
       ('78d6a59f-90ab-46a1-a349-4d25d0798763', '/foo/items/{id}', 'PUT'),
       ('ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db', '/foo/items/{id}', 'DELETE'),
       ('e1a5683a-fece-43fb-bbaa-52a438af9807', '/foo/items/{id}', 'GET'),
       ('e1a5683a-fece-43fb-bbaa-52a438af9807', '/foo/items', 'POST'),
       ('e1a5683a-fece-43fb-bbaa-52a438af9807', '/foo/items/{id}', 'PUT'),
       ('e1a5683a-fece-43fb-bbaa-52a438af9807', '/foo/items/{id}', 'DELETE');

INSERT INTO capability(id, name, description, resource, action, type, application_id, folio_permission)
VALUES ('48e57f3b-3622-43db-b437-5d30ebe8f867', 'ui_foo_item.view',
        'Capability to view a ui foo item', 'UI Foo Item', 'VIEW', 'DATA',
        'test-application-0.0.1', 'plugin.foo.item.get'),

       ('f491047c-32eb-4736-815c-ebb8e94dffac', 'ui_foo_item.create',
        'Capability to create a ui foo item', 'UI Foo Item', 'CREATE', 'DATA',
        'test-application-0.0.1', 'module.foo.item.post'),

       ('af9a59c5-ba1d-47df-82f0-6dd3cef2b25e', 'ui_foo_item.edit',
        'Capability to edit a ui foo item', 'UI Foo Item', 'EDIT', 'DATA',
        'test-application-0.0.1', 'ui-foo.item.put'),

       ('5d764bb8-b5e5-4f33-8640-23eb9732b438', 'ui_foo_item.delete',
        'Capability to delete a ui foo item', 'UI Foo Item', 'DELETE', 'DATA',
        'test-application-0.0.1', 'ui-foo.item.delete');
