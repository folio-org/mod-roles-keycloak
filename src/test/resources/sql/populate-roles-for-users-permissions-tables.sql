INSERT INTO test_mod_roles_keycloak.role(id, name, description, created_date, created_by_user_id,
                                         updated_date, updated_by_user_id)
VALUES ('5aeaa18a-0add-4553-b5b5-4135e9bb6084',
        'let-it-be-role-113', 'description',
        TIMESTAMP WITH TIME ZONE '2023-01-01 12:01:01+04',
        '11111111-2222-1111-2222-111111111111',
        TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04',
        '11111111-1111-2222-1111-111111111111');

INSERT INTO test_mod_roles_keycloak.user_role(user_id, role_id, created_date, created_by_user_id,
                                              updated_date, updated_by_user_id)
VALUES ('8c6d12fa-33a7-48c9-8769-71168d441345', '5aeaa18a-0add-4553-b5b5-4135e9bb6084',
        TIMESTAMP WITH TIME ZONE '2023-01-01 12:01:00+04', '11111111-2222-1111-2222-111111111111',
        TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04', '11111111-1111-2222-1111-111111111111');
