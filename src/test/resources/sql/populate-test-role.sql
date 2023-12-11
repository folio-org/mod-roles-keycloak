SET SEARCH_PATH = 'test_mod_roles_keycloak';

insert into role(id, name, description, created_date, created_by, updated_date, updated_by)
values ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'test-role', 'test-role description', '2023-01-01 12:01:01',
        '11111111-2222-1111-2222-111111111111', '2023-01-02 12:01:01', '11111111-1111-2222-1111-111111111111');
