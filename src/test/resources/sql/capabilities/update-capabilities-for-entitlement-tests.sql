SET SEARCH_PATH = 'test_mod_roles_keycloak';

-- Reassign 2 capabilities to a second application to create mixed-entitlement scenario.
-- ui_foo_item.delete (ui-foo.item.delete) and ui_foo_item.create (module.foo.item.post)
-- are moved to test-application-2.0.0 (not entitled), while test-application-0.0.1 remains entitled.
UPDATE capability
SET application_id = 'test-application-2.0.0'
WHERE id IN ('f491047c-32eb-4736-815c-ebb8e94dffac', '5d764bb8-b5e5-4f33-8640-23eb9732b438');
