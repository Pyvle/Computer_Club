import { describe, expect, it } from 'vitest'
import { getOwnerNavKeys, getPlatformSelectedKey, getSelectedClubNavKey } from './adminNavigation'

describe('admin navigation helpers', () => {
  it('returns full owner navigation and restricted admin navigation', () => {
    expect(getOwnerNavKeys(true)).toContain('staff')
    expect(getOwnerNavKeys(true)).toContain('audit')
    expect(getOwnerNavKeys(true)).toContain('settings')
    expect(getOwnerNavKeys(false)).not.toContain('staff')
    expect(getOwnerNavKeys(false)).not.toContain('audit')
    expect(getOwnerNavKeys(false)).not.toContain('settings')
  })

  it('detects selected club admin section from pathname', () => {
    const navKeys = getOwnerNavKeys(true)
    expect(getSelectedClubNavKey('/admin/club/7/bookings', navKeys)).toBe('bookings')
    expect(getSelectedClubNavKey('/admin/club/7/floorplans/editor', navKeys)).toBe('floorplans')
    expect(getSelectedClubNavKey('/admin/my-clubs', navKeys)).toBe('')
  })

  it('detects selected platform section from pathname', () => {
    const navKeys = [
      '/admin/platform/applications',
      '/admin/platform/clubs',
      '/admin/platform/catalog',
      '/admin/platform/users',
    ]
    expect(getPlatformSelectedKey('/admin/platform/users/12', navKeys)).toBe('/admin/platform/users')
    expect(getPlatformSelectedKey('/admin/platform/catalog', navKeys)).toBe('/admin/platform/catalog')
    expect(getPlatformSelectedKey('/admin/my-clubs', navKeys)).toBe('')
  })
})
