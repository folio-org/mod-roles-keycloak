-- add permissions for capabilities
INSERT INTO permission(id, name, display_name, description, visible, sub_permissions, replaces)
VALUES ('e1a5683a-fece-43fb-bbaa-52a438af1111', 'foo.item.delete',
        'Delete Foo Item', 'Permission to delete a foo item',
        NULL, NULL, ARRAY['replaced.foo.item.delete']),

       ('e1a5683a-1111-1111-1111-52a438af1111', 'ui-foo.item.put',
        'Ui Update Foo Item', 'Ui Permission to update foo item',
        NULL, NULL, ARRAY['replaced.ui-foo.item.put']);
