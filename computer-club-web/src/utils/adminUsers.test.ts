import { describe, expect, it } from 'vitest'
import {
  aggregateUserClubStats,
  buildUserStats,
  filterUsers,
  getAdminUsers,
  getAppUsers,
} from './adminUsers'
import type {
  AdminUserResponse,
  GlobalAdminUserBookingItem,
  GlobalAdminUserPurchaseItem,
} from '../types'

const users: AdminUserResponse[] = [
  {
    id: 1,
    phone: '+79990000001',
    isActive: true,
    globalRole: 'GLOBAL_ADMIN',
    hasPassword: true,
    createdAt: '2026-05-10T10:00:00',
    updatedAt: '2026-05-10T10:00:00',
    bookingsCount: 0,
    purchasesCount: 0,
    totalSpentRub: 0,
    visitedClubsCount: 0,
    lastActivityAt: null,
    clubRoles: [],
  },
  {
    id: 2,
    phone: '+79990000002',
    isActive: false,
    globalRole: 'USER',
    hasPassword: false,
    createdAt: '2026-05-11T10:00:00',
    updatedAt: '2026-05-11T10:00:00',
    bookingsCount: 3,
    purchasesCount: 2,
    totalSpentRub: 1800,
    visitedClubsCount: 1,
    lastActivityAt: '2026-05-14T12:00:00',
    clubRoles: [],
  },
  {
    id: 3,
    phone: '+79990000003',
    isActive: true,
    globalRole: 'USER',
    hasPassword: true,
    createdAt: '2026-05-12T10:00:00',
    updatedAt: '2026-05-12T10:00:00',
    bookingsCount: 1,
    purchasesCount: 1,
    totalSpentRub: 900,
    visitedClubsCount: 1,
    lastActivityAt: '2026-05-15T10:00:00',
    clubRoles: [{ clubId: 7, clubName: 'Cyber Arena', role: 'ADMIN' }],
  },
]

describe('admin users helpers', () => {
  it('builds top-level admin statistics', () => {
    expect(buildUserStats(users)).toEqual({
      total: 3,
      active: 2,
      admins: 1,
      appUsers: 2,
    })
  })

  it('filters users by active status and search string', () => {
    expect(filterUsers(users, 'active', '').map((user) => user.id)).toEqual([1, 3])
    expect(filterUsers(users, 'inactive', '').map((user) => user.id)).toEqual([2])
    expect(filterUsers(users, 'all', '000003').map((user) => user.id)).toEqual([3])
    expect(filterUsers(users, 'all', '2').map((user) => user.id)).toEqual([2])
  })

  it('splits manager accounts and app users for admin tabs', () => {
    expect(getAdminUsers(users, 'all', '').map((user) => user.id)).toEqual([1, 3])
    expect(getAppUsers(users, 'all', '').map((user) => user.id)).toEqual([2, 3])
  })

  it('aggregates user activity by clubs and sums only paid purchases', () => {
    const bookings: GlobalAdminUserBookingItem[] = [
      {
        bookingId: 1,
        clubId: 10,
        clubName: 'Cyber Arena',
        startAt: '2026-05-15T18:00:00',
        endAt: '2026-05-15T20:00:00',
        status: 'DONE',
        totalRub: 500,
        seatLabels: ['A1'],
        purchaseId: 1,
      },
      {
        bookingId: 2,
        clubId: 10,
        clubName: 'Cyber Arena',
        startAt: '2026-05-16T18:00:00',
        endAt: '2026-05-16T20:00:00',
        status: 'DONE',
        totalRub: 700,
        seatLabels: ['A2'],
        purchaseId: 2,
      },
      {
        bookingId: 3,
        clubId: 11,
        clubName: 'Pixel Base',
        startAt: '2026-05-14T18:00:00',
        endAt: '2026-05-14T20:00:00',
        status: 'DONE',
        totalRub: 400,
        seatLabels: ['B1'],
        purchaseId: 3,
      },
    ]

    const purchases: GlobalAdminUserPurchaseItem[] = [
      {
        purchaseId: 1,
        clubId: 10,
        clubName: 'Cyber Arena',
        paymentStatus: 'PAID',
        totalRub: 1200,
        bookingTotalRub: 700,
        productsTotalRub: 500,
        createdAt: '2026-05-16T20:10:00',
      },
      {
        purchaseId: 2,
        clubId: 11,
        clubName: 'Pixel Base',
        paymentStatus: 'FAILED',
        totalRub: 900,
        bookingTotalRub: 400,
        productsTotalRub: 500,
        createdAt: '2026-05-14T20:10:00',
      },
    ]

    expect(aggregateUserClubStats(bookings, purchases)).toEqual([
      {
        clubId: 10,
        clubName: 'Cyber Arena',
        bookingsCount: 2,
        lastVisit: '2026-05-16T18:00:00',
        paidRub: 1200,
      },
      {
        clubId: 11,
        clubName: 'Pixel Base',
        bookingsCount: 1,
        lastVisit: '2026-05-14T18:00:00',
        paidRub: 0,
      },
    ])
  })
})
