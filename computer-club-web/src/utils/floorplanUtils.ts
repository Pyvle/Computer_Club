import type { FloorplanResponse } from '../types'

export type RoomType = 'REGULAR' | 'VIP'

export type SeatItem = { type: 'SEAT'; seatId: number; col: number; row: number }
export type WallItem = { type: 'WALL'; orientation: 'H' | 'V'; col: number; row: number; auto?: boolean }
export type FloorItem = { type: 'FLOOR'; col: number; row: number; roomType: RoomType }
export type FloorplanItem = SeatItem | WallItem | FloorItem

export function parseItems(floorplan: FloorplanResponse): FloorplanItem[] {
  const { data, width, height, gridSize } = floorplan
  if (!data || typeof data !== 'object') return []
  const d = data as { items?: unknown[] }
  if (!Array.isArray(d.items)) return []
  const result: FloorplanItem[] = []

  for (const it of d.items) {
    if (!it || typeof it !== 'object') continue
    const item = it as Record<string, unknown>

    if (item.type === 'FLOOR') {
      if (
        typeof item.col === 'number' &&
        typeof item.row === 'number' &&
        (item.roomType === 'REGULAR' || item.roomType === 'VIP')
      ) {
        result.push({ type: 'FLOOR', col: item.col, row: item.row, roomType: item.roomType })
      }
      continue
    }

    if (item.type === 'WALL') {
      if (
        (item.orientation === 'H' || item.orientation === 'V') &&
        typeof item.col === 'number' &&
        typeof item.row === 'number'
      ) {
        result.push({
          type: 'WALL',
          orientation: item.orientation as 'H' | 'V',
          col: item.col,
          row: item.row,
          auto: item.auto === true,
        })
      }
      continue
    }

    if (item.type !== 'SEAT') continue
    const seatId = typeof item.seatId === 'number' ? item.seatId : null
    if (seatId === null) continue
    if (typeof item.col === 'number' && typeof item.row === 'number') {
      result.push({ type: 'SEAT', seatId, col: item.col, row: item.row })
      continue
    }
    // legacy x/y/w/h
    const x = typeof item.x === 'number' ? item.x : 0
    const y = typeof item.y === 'number' ? item.y : 0
    const w = typeof item.w === 'number' ? item.w : 0
    const col = w >= 1 ? Math.round(x / gridSize) : Math.round((x * width) / gridSize)
    const row = w >= 1 ? Math.round(y / gridSize) : Math.round((y * height) / gridSize)
    result.push({ type: 'SEAT', seatId, col, row })
  }
  return result
}
