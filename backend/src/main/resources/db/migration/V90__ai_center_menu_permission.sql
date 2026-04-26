UPDATE roles
SET menu_permissions = CASE
    WHEN menu_permissions IS NULL OR TRIM(menu_permissions) = '' THEN 'ai-center'
    ELSE CONCAT(menu_permissions, ',ai-center')
END
WHERE LOWER(code) IN ('manager', 'staff')
  AND (
    menu_permissions IS NULL
    OR CONCAT(',', menu_permissions, ',') NOT LIKE '%,ai-center,%'
  );
