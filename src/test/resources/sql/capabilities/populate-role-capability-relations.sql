SET SEARCH_PATH = 'test_mod_roles_keycloak';

insert into role_capability(role_id, capability_id, created_by, created_date)
values ('1e985e76-e9ca-401c-ad8e-0d121a11111e', 'e2628d7d-059a-46a1-a5ea-10a5a37b1af2',
        '11111111-2222-1111-2222-111111111111', '2023-08-30 15:25:00'),
       ('1e985e76-e9ca-401c-ad8e-0d121a11111e', '8d2da27c-1d56-48b6-958d-2bfae6d79dc8',
        '11111111-2222-1111-2222-111111111111', '2023-08-30 15:25:00');
