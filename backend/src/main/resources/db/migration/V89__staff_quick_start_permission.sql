UPDATE roles
SET menu_permissions = CASE
    WHEN menu_permissions IS NULL OR TRIM(menu_permissions) = '' THEN 'dashboard,bidding,project,knowledge,resource,dashboard.quickStart'
    ELSE CONCAT(menu_permissions, ',dashboard.quickStart')
END
WHERE LOWER(code) = 'staff'
  AND (
    menu_permissions IS NULL
    OR CONCAT(',', menu_permissions, ',') NOT LIKE '%,dashboard.quickStart,%'
  );
