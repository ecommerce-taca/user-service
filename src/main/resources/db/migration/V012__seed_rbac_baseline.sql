INSERT INTO roles
(
    id,
    role_key,
    scope_type,
    description,
    is_system
)
VALUES

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000101',
        '-',
        ''
    )),
    'BUYER',
    'USER',
    'Authenticated marketplace buyer.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000102',
        '-',
         ''
    )),
    'SELLER',
    'SHOP',
    'Owner-level access to an owned shop.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000103',
         '-',
         ''
    )),
    'SELLER_STAFF',
    'SHOP',
    'Staff access scoped to one shop.',
    1
 ),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000201',
        '-',
        ''
    )),
    'SUPER_ADMIN',
    'SYSTEM',
    'Full administrative access.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000202',
        '-',
        ''
    )),
    'RISK_MANAGER',
    'SYSTEM',
    'KYC, risk and review administration.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000203',
        '-',
        ''
    )),
    'CATALOG_ADMIN',
    'SYSTEM',
    'Catalog administration role.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000204',
        '-',
        ''
    )),
    'FINANCE_OPS',
    'SYSTEM',
    'Finance and settlement operations role.',
    1
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000000205',
        '-',
        ''
    )),
    'SUPPORT_VIEWER',
    'SYSTEM',
    'Read-only support access.',
    1
);


INSERT INTO permissions
(
    id,
    permission_key,
    scope_type,
    description
)
VALUES

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001001',
        '-',
        ''
    )),
    'KYC_READ',
    'SYSTEM',
    'Read KYC queue, cases and authorized document metadata.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001002',
        '-',
        ''
    )),
    'KYC_DECIDE',
    'SYSTEM',
    'Approve, reject or request additional KYC information.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001003',
        '-',
        ''
    )),
    'KYC_REQUEST_INFO',
    'SYSTEM',
    'Request additional KYC information from a seller.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001101',
        '-',
        ''
    )),
    'USER_READ',
    'SYSTEM',
    'Read user information allowed by administrative policy.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001102',
        '-',
        ''
    )),
    'USER_SUSPEND',
    'SYSTEM',
    'Suspend or restore user accounts according to policy.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001103',
        '-',
        ''
    )),
    'ROLE_READ',
    'SYSTEM',
    'Read role and permission assignments.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001104',
        '-',
        ''
    )),
    'ROLE_ASSIGN',
    'SYSTEM',
    'Grant or revoke allowed role assignments.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001201',
        '-',
        ''
    )),
    'SHOP_READ',
    'SHOP',
    'Read shop information within the authorized shop scope.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001202',
        '-',
        ''
    )),
    'SHOP_UPDATE',
    'SHOP',
    'Update shop information within the authorized shop scope.'
),

(
    UNHEX(REPLACE(
        '019b0000-0000-7000-8000-000000001203',
        '-',
        ''
    )),
    'SELLER_STAFF_MANAGE',
    'SHOP',
    'Manage seller staff within the authorized shop scope.'
);


INSERT INTO role_permissions
(
    role_id,
    permission_id,
    granted_by
)
SELECT
    r.id,
    p.id,
    NULL
FROM
(
    /* Seller owner */
    SELECT 'SELLER' AS role_key, 'SHOP_READ' AS permission_key

    UNION ALL
    SELECT 'SELLER',
            'SHOP_UPDATE'

    UNION ALL
    SELECT 'SELLER', 'SELLER_STAFF_MANAGE'


    /* Seller staff - least privilege */
    UNION ALL
    SELECT 'SELLER_STAFF', 'SHOP_READ'


    /* Risk manager */
    UNION ALL
    SELECT 'RISK_MANAGER', 'KYC_READ'

    UNION ALL
    SELECT 'RISK_MANAGER', 'KYC_DECIDE'

    UNION ALL
    SELECT 'RISK_MANAGER', 'KYC_REQUEST_INFO'


    /* Support viewer - read only */
    UNION ALL
    SELECT 'SUPPORT_VIEWER', 'USER_READ'

    UNION ALL
    SELECT 'SUPPORT_VIEWER', 'ROLE_READ'


    /* Super admin - all Auth User baseline permissions */
    UNION ALL
    SELECT 'SUPER_ADMIN', 'KYC_READ'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'KYC_DECIDE'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'KYC_REQUEST_INFO'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'USER_READ'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'USER_SUSPEND'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'ROLE_READ'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'ROLE_ASSIGN'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'SHOP_READ'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'SHOP_UPDATE'

    UNION ALL
    SELECT 'SUPER_ADMIN', 'SELLER_STAFF_MANAGE'

) matrix

JOIN roles r
    ON r.role_key = matrix.role_key

JOIN permissions p
    ON p.permission_key = matrix.permission_key;