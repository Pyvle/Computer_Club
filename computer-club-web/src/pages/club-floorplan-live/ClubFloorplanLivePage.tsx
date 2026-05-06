import { useState, useEffect, useMemo, useCallback, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  DatePicker,
  Button,
  Spin,
  Alert,
  Drawer,
  Tag,
  Typography,
  Descriptions,
  Space,
} from 'antd'
import { ReloadOutlined, ClockCircleOutlined } from '@ant-design/icons'
import dayjs, { Dayjs } from 'dayjs'
import apiClient from '../../utils/apiClient'
import type {
  AdminSeatResponse,
  FloorplanResponse,
  FloorplanSummaryResponse,
  FloorplanBookingEntry,
} from '../../types'

// --- Floorplan render types (зеркало FloorplanEditor) ---

type RoomType = 'REGULAR' | 'VIP'
type SeatItem = { type: 'SEAT'; seatId: number; col: number; row: number }
type WallItem = { type: 'WALL'; orientation: 'H' | 'V'; col: number; row: number; auto?: boolean }
type FloorItem = { type: 'FLOOR'; col: number; row: number; roomType: RoomType }
type FloorplanItem = SeatItem | WallItem | FloorItem

// --- Константы (идентичны FloorplanEditor) ---

const GAP = 2
const PADDING = 8
const WALL_THICKNESS = 4
const MIN_CELL_PX = 36
const MIN_ZOOM = 0.35
const MAX_ZOOM = 8
const ZOOM_STEP = 0.25

const FLOOR_BG: Record<RoomType, string> = { REGULAR: '#ffffff', VIP: '#fff8e6' }
const VOID_BG = '#e2e4e8'

function clampZoom(value: number) {
  return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Number.isFinite(value) ? value : 1))
}

function formatZoomLabel(value: number) {
  return `${Math.round(value * 100)}%`
}
const SEAT_REGULAR_FREE = '#e6f4ff'
const SEAT_VIP_FREE = '#fff7d6'
const SEAT_UPCOMING = '#ffd591'
const SEAT_ACTIVE = '#95de64'

// --- Утилиты (идентичны FloorplanEditor) ---

function cellKey(col: number, row: number) {
  return `${col},${row}`
}

function wallKey(orientation: 'H' | 'V', col: number, row: number) {
  return `${orientation}:${col},${row}`
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

// --- FloorplanViewer ---

interface ViewerProps {
  floorplan: FloorplanResponse
  seats: AdminSeatResponse[]
  bookingBySeatId: Map<number, FloorplanBookingEntry>
  onSeatClick: (seatId: number) => void
}

function FloorplanViewer({ floorplan, seats, bookingBySeatId, onSeatClick }: ViewerProps) {
  const { width, height, gridSize } = floorplan
  const cols = Math.max(1, Math.floor(width / gridSize))
  const rows = Math.max(1, Math.floor(height / gridSize))

  const viewportRef = useRef<HTMLDivElement | null>(null)
  const [fitZoom, setFitZoom] = useState(1)
  const [autoFit, setAutoFit] = useState(true)
  const [zoom, setZoom] = useState(1)
  const cellPx = gridSize * zoom

  const items = useMemo(
    () => parseItems(floorplan.data, width, height, gridSize),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [floorplan.id, floorplan.version],
  )

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

  const hasFloor = floorMap.size > 0

  const seatById = useMemo(() => {
    const map = new Map<number, AdminSeatResponse>()
    for (const s of seats) map.set(s.id, s)
    return map
  }, [seats])

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
        }}>
          −
        </Button>
        <span style={{ fontSize: 12, color: '#666' }}>{formatZoomLabel(zoom)}</span>
        <Button size="small" onClick={() => {
          setAutoFit(false)
          setZoom((z) => clampZoom(z + ZOOM_STEP))
        }}>
          +
        </Button>
        <Button size="small" onClick={() => {
          setAutoFit(true)
          setZoom(fitZoom)
        }}>
          целиком
        </Button>
      </div>

      <div
        ref={viewportRef}
        style={{
          overflowX: 'auto',
          overflowY: 'auto',
          maxHeight: 'calc(100vh - 320px)',
          border: '1px solid #d9d9d9',
          borderRadius: 6,
          background: VOID_BG,
        }}
      >
        <div
          style={{
            position: 'relative',
            display: 'inline-block',
            padding: PADDING,
            minWidth: '100%',
            boxSizing: 'border-box',
          }}
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
              const booking = seatId !== null ? bookingBySeatId.get(seatId) : undefined
              const isVoid = hasFloor && !floorMap.get(key)
              const floorCell = floorMap.get(key)

              let bgColor: string
              if (isVoid) {
                bgColor = VOID_BG
              } else if (seatId !== null && booking) {
                bgColor = booking.status === 'ACTIVE' ? SEAT_ACTIVE : SEAT_UPCOMING
              } else if (seatId !== null) {
                bgColor = seat?.type === 'VIP' ? SEAT_VIP_FREE : SEAT_REGULAR_FREE
              } else {
                bgColor = floorCell ? FLOOR_BG[floorCell.roomType] : FLOOR_BG.REGULAR
              }

              let borderColor = '#d9d9d9'
              if (seatId !== null && booking) {
                borderColor = booking.status === 'ACTIVE' ? '#389e0d' : '#d46b08'
              } else if (seatId !== null) {
                borderColor = seat?.type === 'VIP' ? '#d48806' : '#91caff'
              } else if (isVoid) {
                borderColor = '#d4d6da'
              }

              const hasSeat = seatId !== null && !isVoid

              return (
                <div
                  key={key}
                  onClick={() => hasSeat && onSeatClick(seatId!)}
                  style={{
                    width: cellPx,
                    height: cellPx,
                    boxSizing: 'border-box',
                    borderRadius: isVoid ? 3 : 5,
                    border: `1px solid ${borderColor}`,
                    background: bgColor,
                    cursor: hasSeat ? 'pointer' : 'default',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 1,
                    userSelect: 'none',
                    overflow: 'hidden',
                    opacity: isVoid ? 0.5 : 1,
                    boxShadow: hasSeat ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                    transition: 'opacity 0.15s',
                  }}
                >
                  {hasSeat && showLabel && (
                    <>
                      <span
                        style={{
                          fontSize,
                          fontWeight: 700,
                          color: booking ? '#333' : '#333',
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
                      {seat?.type === 'VIP' && (
                        <span
                          style={{
                            fontSize: Math.max(8, fontSize - 2),
                            color: '#ad6800',
                            lineHeight: 1,
                            fontWeight: 600,
                            letterSpacing: 0.5,
                          }}
                        >
                          VIP
                        </span>
                      )}
                    </>
                  )}
                </div>
              )
            })}
          </div>

          {/* Слой стен */}
          <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
            {items
              .filter((i): i is WallItem => i.type === 'WALL')
              .map((w) => {
                const key = wallKey(w.orientation, w.col, w.row)
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
                        background: '#2a2a2a',
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
                        background: '#2a2a2a',
                        borderRadius: 2,
                      }}
                    />
                  )
                }
              })}
          </div>
        </div>
      </div>
    </div>
  )
}

// --- Легенда ---

function Legend({ active, upcoming, free }: { active: number; upcoming: number; free: number }) {
  const item = (color: string, border: string, label: string, count: number) => (
    <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
      <span
        style={{
          display: 'inline-block',
          width: 14,
          height: 14,
          background: color,
          border: `1px solid ${border}`,
          borderRadius: 3,
          flexShrink: 0,
        }}
      />
      {label} ({count})
    </span>
  )

  return (
    <Space wrap size={16} style={{ marginBottom: 16 }}>
      {item(SEAT_ACTIVE, '#389e0d', 'Активно', active)}
      {item(SEAT_UPCOMING, '#d46b08', 'Предстоит', upcoming)}
      {item(SEAT_REGULAR_FREE, '#91caff', 'Обычное свободно', free)}
      {item(SEAT_VIP_FREE, '#d48806', 'VIP свободно', 0)}
    </Space>
  )
}

// --- Главный компонент ---

export default function ClubFloorplanLivePage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()

  const [selectedAt, setSelectedAt] = useState<Dayjs>(dayjs())
  const [floorplan, setFloorplan] = useState<FloorplanResponse | null>(null)
  const [seats, setSeats] = useState<AdminSeatResponse[]>([])
  const [bookings, setBookings] = useState<FloorplanBookingEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [bookingsLoading, setBookingsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [selectedSeatId, setSelectedSeatId] = useState<number | null>(null)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const bookingBySeatId = useMemo(() => {
    const map = new Map<number, FloorplanBookingEntry>()
    for (const b of bookings) map.set(b.seatId, b)
    return map
  }, [bookings])

  const seatById = useMemo(() => {
    const map = new Map<number, AdminSeatResponse>()
    for (const s of seats) map.set(s.id, s)
    return map
  }, [seats])

  const loadFloorplan = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    setError(null)
    try {
      const list: FloorplanSummaryResponse[] = (
        await apiClient.get(`/admin/clubs/${clubId}/floorplans`)
      ).data
      const published = list.find((f) => f.status === 'PUBLISHED')
      if (!published) {
        setFloorplan(null)
        setLoading(false)
        return
      }
      const [fpRes, seatsRes] = await Promise.all([
        apiClient.get(`/admin/clubs/${clubId}/floorplans/${published.id}`),
        apiClient.get(`/admin/clubs/${clubId}/seats`),
      ])
      setFloorplan(fpRes.data)
      setSeats(seatsRes.data)
    } catch {
      setError('Не удалось загрузить схему зала')
    } finally {
      setLoading(false)
    }
  }, [clubId])

  const loadBookings = useCallback(
    async (at: Dayjs) => {
      if (!clubId) return
      setBookingsLoading(true)
      try {
        const atStr = at.format('YYYY-MM-DDTHH:mm:ss')
        const res = await apiClient.get(
          `/admin/clubs/${clubId}/floorplan-bookings?at=${atStr}`,
        )
        setBookings(res.data)
      } catch {
        // не блокируем UI при ошибке обновления
      } finally {
        setBookingsLoading(false)
      }
    },
    [clubId],
  )

  useEffect(() => {
    loadFloorplan()
  }, [loadFloorplan])

  useEffect(() => {
    loadBookings(selectedAt)
  }, [loadBookings, selectedAt])

  // авто-обновление каждые 30 с
  useEffect(() => {
    if (autoRefresh) {
      timerRef.current = setInterval(() => {
        const now = dayjs()
        setSelectedAt(now)
        loadBookings(now)
      }, 30_000)
    } else {
      if (timerRef.current) clearInterval(timerRef.current)
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [autoRefresh, loadBookings])

  function handleSeatClick(seatId: number) {
    setSelectedSeatId(seatId)
    setDrawerOpen(true)
  }

  const selectedBooking = selectedSeatId ? bookingBySeatId.get(selectedSeatId) : undefined
  const selectedSeat = selectedSeatId ? seatById.get(selectedSeatId) : undefined

  const activeCount = bookings.filter((b) => b.status === 'ACTIVE').length
  const upcomingCount = bookings.filter((b) => b.status === 'UPCOMING').length
  const freeSeats = seats.filter((s) => s.isActive && !bookingBySeatId.has(s.id)).length

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />
  if (error) return <Alert type="error" message={error} />

  return (
    <div>
      <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 16 }}>
        Занятость зала
      </Typography.Title>

      {/* Панель управления */}
      <Space wrap style={{ marginBottom: 16 }}>
        <DatePicker
          showTime={{ format: 'HH:mm' }}
          format="DD.MM.YYYY HH:mm"
          value={selectedAt}
          onChange={(v) => v && setSelectedAt(v)}
          allowClear={false}
        />
        <Button icon={<ClockCircleOutlined />} onClick={() => setSelectedAt(dayjs())}>
          Сейчас
        </Button>
        <Button
          icon={<ReloadOutlined spin={bookingsLoading} />}
          onClick={() => loadBookings(selectedAt)}
          loading={bookingsLoading}
        >
          Обновить
        </Button>
        <Button
          type={autoRefresh ? 'primary' : 'default'}
          onClick={() => setAutoRefresh((v) => !v)}
        >
          Авто {autoRefresh ? 'вкл' : 'выкл'}
        </Button>
      </Space>

      {/* Легенда */}
      <Legend active={activeCount} upcoming={upcomingCount} free={freeSeats} />

      {/* Схема */}
      {!floorplan ? (
        <Alert
          type="info"
          message="Нет опубликованной схемы зала"
          description='Опубликуйте схему в разделе "Схемы зала"'
        />
      ) : (
        <FloorplanViewer
          floorplan={floorplan}
          seats={seats}
          bookingBySeatId={bookingBySeatId}
          onSeatClick={handleSeatClick}
        />
      )}

      {/* Drawer с информацией о месте */}
      <Drawer
        title={
          selectedSeat
            ? `${selectedSeat.label}${selectedSeat.type === 'VIP' ? ' · VIP' : ''}`
            : 'Место'
        }
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={340}
      >
        {selectedBooking ? (
          <>
            <div style={{ marginBottom: 16 }}>
              <Tag color={selectedBooking.status === 'ACTIVE' ? 'green' : 'orange'} style={{ fontSize: 14 }}>
                {selectedBooking.status === 'ACTIVE' ? 'Активно' : 'Предстоит'}
              </Tag>
            </div>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Телефон">
                {selectedBooking.userPhone ?? '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Начало">
                {dayjs(selectedBooking.startAt).format('DD.MM.YYYY HH:mm')}
              </Descriptions.Item>
              <Descriptions.Item label="Конец">
                {dayjs(selectedBooking.endAt).format('DD.MM.YYYY HH:mm')}
              </Descriptions.Item>
              <Descriptions.Item label="Сумма">
                {selectedBooking.totalRub} ₽
              </Descriptions.Item>
            </Descriptions>
            <Button
              type="link"
              style={{ marginTop: 16, padding: 0 }}
              onClick={() => {
                setDrawerOpen(false)
                navigate(`/admin/club/${clubId}/bookings/${selectedBooking.bookingId}`)
              }}
            >
              Открыть бронирование →
            </Button>
          </>
        ) : (
          <div style={{ color: '#52c41a', fontWeight: 500, fontSize: 15 }}>
            ✓ Свободно
          </div>
        )}
      </Drawer>
    </div>
  )
}
