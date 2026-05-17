import { describe, it, expect } from 'vitest'
import { hasAnyPermission, isAdminRole } from './permission'

describe('hasAnyPermission', () => {
  it('returns true when requiredPermissions is empty', () => {
    expect(hasAnyPermission(['dashboard'], [])).toBe(true)
    expect(hasAnyPermission([], [])).toBe(true)
  })

  it('returns true when userPermissions is empty', () => {
    expect(hasAnyPermission([], ['dashboard'])).toBe(true)
  })

  it('returns true when userPermissions includes "all"', () => {
    expect(hasAnyPermission(['all'], ['dashboard', 'settings'])).toBe(true)
  })

  it('returns true when user has at least one required permission', () => {
    expect(hasAnyPermission(['dashboard', 'bidding'], ['bidding'])).toBe(true)
    expect(hasAnyPermission(['dashboard', 'bidding'], ['bidding', 'settings'])).toBe(true)
  })

  it('returns false when user has none of the required permissions', () => {
    expect(hasAnyPermission(['dashboard'], ['bidding'])).toBe(false)
    expect(hasAnyPermission(['dashboard', 'project'], ['bidding', 'settings'])).toBe(false)
  })

  it('handles undefined/null inputs gracefully', () => {
    expect(hasAnyPermission(undefined, undefined)).toBe(true)
    expect(hasAnyPermission(undefined, ['dashboard'])).toBe(true)
    expect(hasAnyPermission(['all'], undefined)).toBe(true)
  })
})

describe('isAdminRole', () => {
  it('returns true for admin', () => {
    expect(isAdminRole('admin')).toBe(true)
  })

  it('returns false for non-admin roles', () => {
    expect(isAdminRole('manager')).toBe(false)
    expect(isAdminRole('staff')).toBe(false)
    expect(isAdminRole('bid_admin')).toBe(false)
    expect(isAdminRole('')).toBe(false)
    expect(isAdminRole(undefined)).toBe(false)
  })
})
