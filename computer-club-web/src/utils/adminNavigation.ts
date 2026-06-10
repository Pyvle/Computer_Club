export function getOwnerNavKeys(isOwner: boolean): string[] {
  const baseKeys = [
    'dashboard',
    'bookings',
    'purchases',
    'messages',
    'catalog',
    'time-packages',
    'seats',
    'floorplans',
    'users',
  ]

  return isOwner ? [...baseKeys, 'staff', 'audit', 'settings'] : baseKeys
}

export function getSelectedClubNavKey(pathname: string, navKeys: string[]): string {
  const pathParts = pathname.split('/')
  return navKeys.find((key) => pathParts.includes(key)) ?? ''
}

export function getPlatformSelectedKey(pathname: string, navKeys: string[]): string {
  return navKeys.find((key) => pathname.startsWith(key)) ?? ''
}
