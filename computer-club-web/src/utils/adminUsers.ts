import type {
  AdminUserResponse,
  GlobalAdminUserBookingItem,
  GlobalAdminUserPurchaseItem,
} from '../types'

export type ActiveFilter = 'all' | 'active' | 'inactive'

export function buildUserStats(users: AdminUserResponse[]) {
  return {
    total: users.length,
    active: users.filter((user) => user.isActive).length,
    admins: users.filter((user) => user.globalRole === 'GLOBAL_ADMIN').length,
    appUsers: users.filter((user) => user.globalRole !== 'GLOBAL_ADMIN').length,
  }
}

export function filterUsers(
  users: AdminUserResponse[],
  activeFilter: ActiveFilter,
  search: string,
): AdminUserResponse[] {
  let result = users

  if (activeFilter === 'active') result = result.filter((user) => user.isActive)
  if (activeFilter === 'inactive') result = result.filter((user) => !user.isActive)

  const query = search.trim().toLowerCase()
  if (query) {
    result = result.filter((user) =>
      (user.phone ?? '').toLowerCase().includes(query) || String(user.id).includes(query),
    )
  }

  return result
}

export function getAdminUsers(users: AdminUserResponse[], activeFilter: ActiveFilter, search: string) {
  return filterUsers(users.filter((user) => user.hasPassword), activeFilter, search)
}

export function getAppUsers(users: AdminUserResponse[], activeFilter: ActiveFilter, search: string) {
  return filterUsers(users.filter((user) => user.globalRole !== 'GLOBAL_ADMIN'), activeFilter, search)
}

export function aggregateUserClubStats(
  bookings: GlobalAdminUserBookingItem[] | null,
  purchases: GlobalAdminUserPurchaseItem[] | null,
) {
  if (!bookings) return null

  const map = new Map<number, {
    clubName: string
    bookingsCount: number
    lastVisit: string
    paidRub: number
  }>()

  bookings.forEach((booking) => {
    const entry = map.get(booking.clubId) ?? {
      clubName: booking.clubName,
      bookingsCount: 0,
      lastVisit: '',
      paidRub: 0,
    }
    entry.bookingsCount++
    if (!entry.lastVisit || booking.startAt > entry.lastVisit) entry.lastVisit = booking.startAt
    map.set(booking.clubId, entry)
  })

  if (purchases) {
    purchases
      .filter((purchase) => purchase.paymentStatus === 'PAID')
      .forEach((purchase) => {
        const entry = map.get(purchase.clubId)
        if (entry) entry.paidRub += purchase.totalRub
      })
  }

  return Array.from(map.entries())
    .map(([clubId, value]) => ({ clubId, ...value }))
    .sort((left, right) => right.lastVisit.localeCompare(left.lastVisit))
}
