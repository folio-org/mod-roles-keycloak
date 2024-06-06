SET SEARCH_PATH = "test_mod_roles_keycloak";

INSERT INTO capability (id, name, description, resource, action, type, application_id,
                        folio_permission, created_by, created_date)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', 'foo_item.view', 'foo.item.get - description', 'Foo Item',
        'VIEW', 'DATA', 'test-application-0.0.1', 'foo.item.get', NULL, '2023-10-24 13:39:50.682216');

INSERT INTO capability_endpoint(capability_id, path, method)
VALUES ('366240cd-fb38-42c1-9e72-1694532ecf06', '/foo/items/{id}', 'GET');

INSERT INTO capability_set (id, name, description, resource, folio_permission,
                            created_by, created_date, action, type, application_id)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', 'foo_item.view', 'foo_item.view - description', 'Foo Item',
        'foo.item.view', NULL, '2023-10-24 13:39:50.924454', 'VIEW', 'DATA', 'test-application-0.0.1');

INSERT INTO capability_set_capability(capability_set_id, capability_id)
VALUES ('60a691b2-d5ee-4845-84f7-b3f68ab6ec13', '366240cd-fb38-42c1-9e72-1694532ecf06');

INSERT INTO user_capability(user_id, capability_id, created_by, created_date)
VALUES ('3e8647ee-2a23-4ca4-896b-95476559c567', '60a691b2-d5ee-4845-84f7-b3f68ab6ec13',
        '11111111-2222-1111-2222-111111111111', '2023-09-05 15:00:00')
