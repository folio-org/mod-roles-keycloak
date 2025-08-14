SET SEARCH_PATH = 'test_mod_roles_keycloak';

-- Create a capability set that is has a wrong folio_permission
INSERT INTO capability_set (id, name, resource, action, type, folio_permission, application_id, module_id)
VALUES ('88888888-8888-8888-8888-888888888888',
        'test_capability-set_all.execute',
        'Test Capability Set',
        'MANAGE',
        'DATA',
        'wrong.capability-set.permission', -- this is wrong, it should be 'test.capability-set.all'
        'test-app-1.0.0',
        'test-module-1.0.0');

-- Create a capability that is part of the capability set and has a wrong folio_permission
INSERT INTO capability (id, name, resource, action, type, folio_permission, dummy_capability, application_id, module_id)
VALUES ('77777777-7777-7777-7777-777777777777', -- corrupted "real" capability
        'test_sub-capability_view.execute',
        'Dummy Resource',
        'VIEW',
        'DATA',
        'wrong.capability.permission', -- this is wrong, it should be 'test.sub-capability.view'
        false,
        'test-app-1.0.0',
        'test-module-1.0.0'),
        ('66666666-6666-6666-6666-666666666666', -- technical capability for the capability set
        'test_capability-set_all.execute',
        'Test Capability Set',
        'MANAGE',
        'DATA',
        'wrong.capability-set.permission',
        false,
        'test-app-1.0.0',
        'test-module-1.0.0'),
        ('55555555-5555-5555-5555-555555555555', -- corrupted "dummy" capability
        'test_dummy_capability_view.execute',
        'Dummy Resource',
        'VIEW',
        'DATA',
        'wrong.dummy.capability.view', -- this is wrong, it should be 'test.dummy.capability.view'
        true,
        'test-app-1.0.0',
        'test-module-1.0.0');
