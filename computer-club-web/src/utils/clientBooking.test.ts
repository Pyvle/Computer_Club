import { describe, expect, it } from 'vitest'
import {
  calculateCartBookingTotal,
  calculateCartProductTotal,
  calculateEstimatedBookingPrice,
  calculateSeatSelectionTotal,
  getEffectiveSeatPricePerHour,
  getDurationHours,
} from './clientBooking'
import type {
  CartBookingLineClientResponse,
  CartProductLineClientResponse,
  SeatClientResponse,
  SeatPriceClientResponse,
  TimePackageClientResponse,
} from '../types'

const seatPrices: SeatPriceClientResponse[] = [
  { seatType: 'REGULAR', pricePerHourRub: 120 },
  { seatType: 'VIP', pricePerHourRub: 200 },
]

const timePackages: TimePackageClientResponse[] = [
  { id: 1, name: '3 hours', hours: 3, pricePerHourRub: 100, totalPriceRub: 300, availableFrom: null, availableTo: null },
]

const seats = new Map<number, SeatClientResponse>([
  [1, { id: 1, label: 'A1', type: 'REGULAR' }],
  [2, { id: 2, label: 'A2', type: 'VIP' }],
])

describe('client booking helpers', () => {
  it('calculates booking duration in hours', () => {
    expect(getDurationHours('2026-05-15T18:00:00', '2026-05-15T20:30:00')).toBe(2.5)
  })

  it('estimates booking price by minimal hourly seat rate', () => {
    expect(calculateEstimatedBookingPrice(2.5, seatPrices)).toBe(300)
    expect(calculateEstimatedBookingPrice(null, seatPrices)).toBeNull()
    expect(calculateEstimatedBookingPrice(0, seatPrices)).toBeNull()
  })

  it('uses package rate as base rate and keeps vip surcharge', () => {
    expect(calculateEstimatedBookingPrice(3, seatPrices, timePackages, 3)).toBe(300)
    expect(getEffectiveSeatPricePerHour(1, seats, seatPrices, timePackages, 3)).toBe(100)
    expect(getEffectiveSeatPricePerHour(2, seats, seatPrices, timePackages, 3)).toBe(180)
    expect(calculateSeatSelectionTotal([1, 2], seats, seatPrices, 3, timePackages, 3)).toBe(840)
  })

  it('calculates selected seats total for mixed regular and vip seats', () => {
    expect(calculateSeatSelectionTotal([1, 2], seats, seatPrices, 2.5)).toBe(800)
  })

  it('calculates cart booking total from booking line dates and seats', () => {
    const booking: CartBookingLineClientResponse = {
      lineId: 10,
      startAt: '2026-05-15T18:00:00',
      endAt: '2026-05-15T20:30:00',
      packageHours: null,
      seatIds: [1, 2],
    }

    expect(calculateCartBookingTotal(booking, seats, seatPrices)).toBe(800)
  })

  it('prefers server cart booking total when it is present', () => {
    const booking: CartBookingLineClientResponse = {
      lineId: 10,
      startAt: '2026-05-15T18:00:00',
      endAt: '2026-05-15T21:00:00',
      packageHours: 3,
      seatIds: [1],
      lineTotalRub: 270,
    }

    expect(calculateCartBookingTotal(booking, seats, seatPrices, timePackages)).toBe(270)
  })

  it('calculates product total from cart lines', () => {
    const products: CartProductLineClientResponse[] = [
      { lineId: 1, productId: 5, title: 'Water', qty: 2, priceRub: 90, lineTotalRub: 180 },
      { lineId: 2, productId: 6, title: 'Snack', qty: 1, priceRub: 150, lineTotalRub: 150 },
    ]

    expect(calculateCartProductTotal(products)).toBe(330)
  })
})
