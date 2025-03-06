INSERT INTO member_mod_roles_keycloak.role(id, name, description, created_date, created_by_user_id,
                                         updated_date, updated_by_user_id)
VALUES
  ('00000000-0000-0000-0001-000000000002',
  'role-1', 'description',
  TIMESTAMP WITH TIME ZONE '2023-01-01 12:01:01+04',
  '11111111-2222-1111-2222-111111111111',
  TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04',
  '11111111-1111-2222-1111-111111111111'),
  ('00000000-0000-0000-0002-000000000002',
  'role-2',
  'description',
  TIMESTAMP WITH TIME ZONE '2023-01-01 12:01:01+04',
  '11111111-2222-1111-2222-111111111111',
  TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04',
  '11111111-1111-2222-1111-111111111111');

INSERT INTO member_mod_roles_keycloak.user_role(user_id, role_id, created_date, created_by_user_id,
                                              updated_date, updated_by_user_id)
VALUES
  ('61893f40-4739-49fc-bf07-daeff3021f90',
  '00000000-0000-0000-0001-000000000002',
  TIMESTAMP WITH TIME ZONE '2023-01-01 12:01:00+04',
  '11111111-2222-1111-2222-111111111111',
  TIMESTAMP WITH TIME ZONE '2023-01-02 12:01:01+04',
  '11111111-1111-2222-1111-111111111111');
