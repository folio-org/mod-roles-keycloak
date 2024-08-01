SET SEARCH_PATH = 'test_mod_roles_keycloak';

insert into policy(id, name, description, type, is_system, logic)
values ('dec68c30-4f93-4481-95bd-9ec587b15c1c', 'Policy for role: 1e985e76-e9ca-401c-ad8e-0d121a11111e',
        'System generated policy for role: 1e985e76-e9ca-401c-ad8e-0d121a11111e', 'ROLE', FALSE, 'POSITIVE');

insert into policy_roles(policy_id, role_id, required)
values ('dec68c30-4f93-4481-95bd-9ec587b15c1c', '1e985e76-e9ca-401c-ad8e-0d121a11111e', false);
