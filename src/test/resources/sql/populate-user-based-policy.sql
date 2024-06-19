INSERT INTO test_mod_roles_keycloak.policy(id, name, description, type, created_by_user_id)
VALUES ('1e111e11-1111-401c-ad8e-0d121a11111e', 'user-based-policy', 'hello work', 'USER',
'11111111-1111-4011-1111-0d121a11111e');

INSERT INTO test_mod_roles_keycloak.policy_users(policy_id, user_id)
VALUES ('1e111e11-1111-401c-ad8e-0d121a11111e', '61893f40-4739-49fc-bf07-daeff3021f90');
