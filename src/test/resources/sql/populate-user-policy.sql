SET SEARCH_PATH = 'test_mod_roles_keycloak';

INSERT INTO policy(id, name, description, type, is_system, logic, created_date, created_by_user_id)
VALUES ('ce49bb90-0a59-4f74-a25c-2dbb2728cdf6', 'Policy for user: 3e8647ee-2a23-4ca4-896b-95476559c567',
        'System generated policy for user: 3e8647ee-2a23-4ca4-896b-95476559c567', 'USER', FALSE,'POSITIVE',
        '2023-09-05 11:51:33.571354', '11111111-2222-1111-2222-111111111111');

INSERT INTO policy_users (policy_id, user_id)
VALUES ('ce49bb90-0a59-4f74-a25c-2dbb2728cdf6', '3e8647ee-2a23-4ca4-896b-95476559c567');
