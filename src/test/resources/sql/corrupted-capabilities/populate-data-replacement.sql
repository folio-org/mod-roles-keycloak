SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO capability (id, name, resource, action, type, folio_permission, dummy_capability, application_id, module_id)
VALUES ('00000000-0000-0000-0000-000000000002',
        'old-feature.item.view',
        'Old Feature Item',
        'VIEW',
        'DATA',
        'old-feature.item.view', -- correct permission
        false,
        'old-module-1.0.0',
        'old-app-1.0.0');

-- The bug caused its 'folio_permission' to point to the OLD feature's permission
INSERT INTO capability (id, name, resource, action, type, folio_permission, dummy_capability, application_id, module_id)
VALUES ('00000000-0000-0000-0000-000000000001',
        'new-feature.item.view', -- The name is for a new feature
        'Dummy New Feature',
        'VIEW',
        'DATA',
        'old-feature.item.view', -- this is wrong folio_permission. It should be 'new-feature.item.view'
        true,
        'new-module-1.0.0',
        'new-app-1.0.0');
