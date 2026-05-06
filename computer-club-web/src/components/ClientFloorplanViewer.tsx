import { useEffect, useMemo, useRef, useState } from 'react'
import { Button } from 'antd'
import { parseItems, type FloorItem, type WallItem } from '../utils/floorplanUtils'
import type { FloorplanResponse } from '../types'
import type { SeatClientResponse } from '../types'
import { tokens } from '../theme/tokens'

interface Props {
  floorplan: FloorplanResponse
  seats: SeatClientResponse[]
  busySeatIds: number[]
  selectedSeatIds: number[]
  onToggleSeat: (seatId: number) => void
}

const GAP = 2
const PADDING = 8
const WALL_THICKNESS = 4
const MIN_CELL_PX = 36
const MIN_ZOOM = 0.35
const MAX_ZOOM = 8
const ZOOM_STEP = 0.25

const VOID_BG = '#e2e4e8'
const FLOOR_BG: Record<string, string> = { REGULAR: '#ffffff', VIP: '#fff8e6' }

function cellKey(col: number, row: number) { return `${col},${row}` }
function wallKey(o: 'H' | 'V', col: number, row: number) { return `${o}:${col},${row}` }
function clampZoom(value: number) { return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Number.isFinite(value) ? value : 1)) }
function formatZoomLabel(value: number) { return `${Math.round(value * 100)}%` }

export default function ClientFloorplanViewer({ floorplan, seats, busySeatIds, selectedSeatIds, onToggleSeat }: Props) {
  const { width, height, gridSize } = floorplan
  const cols = Math.max(1, Math.floor(width / gridSize))
  const rows = Math.max(1, Math.floor(height / gridSize))

  const viewportRef = useRef<HTMLDivElement | null>(null)
  const [fitZoom, setFitZoom] = useState(1)
  const [autoFit, setAutoFit] = useState(true)
  const [zoom, setZoom] = useState(1)
  const cellPx = gridSize * zoom

  const items = useMemo(() => parseItems(floorplan), [floorplan])

  const cellMap = useMemo(() => {
    const map = new Map<string, number>()
    for (const item of items)
      if (item.type === 'SEAT') map.set(cellKey(item.col, item.row), item.seatId)
    return map
  }, [items])

  const floorMap = useMemo(() => {
    const map = new Map<string, FloorItem>()
    for (const item of items)
      if (item.type === 'FLOOR') map.set(cellKey(item.col, item.row), item)
    return map
  }, [items])

  const seatById = useMemo(() => {
    const map = new Map<number, SeatClientResponse>()
    for (const s of seats) map.set(s.id, s)
    return map
  }, [seats])

  const busySet = useMemo(() => new Set(busySeatIds), [busySeatIds])
  const selectedSet = useMemo(() => new Set(selectedSeatIds), [selectedSeatIds])

  const hasFloor = floorMap.size > 0
  const cellLeft = (c: number) => PADDING + c * (cellPx + GAP)
  const cellTop = (r: number) => PADDING + r * (cellPx + GAP)
  const fontSize = Math.max(8, Math.min(12, cellPx * 0.3))
  const showLabel = cellPx >= 28

  useEffect(() => {
    setAutoFit(true)
  }, [floorplan.id, floorplan.version, cols, rows, gridSize])

  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return

    const updateFitZoom = () => {
      const availableWidth = Math.max(160, viewport.clientWidth - PADDING * 2)
      const availableHeight = Math.max(160, viewport.clientHeight - PADDING * 2)
      const fitByWidth = (availableWidth - Math.max(0, (cols - 1) * GAP)) / Math.max(1, cols * gridSize)
      const fitByHeight = (availableHeight - Math.max(0, (rows - 1) * GAP)) / Math.max(1, rows * gridSize)
      const preferredZoom = Math.max(1, MIN_CELL_PX / gridSize)
      const nextFitZoom = clampZoom(Math.min(preferredZoom, fitByWidth, fitByHeight))

      setFitZoom(nextFitZoom)
      if (autoFit) {
        setZoom(nextFitZoom)
      }
    }

    updateFitZoom()
    const observer = new ResizeObserver(updateFitZoom)
    observer.observe(viewport)
    return () => observer.disconnect()
  }, [autoFit, cols, rows, gridSize])

  return (
    <div>
      <div style={{ marginBottom: 8, display: 'flex', gap: 8, alignItems: 'center' }}>
        <Button size="small" onClick={() => {
          setAutoFit(false)
          setZoom((z) => clampZoom(z - ZOOM_STEP))
        }}>−</Button>
        <span style={{ fontSize: 12, color: tokens.colors.textSecondary, minWidth: 44, textAlign: 'center' }}>{formatZoomLabel(zoom)}</span>
        <Button size="small" onClick={() => {
          setAutoFit(false)
          setZoom((z) => clampZoom(z + ZOOM_STEP))
        }}>+</Button>
        <Button size="small" onClick={() => {
          setAutoFit(true)
          setZoom(fitZoom)
        }}>целиком</Button>
      </div>

      {/* Легенда */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 10, flexWrap: 'wrap', fontSize: 12, color: tokens.colors.textSecondary }}>
        {[
          { color: tokens.colors.successSoft, border: tokens.colors.success, label: 'Свободно' },
          { color: '#fff7d6',                 border: '#d48806',              label: 'VIP свободно' },
          { color: tokens.colors.infoSoft,    border: tokens.colors.info,    label: 'Выбрано' },
          { color: tokens.colors.errorSoft,   border: tokens.colors.error,   label: 'Занято' },
        ].map(({ color, border, label }) => (
          <span key={label} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ width: 14, height: 14, background: color, border: `1px solid ${border}`, borderRadius: 3, display: 'inline-block' }} />
            {label}
          </span>
        ))}
      </div>

      <div ref={viewportRef} style={{ overflowX: 'auto', overflowY: 'auto', maxHeight: 'calc(100vh - 320px)', border: `1px solid ${tokens.colors.border}`, borderRadius: tokens.radius.sm, background: VOID_BG }}>
        <div style={{ position: 'relative', display: 'inline-block', padding: PADDING, minWidth: '100%', boxSizing: 'border-box' }}>
          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, ${cellPx}px)`, gap: GAP }}>
            {Array.from({ length: rows * cols }, (_, i) => {
              const row = Math.floor(i / cols)
              const col = i % cols
              const key = cellKey(col, row)
              const seatId = cellMap.get(key) ?? null
              const seat = seatId !== null ? seatById.get(seatId) : undefined
              const isVoid = hasFloor && !floorMap.get(key)
              const floorCell = floorMap.get(key)

              const isBusy = seatId !== null && busySet.has(seatId)
              const isSelected = seatId !== null && selectedSet.has(seatId)
              const hasSeat = seatId !== null && !isVoid

              let bgColor = VOID_BG
              let borderColor = '#d4d6da'

              if (!isVoid) {
                if (!hasSeat) {
                  bgColor = floorCell ? FLOOR_BG[floorCell.roomType] : FLOOR_BG.REGULAR
                  borderColor = tokens.colors.border
                } else if (isBusy) {
                  bgColor = tokens.colors.errorSoft
                  borderColor = tokens.colors.error
                } else if (isSelected) {
                  bgColor = tokens.colors.infoSoft
                  borderColor = tokens.colors.info
                } else if (seat?.type === 'VIP') {
                  bgColor = '#fff7d6'
                  borderColor = '#d48806'
                } else {
                  bgColor = tokens.colors.successSoft
                  borderColor = tokens.colors.success
                }
              }

              return (
                <div
                  key={key}
                  onClick={() => hasSeat && !isBusy && onToggleSeat(seatId!)}
                  style={{
                    width: cellPx, height: cellPx,
                    boxSizing: 'border-box',
                    borderRadius: isVoid ? 3 : 5,
                    border: `1px solid ${borderColor}`,
                    background: bgColor,
                    cursor: hasSeat && !isBusy ? 'pointer' : 'default',
                    display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                    gap: 1, userSelect: 'none', overflow: 'hidden',
                    opacity: isVoid ? 0.5 : 1,
                    boxShadow: hasSeat && !isBusy ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                    transition: 'background 0.15s',
                  }}
                >
                  {hasSeat && showLabel && (
                    <>
                      <span style={{ fontSize, fontWeight: 700, color: '#333', lineHeight: 1, textAlign: 'center', maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', padding: '0 2px' }}>
                        {seat ? seat.label : `#${seatId}`}
                      </span>
                      {seat?.type === 'VIP' && (
                        <span style={{ fontSize: Math.max(8, fontSize - 2), color: tokens.colors.warning, lineHeight: 1, fontWeight: 600 }}>VIP</span>
                      )}
                    </>
                  )}
                </div>
              )
            })}
          </div>

          {/* Стены */}
          <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
            {items.filter((i): i is WallItem => i.type === 'WALL').map((w) => {
              const key = wallKey(w.orientation, w.col, w.row)
              return w.orientation === 'H' ? (
                <div key={key} style={{ position: 'absolute', left: cellLeft(w.col), top: cellTop(w.row) - GAP / 2 - WALL_THICKNESS / 2, width: cellPx, height: WALL_THICKNESS, background: '#2a2a2a', borderRadius: 2 }} />
              ) : (
                <div key={key} style={{ position: 'absolute', left: cellLeft(w.col) - GAP / 2 - WALL_THICKNESS / 2, top: cellTop(w.row), width: WALL_THICKNESS, height: cellPx, background: '#2a2a2a', borderRadius: 2 }} />
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
