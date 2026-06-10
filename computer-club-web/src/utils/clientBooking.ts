import dayjs from 'dayjs'
import type {
  CartBookingLineClientResponse,
  CartProductLineClientResponse,
  SeatClientResponse,
  SeatPriceClientResponse,
  TimePackageClientResponse,
} from '../types'

export function getDurationHours(startAt: string, endAt: string): number {
  return dayjs(endAt).diff(dayjs(startAt), 'minute') / 60
}

export function calculateEstimatedBookingPrice(
  durationHours: number | null,
  seatPrices: SeatPriceClientResponse[],
  timePackages: TimePackageClientResponse[] = [],
  packageHours: number | null = null,
): number | null {
  if (!durationHours || durationHours <= 0 || seatPrices.length === 0) return null
  const standardRate = getStandardSeatRate(seatPrices)
  if (standardRate === null) return null
  const baseRate = getPackageRatePerHour(packageHours, timePackages) ?? standardRate
  return Math.ceil(durationHours * baseRate)
}

export function getStandardSeatRate(seatPrices: SeatPriceClientResponse[]): number | null {
  if (seatPrices.length === 0) return null
  return Math.min(...seatPrices.map((price) => price.pricePerHourRub))
}

export function getPackageRatePerHour(
  packageHours: number | null,
  timePackages: TimePackageClientResponse[],
): number | null {
  if (packageHours === null) return null
  return timePackages.find((pkg) => pkg.hours === packageHours)?.pricePerHourRub ?? null
}

export function getSeatPricePerHour(
  seatId: number,
  seatById: Map<number, SeatClientResponse>,
  seatPrices: SeatPriceClientResponse[],
): number {
  const seat = seatById.get(seatId)
  return seatPrices.find((price) => price.seatType === seat?.type)?.pricePerHourRub ?? 0
}

export function getEffectiveSeatPricePerHour(
  seatId: number,
  seatById: Map<number, SeatClientResponse>,
  seatPrices: SeatPriceClientResponse[],
  timePackages: TimePackageClientResponse[] = [],
  packageHours: number | null = null,
): number {
  const standardRate = getStandardSeatRate(seatPrices) ?? 0
  const baseRate = getPackageRatePerHour(packageHours, timePackages) ?? standardRate
  const seatTypeRate = getSeatPricePerHour(seatId, seatById, seatPrices) || standardRate
  return baseRate + Math.max(0, seatTypeRate - standardRate)
}

export function calculateSeatSelectionTotal(
  selectedSeatIds: number[],
  seatById: Map<number, SeatClientResponse>,
  seatPrices: SeatPriceClientResponse[],
  durationHours: number,
  timePackages: TimePackageClientResponse[] = [],
  packageHours: number | null = null,
): number {
  return selectedSeatIds.reduce((sum, seatId) => {
    return sum + Math.ceil(
      getEffectiveSeatPricePerHour(seatId, seatById, seatPrices, timePackages, packageHours) * durationHours
    )
  }, 0)
}

export function calculateCartBookingTotal(
  booking: CartBookingLineClientResponse,
  seatById: Map<number, SeatClientResponse>,
  seatPrices: SeatPriceClientResponse[],
  timePackages: TimePackageClientResponse[] = [],
): number {
  if (booking.lineTotalRub !== null && booking.lineTotalRub !== undefined) return booking.lineTotalRub
  const durationHours = getDurationHours(booking.startAt, booking.endAt)
  return calculateSeatSelectionTotal(
    booking.seatIds,
    seatById,
    seatPrices,
    durationHours,
    timePackages,
    booking.packageHours,
  )
}

export function calculateCartProductTotal(products: CartProductLineClientResponse[]): number {
  return products.reduce((sum, product) => sum + product.lineTotalRub, 0)
}
