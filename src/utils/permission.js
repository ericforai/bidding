/**
 * Permission utilities for frontend role/permission checks.
 * Replaces hardcoded role comparisons with menuPermission-driven logic.
 */

/**
 * Check if a user has any of the required permissions.
 * @param {string[]} userPermissions - user's menuPermissions (may contain 'all')
 * @param {string[]} requiredPermissions - required permission keys
 * @returns {boolean}
 */
export function hasAnyPermission(userPermissions, requiredPermissions) {
  if (!requiredPermissions || requiredPermissions.length === 0) return true
  if (!userPermissions || userPermissions.length === 0) return true
  if (userPermissions.includes('all')) return true
  return requiredPermissions.some((key) => userPermissions.includes(key))
}

/**
 * Check if the given role code represents an admin.
 * @param {string} roleCode
 * @returns {boolean}
 */
export function isAdminRole(roleCode) {
  return roleCode === 'admin'
}
