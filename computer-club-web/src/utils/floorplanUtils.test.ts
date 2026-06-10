import { describe, expect, it } from 'vitest'
import { parseItems } from './floorplanUtils'
import type { FloorplanResponse } from '../types'

function floorplan(data: unknown): FloorplanResponse {
  return {
    id: 1,
    clubId: 10,
    name: 'Основной зал',
    width: 1000,
    height: 600,
    gridSize: 50,
    status: 'PUBLISHED',
    version: 1,
    data,
    updatedAt: '2026-05-14T12:00:00',
  }
}

describe('parseItems', () => {
  it('returns empty list for malformed floorplan data', () => {
    expect(parseItems(floorplan(null))).toEqual([])
    expect(parseItems(floorplan({ items: 'bad-data' }))).toEqual([])
  })

  it('keeps valid floor, wall and seat items and drops invalid items', () => {
    const result = parseItems(
      floorplan({
        items: [
          { type: 'FLOOR', col: 1, row: 2, roomType: 'VIP' },
          { type: 'WALL', orientation: 'H', col: 2, row: 3, auto: true },
          { type: 'SEAT', seatId: 15, col: 4, row: 5 },
          { type: 'FLOOR', col: 1, row: 1, roomType: 'UNKNOWN' },
          { type: 'SEAT', seatId: 'bad', col: 1, row: 1 },
        ],
      }),
    )

    expect(result).toEqual([
      { type: 'FLOOR', col: 1, row: 2, roomType: 'VIP' },
      { type: 'WALL', orientation: 'H', col: 2, row: 3, auto: true },
      { type: 'SEAT', seatId: 15, col: 4, row: 5 },
    ])
  })

  it('converts legacy relative seat coordinates to grid cells', () => {
    const result = parseItems(
      floorplan({
        items: [
          { type: 'SEAT', seatId: 7, x: 0.2, y: 0.5, w: 0.05, h: 0.05 },
        ],
      }),
    )

    expect(result).toEqual([{ type: 'SEAT', seatId: 7, col: 4, row: 6 }])
  })
})
