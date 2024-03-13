INSERT INTO test_mod_roles_keycloak.capability (id, name, description, resource, action, type, application_id,
                                                folio_permission, created_by, created_date)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', 'foo_item.view', 'foo.item.get - description', 'Foo Item',
        'VIEW', 'DATA', 'test-application-0.0.1', 'foo.item.get', NULL, '2023-10-24 13:39:50.682216'),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', 'foo_item.edit', 'foo.item.put - description', 'Foo Item',
        'EDIT', 'DATA', 'test-application-0.0.1', 'foo.item.put', NULL, '2023-10-24 13:39:50.821494'),
       ('ebdef33e-2958-4401-bf20-2bf2d3f61bd2', 'foo_item.delete', 'foo.item.delete - description', 'Foo Item',
        'DELETE', 'DATA', 'test-application-0.0.1', 'foo.item.delete', NULL, '2023-10-24 13:39:50.822493');

INSERT INTO test_mod_roles_keycloak.capability_endpoint(capability_id, path, method)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', '/foo/items/{id}', 'GET'),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', '/foo/items/{id}', 'PUT'),
       ('dbc35c91-c086-43ec-8a42-1bf8779817fc', '/foo/items/{id}', 'PATCH'),
       ('ebdef33e-2958-4401-bf20-2bf2d3f61bd2', '/foo/items/{id}', 'DELETE');

INSERT INTO test_mod_roles_keycloak.capability_set (id, name, description, resource, folio_permission,
                                                    created_by, created_date, action, type, application_id)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', 'foo_item.view', 'foo_item.view - description', 'Foo Item',
        'foo.item.view', NULL, '2023-10-24 13:39:50.924454', 'VIEW', 'DATA', 'test-application-0.0.1');

INSERT INTO test_mod_roles_keycloak.capability_set_capability(capability_set_id, capability_id)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', '366240cd-fb38-42c1-9e72-1694532ecf06');
