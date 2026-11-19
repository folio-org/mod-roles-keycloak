SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO role(id, name, description)
VALUES ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'large-role', 'a role with many capabilities');

-- 300 capabilities with 3 endpoints each
INSERT INTO capability(id, name, description, resource, action, type, application_id, folio_permission, visible)
SELECT ('00000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid, 'large_cap_' || i, 'capability ' || i,
       'Large ' || i, 'VIEW'::capability_action, 'DATA'::capability_type, 'test-application-0.0.1',
       'large.perm.' || i, true
FROM generate_series(1, 300) i;

INSERT INTO capability_endpoint(capability_id, path, method)
SELECT c.id, '/large/' || e || '/{id}', 'GET'
FROM capability c, generate_series(1, 3) e;

-- 150 capability sets with 2 capabilities each
INSERT INTO capability_set(id, name, description, resource, action, type, application_id, folio_permission, visible)
SELECT ('00000000-0000-4000-9000-' || lpad(s::text, 12, '0'))::uuid, 'large_set_' || s, 'capability set ' || s,
       'Large set ' || s, 'VIEW'::capability_action, 'DATA'::capability_type, 'test-application-0.0.1',
       'large.set.perm.' || s, true
FROM generate_series(1, 150) s;

INSERT INTO capability_set_capability(capability_set_id, capability_id)
SELECT ('00000000-0000-4000-9000-' || lpad(((i - 1) / 2 + 1)::text, 12, '0'))::uuid,
       ('00000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 300) i;

-- the role: first 150 capabilities directly, all capability sets
INSERT INTO role_capability(role_id, capability_id)
SELECT '1e985e76-e9ca-401c-ad8e-0d121a11111e', ('00000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 150) i;

INSERT INTO role_capability_set(role_id, capability_set_id)
SELECT '1e985e76-e9ca-401c-ad8e-0d121a11111e', id
FROM capability_set;
