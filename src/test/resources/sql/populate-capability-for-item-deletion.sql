INSERT INTO test_mod_roles_keycloak.capability_set
  (id, name, description, resource, action, type, application_id, created_by)
VALUES ('9a8b1202-2a7b-46e5-9ca4-84ba5b41642c', 'Delete foo.item capability',
        'Capability to delete item', '/foo/item', 'DELETE', 'DATA', 'foo-application-1.0.0',
        '11111111-1111-4011-1111-0d121a11111e');

INSERT INTO test_mod_roles_keycloak.capability_permissions(capability_id, permission)
VALUES ('9a8b1202-2a7b-46e5-9ca4-84ba5b41642c', 'foo.item.delete');
