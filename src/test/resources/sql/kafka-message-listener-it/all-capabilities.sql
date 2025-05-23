SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO capability (id, name, description, resource, action, type, application_id,
                                                folio_permission, module_id, created_by_user_id, created_date, visible)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', 'foo_item.view', 'foo_item.view - description', 'Foo Item',
        'VIEW', 'DATA', 'test-application-0.0.1', 'foo.item.get', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:50', false),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', 'foo_item.edit', 'foo_item.edit - description', 'Foo Item',
        'EDIT', 'DATA', 'test-application-0.0.1', 'foo.item.put', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:51', false),
       ('35137e56-2689-4245-9588-ca2ef43d7aff', 'foo_item.create', 'foo_item.create - description', 'Foo Item',
        'CREATE', 'DATA', 'test-application-0.0.1', 'foo.item.post', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:52', false),
       ('ebdef33e-2958-4401-bf20-2bf2d3f61bd2', 'foo_item.delete', 'foo_item.delete - description', 'Foo Item',
        'DELETE', 'DATA', 'test-application-0.0.1', 'foo.item.delete', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:53', false),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', 'foo_item.manage', 'foo_item.manage - description', 'Foo Item',
        'MANAGE', 'DATA', 'test-application-0.0.1', 'foo.item.all', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:53', false);

INSERT INTO capability_endpoint(capability_id, path, method)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', '/foo/items/{id}', 'GET'),
       ('35137e56-2689-4245-9588-ca2ef43d7aff', '/foo/items', 'POST'),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', '/foo/items/{id}', 'PUT'),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', '/foo/items/{id}', 'PATCH'),
       ('ebdef33e-2958-4401-bf20-2bf2d3f61bd2', '/foo/items/{id}', 'DELETE');

INSERT INTO capability_set(id, name, description, resource, folio_permission, module_id,
                           created_by_user_id, created_date, action, type, application_id, visible)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', 'foo_item.view', 'foo_item.view - description', 'Foo Item',
        'foo.item.view', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:51', 'VIEW', 'DATA', 'test-application-0.0.1', false),
       ('3d159575-2533-4351-94b0-2a4255c05c2c', 'foo_item.edit', 'foo_item.edit - description', 'Foo Item',
        'foo.item.edit', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:52', 'EDIT', 'DATA', 'test-application-0.0.1', false),
       ('12277da3-d05d-41de-ae74-0e8ec59f1dcc', 'foo_item.create', 'foo_item.create - description', 'Foo Item',
        'foo.item.create', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:53', 'CREATE', 'DATA', 'test-application-0.0.1', false),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', 'foo_item.manage', 'foo_item.manage - description', 'Foo Item',
        'foo.item.all', 'foo-module-1.0.0', NULL, '2023-10-24 13:39:54', 'MANAGE', 'DATA', 'test-application-0.0.1', false);

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', '366240cd-fb38-42c1-9e72-1694532ecf06'),
       ('3d159575-2533-4351-94b0-2a4255c05c2c', '366240cd-fb38-42c1-9e72-1694532ecf06'),
       ('3d159575-2533-4351-94b0-2a4255c05c2c', 'dbc35c91-c086-43ec-8a42-1bf8779817fc'),
       ('12277da3-d05d-41de-ae74-0e8ec59f1dcc', '366240cd-fb38-42c1-9e72-1694532ecf06'),
       ('12277da3-d05d-41de-ae74-0e8ec59f1dcc', '35137e56-2689-4245-9588-ca2ef43d7aff'),
       ('12277da3-d05d-41de-ae74-0e8ec59f1dcc', 'dbc35c91-c086-43ec-8a42-1bf8779817fc'),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', '366240cd-fb38-42c1-9e72-1694532ecf06'),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', 'dbc35c91-c086-43ec-8a42-1bf8779817fc'),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', '35137e56-2689-4245-9588-ca2ef43d7aff'),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', 'ebdef33e-2958-4401-bf20-2bf2d3f61bd2'),
       ('8b117697-ecf3-47d8-84c9-d5d1734e0237', '8b117697-ecf3-47d8-84c9-d5d1734e0237');
