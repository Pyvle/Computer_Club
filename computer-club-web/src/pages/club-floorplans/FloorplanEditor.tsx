import { useState, useMemo, useEffect, useRef } from 'react'
import { Modal, Select, Button, Tag, Empty, Tooltip, Segmented } from 'antd'
import { DeleteOutlined, ZoomInOutlined, ZoomOutOutlined, CompressOutlined } from '@ant-design/icons'
import type { AdminSeatResponse, FloorplanResponse } from '../../types'

// --- Types ---

type RoomType = 'REGULAR' | 'VIP'
type SeatItem = { type: 'SEAT'; seatId: number; col: number; row: number }
type WallItem = { type: 'WALL'; orientation: 'H' | 'V'; col: number; row: number; auto?: boolean }
type FloorItem = { type: 'FLOOR'; col: number; row: number; roomType: RoomType }
type FloorplanItem = SeatItem | WallItem | FloorItem

export interface FloorplanData {
  items: FloorplanItem[]
}

// --- Constants ---

const GAP = 2
const PADDING = 8
const WALL_THICKNESS = 4
const HIT_SIZE = 14
// размер сетки по умолчанию при создании новой схемы
export const DEFAULT_COLS = 30
export const DEFAULT_ROWS = 20

const FLOOR_BG: Record<RoomType, string> = {
  REGULAR: '#ffffff',
  VIP: '#fff8e6',
}
const VOID_BG = '#e2e4e8'

// --- Helpers ---

function wallKey(orientation: 'H' | 'V', col: number, row: number) {
  return `${orientation}:${col},${row}`
}

function cellKey(col: number, row: number) {
  return `${col},${row}`
}

function parseItems(raw: unknown, width: number, height: number, gridSize: number): FloorplanItem[] {
  if (!raw || typeof raw !== 'object') return []
  const d = raw as { items?: unknown[] }
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
          orientation: item.orientation,
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

// --- Component ---

const MIN_CELL_PX = 36
const MIN_ZOOM = 0.35
const MAX_ZOOM = 8
const ZOOM_STEP = 0.25
type Tool = 'room' | 'seat' | 'wall' | 'erase'

interface SelectionRect {
  minCol: number
  maxCol: number
  minRow: number
  maxRow: number
}

interface Props {
  floorplan: FloorplanResponse
  seats: AdminSeatResponse[]
  readOnly: boolean
  onChange: (data: FloorplanData) => void
}

function clampZoom(value: number) {
  return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Number.isFinite(value) ? value : 1))
}

function formatZoomLabel(value: number) {
  return `${Math.round(value * 100)}%`
}

export default function FloorplanEditor({ floorplan, seats, readOnly, onChange }: Props) {
  const { width, height, gridSize } = floorplan
  const cols = Math.max(1, Math.floor(width / gridSize))
  const rows = Math.max(1, Math.floor(height / gridSize))

  const viewportRef = useRef<HTMLDivElement | null>(null)
  const [fitZoom, setFitZoom] = useState(1)
  const [autoFit, setAutoFit] = useState(true)
  const [zoom, setZoom] = useState(1)
  const cellPx = gridSize * zoom

  const [items, setItems] = useState<FloorplanItem[]>(() =>
    parseItems(floorplan.data, width, height, gridSize),
  )
  const filteredRef = useRef(false)

  // начинаем с «Комнаты» если схема пустая, иначе — «Место»
  const [tool, setTool] = useState<Tool>(() => {
    const parsed = parseItems(floorplan.data, width, height, gridSize)
    return parsed.some((i) => i.type === 'FLOOR') ? 'seat' : 'room'
  })
  const [roomType, setRoomType] = useState<RoomType>('REGULAR')
  const [hoveredEdge, setHoveredEdge] = useState<string | null>(null)
  const [dragStart, setDragStart] = useState<{ col: number; row: number } | null>(null)
  const [dragCurrent, setDragCurrent] = useState<{ col: number; row: number } | null>(null)
  const [activeCell, setActiveCell] = useState<{ col: number; row: number } | null>(null)
  const [pickSeatId, setPickSeatId] = useState<number | null>(null)

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

  // пиксельная позиция левого/верхнего края ячейки (внутри wrapper с padding)
  const cellLeft = (c: number) => PADDING + c * (cellPx + GAP)
  const cellTop = (r: number) => PADDING + r * (cellPx + GAP)

  // --- Derived state ---

  const cellMap = useMemo(() => {
    const map = new Map<string, number>()
    for (const item of items)
      if (item.type === 'SEAT') map.set(cellKey(item.col, item.row), item.seatId)
    return map
  }, [items])

  const wallSet = useMemo(() => {
    const s = new Set<string>()
    for (const item of items)
      if (item.type === 'WALL') s.add(wallKey(item.orientation, item.col, item.row))
    return s
  }, [items])

  const floorMap = useMemo(() => {
    const map = new Map<string, FloorItem>()
    for (const item of items)
      if (item.type === 'FLOOR') map.set(cellKey(item.col, item.row), item)
    return map
  }, [items])

  // если нет ни одного FLOOR — обратная совместимость: все ячейки считаются floor
  const hasFloor = floorMap.size > 0

  const seatById = useMemo(() => {
    const map = new Map<number, AdminSeatResponse>()
    for (const s of seats) map.set(s.id, s)
    return map
  }, [seats])

  const placedSeatIds = useMemo(
    () => new Set(items.filter((i): i is SeatItem => i.type === 'SEAT').map((i) => i.seatId)),
    [items],
  )

  const selectionRect = useMemo((): SelectionRect | null => {
    if (!dragStart || !dragCurrent) return null
    return {
      minCol: Math.min(dragStart.col, dragCurrent.col),
      maxCol: Math.max(dragStart.col, dragCurrent.col),
      minRow: Math.min(dragStart.row, dragCurrent.row),
      maxRow: Math.max(dragStart.row, dragCurrent.row),
    }
  }, [dragStart, dragCurrent])

  useEffect(() => {
    if (seats.length === 0 || filteredRef.current) return
    filteredRef.current = true
    const validIds = new Set(seats.map((s) => s.id))
    const cleaned = items.filter((item) => item.type !== 'SEAT' || validIds.has(item.seatId))
    if (cleaned.length !== items.length) {
      setItems(cleaned)
      onChange({ items: cleaned })
    }
  }, [seats])

  // --- Core functions ---

  function commit(next: FloorplanItem[]) {
    setItems(next)
    onChange({ items: next })
  }

  /**
   * Вычисляет авто-стены: ребро добавляется там, где floor-ячейка граничит
   * с void-ячейкой или с ячейкой другого типа комнаты.
   */
  function computeAutoWalls(floors: FloorItem[]): WallItem[] {
    const typeMap = new Map<string, RoomType>()
    for (const f of floors) typeMap.set(cellKey(f.col, f.row), f.roomType)

    const keys = new Set<string>()
    for (const f of floors) {
      const { col, row, roomType: t } = f
      if (row > 0 && typeMap.get(cellKey(col, row - 1)) !== t) keys.add(wallKey('H', col, row))
      if (row < rows - 1 && typeMap.get(cellKey(col, row + 1)) !== t)
        keys.add(wallKey('H', col, row + 1))
      if (col > 0 && typeMap.get(cellKey(col - 1, row)) !== t) keys.add(wallKey('V', col, row))
      if (col < cols - 1 && typeMap.get(cellKey(col + 1, row)) !== t)
        keys.add(wallKey('V', col + 1, row))
    }

    return [...keys].map((key) => {
      const ci = key.indexOf(':')
      const orient = key.slice(0, ci) as 'H' | 'V'
      const [c, r] = key.slice(ci + 1).split(',').map(Number)
      return { type: 'WALL' as const, orientation: orient, col: c, row: r, auto: true }
    })
  }

  /** Пересобирает items из переданных floor-ячеек + ручных стен + мест. */
  function rebuildWithFloors(floors: FloorItem[], keepSeats?: SeatItem[]) {
    const manualWalls = items.filter((i): i is WallItem => i.type === 'WALL' && !i.auto)
    const seatItems = keepSeats ?? items.filter((i): i is SeatItem => i.type === 'SEAT')
    const autoWalls = computeAutoWalls(floors)
    const manualKeys = new Set(manualWalls.map((w) => wallKey(w.orientation, w.col, w.row)))
    const filteredAuto = autoWalls.filter((w) => !manualKeys.has(wallKey(w.orientation, w.col, w.row)))
    commit([...floors, ...manualWalls, ...filteredAuto, ...seatItems])
  }

  function applyRoomToRect(rect: SelectionRect) {
    const rectKeys = new Set<string>()
    const newFloors: FloorItem[] = []
    for (let c = rect.minCol; c <= rect.maxCol; c++) {
      for (let r = rect.minRow; r <= rect.maxRow; r++) {
        rectKeys.add(cellKey(c, r))
        newFloors.push({ type: 'FLOOR', col: c, row: r, roomType })
      }
    }
    const existing = items.filter(
      (i): i is FloorItem => i.type === 'FLOOR' && !rectKeys.has(cellKey(i.col, i.row)),
    )
    rebuildWithFloors([...existing, ...newFloors])
  }

  function applyEraseToRect(rect: SelectionRect) {
    const eraseKeys = new Set<string>()
    for (let c = rect.minCol; c <= rect.maxCol; c++)
      for (let r = rect.minRow; r <= rect.maxRow; r++) eraseKeys.add(cellKey(c, r))
    const floors = items.filter(
      (i): i is FloorItem => i.type === 'FLOOR' && !eraseKeys.has(cellKey(i.col, i.row)),
    )
    const seats = items.filter(
      (i): i is SeatItem => i.type === 'SEAT' && !eraseKeys.has(cellKey(i.col, i.row)),
    )
    rebuildWithFloors(floors, seats)
  }

  // --- Pointer drag (room / erase) ---

  function getCellFromPointer(e: React.PointerEvent<HTMLDivElement>) {
    const x = e.nativeEvent.offsetX
    const y = e.nativeEvent.offsetY
    return {
      col: Math.max(0, Math.min(cols - 1, Math.floor((x - PADDING) / (cellPx + GAP)))),
      row: Math.max(0, Math.min(rows - 1, Math.floor((y - PADDING) / (cellPx + GAP)))),
    }
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    if (readOnly || (tool !== 'room' && tool !== 'erase')) return
    const cell = getCellFromPointer(e)
    setDragStart(cell)
    setDragCurrent(cell)
    e.currentTarget.setPointerCapture(e.pointerId)
    e.preventDefault()
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (!dragStart) return
    setDragCurrent(getCellFromPointer(e))
  }

  function handlePointerUp() {
    if (dragStart && selectionRect) {
      if (tool === 'room') applyRoomToRect(selectionRect)
      else if (tool === 'erase') applyEraseToRect(selectionRect)
    }
    setDragStart(null)
    setDragCurrent(null)
  }

  // --- Seat tool ---

  function handleCellClick(col: number, row: number) {
    if (readOnly || tool !== 'seat') return
    if (hasFloor && !floorMap.has(cellKey(col, row))) return
    setActiveCell({ col, row })
    setPickSeatId(null)
  }

  function toggleWall(orientation: 'H' | 'V', col: number, row: number) {
    const key = wallKey(orientation, col, row)
    if (wallSet.has(key)) {
      commit(
        items.filter(
          (i) =>
            !(i.type === 'WALL' && i.orientation === orientation && i.col === col && i.row === row),
        ),
      )
    } else {
      commit([...items, { type: 'WALL', orientation, col, row }])
    }
  }

  function closeModal() {
    setActiveCell(null)
    setPickSeatId(null)
  }

  function placeSeat() {
    if (!activeCell || pickSeatId === null) return
    const { col, row } = activeCell
    const filtered = items.filter(
      (i) => !(i.type === 'SEAT' && i.col === col && i.row === row),
    )
    filtered.push({ type: 'SEAT', seatId: pickSeatId, col, row })
    commit(filtered)
    closeModal()
  }

  function removeSeat(col: number, row: number) {
    commit(items.filter((i) => !(i.type === 'SEAT' && i.col === col && i.row === row)))
    closeModal()
  }

  // --- Modal state ---

  const activeSeatId =
    activeCell !== null ? (cellMap.get(cellKey(activeCell.col, activeCell.row)) ?? null) : null
  const activeSeat = activeSeatId !== null ? (seatById.get(activeSeatId) ?? null) : null
  const availableSeats = seats.filter(
    (s) => s.isActive && (!placedSeatIds.has(s.id) || s.id === activeSeatId),
  )
  const isPlaceModal = activeCell !== null && activeSeat === null
  const isInfoModal = activeCell !== null && activeSeat !== null

  // --- Render helpers ---

  const showLabel = cellPx >= 28
  const showVipBadge = cellPx >= 44
  const fontSize = Math.min(13, Math.max(9, cellPx * 0.28))
  const wallCount = items.filter((i) => i.type === 'WALL' && !i.auto).length
  const autoWallCount = items.filter((i) => i.type === 'WALL' && i.auto).length
  const seatCount = items.filter((i) => i.type === 'SEAT').length
  const floorCount = floorMap.size

  const wrapperCursor = readOnly
    ? 'default'
    : tool === 'room'
      ? 'crosshair'
      : tool === 'erase'
        ? 'cell'
        : 'default'

  const selOverlayColor =
    tool === 'erase'
      ? { bg: 'rgba(255,77,79,0.22)', border: '#ff4d4f' }
      : roomType === 'VIP'
        ? { bg: 'rgba(250,173,20,0.28)', border: '#faad14' }
        : { bg: 'rgba(22,119,255,0.18)', border: '#1677ff' }

  return (
    <div>
      {/* Toolbar */}
      <div
        style={{
          marginBottom: 10,
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        {/* Legend */}
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', fontSize: 12, color: '#555' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <span
              style={{
                width: 14,
                height: 14,
                background: FLOOR_BG.REGULAR,
                border: '1px solid #d9d9d9',
                borderRadius: 3,
                display: 'inline-block',
              }}
            />
            Обычная
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <span
              style={{
                width: 14,
                height: 14,
                background: FLOOR_BG.VIP,
                border: '1px solid #d9d9d9',
                borderRadius: 3,
                display: 'inline-block',
              }}
            />
            VIP
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <span
              style={{
                width: 14,
                height: 14,
                background: VOID_BG,
                borderRadius: 3,
                display: 'inline-block',
              }}
            />
            Вне клуба
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <span
              style={{
                width: 18,
                height: 4,
                background: '#434343',
                borderRadius: 2,
                display: 'inline-block',
              }}
            />
            Стена
          </span>
          <span style={{ color: '#aaa' }}>
            {cols}×{rows} кл. · {gridSize}px
          </span>
        </div>

        {/* Zoom */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: 'auto' }}>
          <Tooltip title="Уменьшить">
            <Button
              size="small"
              icon={<ZoomOutOutlined />}
              onClick={() => {
                setAutoFit(false)
                setZoom((z) => clampZoom(z - ZOOM_STEP))
              }}
              disabled={zoom <= MIN_ZOOM}
            />
          </Tooltip>
          <span
            style={{
              minWidth: 48,
              textAlign: 'center',
              fontSize: 12,
              color: '#555',
              userSelect: 'none',
            }}
          >
            {formatZoomLabel(zoom)}
          </span>
          <Tooltip title="Увеличить">
            <Button
              size="small"
              icon={<ZoomInOutlined />}
              onClick={() => {
                setAutoFit(false)
                setZoom((z) => clampZoom(z + ZOOM_STEP))
              }}
              disabled={zoom >= MAX_ZOOM}
            />
          </Tooltip>
          {Math.abs(zoom - fitZoom) > 0.001 && (
            <Tooltip title="Показать схему целиком">
              <Button
                size="small"
                icon={<CompressOutlined />}
                onClick={() => {
                  setAutoFit(true)
                  setZoom(fitZoom)
                }}
              />
            </Tooltip>
          )}
        </div>
      </div>

      {/* Tool selector */}
      {!readOnly && (
        <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <Segmented
            size="small"
            value={tool}
            onChange={(v) => setTool(v as Tool)}
            options={[
              { label: 'Комната', value: 'room' },
              { label: 'Место', value: 'seat' },
              { label: 'Стена', value: 'wall' },
              { label: 'Ластик', value: 'erase' },
            ]}
          />
          {tool === 'room' && (
            <Select
              size="small"
              value={roomType}
              onChange={setRoomType}
              style={{ width: 110 }}
              options={[
                { value: 'REGULAR', label: 'Обычная' },
                { value: 'VIP', label: 'VIP' },
              ]}
            />
          )}
          <span style={{ fontSize: 12, color: '#aaa' }}>
            {tool === 'room' && 'Зажмите и выделите прямоугольник — создать комнату.'}
            {tool === 'seat' && 'Кликните по ячейке комнаты — разместить/убрать место.'}
            {tool === 'wall' && 'Кликните между ячейками — поставить/убрать стену.'}
            {tool === 'erase' && 'Зажмите и выделите область — удалить комнату и места.'}
          </span>
        </div>
      )}

      {/* Scrollable grid */}
      <div
        ref={viewportRef}
        style={{
          overflowX: 'auto',
          overflowY: 'auto',
          maxHeight: 'calc(100vh - 420px)',
          border: '1px solid #d9d9d9',
          borderRadius: 6,
          background: VOID_BG,
        }}
      >
        {/*
          display:inline-block — размер wrapper определяется содержимым (сеткой),
          а не viewport'ом; иначе inset:0 у оверлеев обрежет их по видимой области.
        */}
        <div
          style={{
            position: 'relative',
            display: 'inline-block',
            padding: PADDING,
            minWidth: '100%',
            boxSizing: 'border-box',
            cursor: wrapperCursor,
            userSelect: 'none',
          }}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
        >
          {/* Сетка ячеек */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${cols}, ${cellPx}px)`,
              gap: GAP,
            }}
          >
            {Array.from({ length: rows * cols }, (_, i) => {
              const row = Math.floor(i / cols)
              const col = i % cols
              const key = cellKey(col, row)
              const seatId = cellMap.get(key) ?? null
              const seat = seatId !== null ? seatById.get(seatId) : undefined
              const occupied = seatId !== null
              const isVip = seat?.type === 'VIP'
              const isActiveCell = activeCell?.col === col && activeCell?.row === row
              const floorCell = floorMap.get(key)
              const isVoid = hasFloor && !floorCell

              const bgColor = occupied
                ? isVip
                  ? '#faad14'
                  : '#1677ff'
                : isVoid
                  ? VOID_BG
                  : floorCell
                    ? FLOOR_BG[floorCell.roomType]
                    : FLOOR_BG.REGULAR

              const borderColor = isActiveCell
                ? '#ff4d4f'
                : occupied
                  ? isVip
                    ? '#d48806'
                    : '#0958d9'
                  : isVoid
                    ? '#d4d6da'
                    : '#d9d9d9'
              const borderWidth = isActiveCell ? 2 : 1

              const canInteract = !readOnly && !isVoid
              const cellCursor =
                tool === 'room' || tool === 'erase'
                  ? 'default'
                  : tool === 'seat' && canInteract
                    ? occupied
                      ? 'pointer'
                      : 'cell'
                    : 'default'

              return (
                <div
                  key={key}
                  onClick={() => handleCellClick(col, row)}
                  title={
                    occupied && seat
                      ? `${seat.label}${seat.type === 'VIP' ? ' · VIP' : ''}`
                      : isVoid
                        ? ''
                        : !readOnly && tool === 'seat'
                          ? `Разместить место (${col + 1}, ${row + 1})`
                          : ''
                  }
                  style={{
                    width: cellPx,
                    height: cellPx,
                    boxSizing: 'border-box',
                    borderRadius: isVoid ? 3 : 5,
                    border: `${borderWidth}px solid ${borderColor}`,
                    background: bgColor,
                    cursor: cellCursor,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 1,
                    userSelect: 'none',
                    overflow: 'hidden',
                    opacity: isVoid ? 0.5 : occupied ? 1 : 0.85,
                    boxShadow: occupied ? '0 1px 3px rgba(0,0,0,0.15)' : 'none',
                    // в режиме комнаты/ластика — пропускаем события через ячейки на wrapper
                    pointerEvents: tool === 'room' || tool === 'erase' ? 'none' : 'auto',
                  }}
                >
                  {occupied ? (
                    <>
                      {showLabel && (
                        <span
                          style={{
                            fontSize,
                            fontWeight: 700,
                            color: '#fff',
                            lineHeight: 1,
                            textAlign: 'center',
                            maxWidth: '100%',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            padding: '0 2px',
                          }}
                        >
                          {seat ? seat.label : `#${seatId}`}
                        </span>
                      )}
                      {showVipBadge && isVip && (
                        <span
                          style={{
                            fontSize: Math.max(8, fontSize - 2),
                            color: 'rgba(255,255,255,0.9)',
                            lineHeight: 1,
                            fontWeight: 600,
                            letterSpacing: 0.5,
                          }}
                        >
                          VIP
                        </span>
                      )}
                    </>
                  ) : (
                    !readOnly &&
                    tool === 'seat' &&
                    !isVoid && (
                      <span style={{ fontSize: cellPx * 0.35, color: '#bbb', lineHeight: 1 }}>
                        +
                      </span>
                    )
                  )}
                </div>
              )
            })}
          </div>

          {/* Слой отрисовки стен */}
          <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
            {items
              .filter((i): i is WallItem => i.type === 'WALL')
              .map((w) => {
                const key = wallKey(w.orientation, w.col, w.row)
                const hovered = hoveredEdge === key
                if (w.orientation === 'H') {
                  return (
                    <div
                      key={key}
                      style={{
                        position: 'absolute',
                        left: cellLeft(w.col),
                        top: cellTop(w.row) - GAP / 2 - WALL_THICKNESS / 2,
                        width: cellPx,
                        height: WALL_THICKNESS,
                        background: hovered ? '#ff4d4f' : '#2a2a2a',
                        borderRadius: 2,
                      }}
                    />
                  )
                } else {
                  return (
                    <div
                      key={key}
                      style={{
                        position: 'absolute',
                        left: cellLeft(w.col) - GAP / 2 - WALL_THICKNESS / 2,
                        top: cellTop(w.row),
                        width: WALL_THICKNESS,
                        height: cellPx,
                        background: hovered ? '#ff4d4f' : '#2a2a2a',
                        borderRadius: 2,
                      }}
                    />
                  )
                }
              })}
          </div>

          {/* Зоны клика на рёбрах (только режим стены) */}
          {!readOnly && tool === 'wall' && (
            <div style={{ position: 'absolute', inset: 0 }}>
              {Array.from({ length: rows - 1 }, (_, ri) =>
                Array.from({ length: cols }, (_, ci) => {
                  const row = ri + 1
                  const key = wallKey('H', ci, row)
                  const hovered = hoveredEdge === key
                  const has = wallSet.has(key)
                  return (
                    <div
                      key={key}
                      onClick={() => toggleWall('H', ci, row)}
                      onMouseEnter={() => setHoveredEdge(key)}
                      onMouseLeave={() => setHoveredEdge(null)}
                      style={{
                        position: 'absolute',
                        left: cellLeft(ci),
                        top: cellTop(row) - HIT_SIZE / 2,
                        width: cellPx,
                        height: HIT_SIZE,
                        cursor: 'pointer',
                        borderRadius: 2,
                        background: hovered
                          ? has
                            ? 'rgba(255,77,79,0.22)'
                            : 'rgba(42,42,42,0.18)'
                          : 'transparent',
                        zIndex: 2,
                      }}
                    />
                  )
                }),
              )}
              {Array.from({ length: rows }, (_, ri) =>
                Array.from({ length: cols - 1 }, (_, ci) => {
                  const col = ci + 1
                  const key = wallKey('V', col, ri)
                  const hovered = hoveredEdge === key
                  const has = wallSet.has(key)
                  return (
                    <div
                      key={key}
                      onClick={() => toggleWall('V', col, ri)}
                      onMouseEnter={() => setHoveredEdge(key)}
                      onMouseLeave={() => setHoveredEdge(null)}
                      style={{
                        position: 'absolute',
                        left: cellLeft(col) - HIT_SIZE / 2,
                        top: cellTop(ri),
                        width: HIT_SIZE,
                        height: cellPx,
                        cursor: 'pointer',
                        borderRadius: 2,
                        background: hovered
                          ? has
                            ? 'rgba(255,77,79,0.22)'
                            : 'rgba(42,42,42,0.18)'
                          : 'transparent',
                        zIndex: 2,
                      }}
                    />
                  )
                }),
              )}
            </div>
          )}

          {/* Превью выделения при перетаскивании */}
          {selectionRect !== null && (
            <div
              style={{
                position: 'absolute',
                left: cellLeft(selectionRect.minCol),
                top: cellTop(selectionRect.minRow),
                width:
                  (selectionRect.maxCol - selectionRect.minCol + 1) * (cellPx + GAP) - GAP,
                height:
                  (selectionRect.maxRow - selectionRect.minRow + 1) * (cellPx + GAP) - GAP,
                background: selOverlayColor.bg,
                border: `2px dashed ${selOverlayColor.border}`,
                borderRadius: 4,
                pointerEvents: 'none',
                zIndex: 3,
              }}
            />
          )}
        </div>
      </div>

      {/* Stats */}
      <div style={{ marginTop: 8, fontSize: 12, color: '#888', display: 'flex', gap: 16 }}>
        <span>
          Ячеек комнат: <strong style={{ color: '#333' }}>{floorCount}</strong>
        </span>
        <span>
          Мест: <strong style={{ color: '#333' }}>{seatCount}</strong> /{' '}
          {seats.filter((s) => s.isActive).length}
        </span>
        <span>
          Стен: <strong style={{ color: '#333' }}>{autoWallCount + wallCount}</strong>
          {wallCount > 0 && (
            <span style={{ color: '#aaa' }}> ({wallCount} ручных)</span>
          )}
        </span>
      </div>

      {/* Modal: place seat */}
      <Modal
        open={isPlaceModal}
        title={activeCell ? `Ячейка (${activeCell.col + 1}, ${activeCell.row + 1})` : ''}
        onCancel={closeModal}
        onOk={placeSeat}
        okText="Разместить"
        cancelText="Отмена"
        okButtonProps={{ disabled: pickSeatId === null }}
        destroyOnClose
      >
        {availableSeats.length === 0 ? (
          <Empty description="Все активные места уже размещены на схеме" />
        ) : (
          <>
            <p style={{ color: '#666', fontSize: 13, marginBottom: 8 }}>Выберите место:</p>
            <Select
              style={{ width: '100%' }}
              placeholder="Место..."
              value={pickSeatId}
              onChange={setPickSeatId}
              showSearch
              autoFocus
              filterOption={(input, option) =>
                (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
              }
              options={availableSeats
                .slice()
                .sort((a, b) => a.sortOrder - b.sortOrder)
                .map((s) => ({
                  value: s.id,
                  label: `${s.label} — ${s.type === 'VIP' ? 'VIP' : 'Обычное'}`,
                }))}
            />
          </>
        )}
      </Modal>

      {/* Modal: seat info + remove */}
      <Modal
        open={isInfoModal}
        title={
          activeSeat ? (
            <span>
              Место: <strong>{activeSeat.label}</strong>
              <Tag
                color={activeSeat.type === 'VIP' ? 'gold' : 'blue'}
                style={{ marginLeft: 8 }}
              >
                {activeSeat.type === 'VIP' ? 'VIP' : 'Обычное'}
              </Tag>
            </span>
          ) : (
            ''
          )
        }
        onCancel={closeModal}
        footer={
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <Button onClick={closeModal}>Закрыть</Button>
            {!readOnly && (
              <Button
                danger
                icon={<DeleteOutlined />}
                onClick={() => activeCell && removeSeat(activeCell.col, activeCell.row)}
              >
                Убрать с карты
              </Button>
            )}
          </div>
        }
        destroyOnClose
      >
        {activeSeat && (
          <div style={{ color: '#555', fontSize: 13 }}>
            Ячейка: ({activeCell?.col !== undefined ? activeCell.col + 1 : '—'},{' '}
            {activeCell?.row !== undefined ? activeCell.row + 1 : '—'})
          </div>
        )}
      </Modal>
    </div>
  )
}
