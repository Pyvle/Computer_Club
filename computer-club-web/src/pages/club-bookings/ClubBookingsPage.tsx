import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  message,
  Row,
  Select,
  Segmented,
  Space,
  Spin,
  Table,
} from 'antd'
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import { tokens } from '../../theme/tokens'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { ReloadOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import type {
  AdminBookingResponse,
  AdminSeatResponse,
  BookingStatus,
  FloorplanBookingEntry,
  FloorplanResponse,
  FloorplanSummaryResponse,
} from '../../types'

// ─── Helpers: list view ──────────────────────────────────────────────────────


function formatDuration(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  return m === 0 ? `${h} ч` : `${h} ч ${m} мин`
}

// ─── Floorplan viewer types/constants ────────────────────────────────────────

type RoomType = 'REGULAR' | 'VIP'
type SeatItem = { type: 'SEAT'; seatId: number; col: number; row: number }
type WallItem = { type: 'WALL'; orientation: 'H' | 'V'; col: number; row: number; auto?: boolean }
type FloorItem = { type: 'FLOOR'; col: number; row: number; roomType: RoomType }
type FloorplanItem = SeatItem | WallItem | FloorItem
type SeatState = 'FREE' | 'OCCUPIED' | 'UNAVAILABLE'

const GAP = 2
const PADDING = 8
const WALL_THICKNESS = 4
const MIN_CELL_PX = 40
const MIN_ZOOM = 0.35
const MAX_ZOOM = 8
const ZOOM_STEP = 0.25

// цвета состояний мест
const SEAT_COLORS: Record<SeatState, { bg: string; border: string; text: string }> = {
  FREE:        { bg: tokens.colors.successSoft, border: tokens.colors.success, text: tokens.colors.success },
  OCCUPIED:    { bg: tokens.colors.errorSoft,   border: tokens.colors.error,   text: tokens.colors.error },
  UNAVAILABLE: { bg: '#bfbfbf', border: '#8c8c8c', text: '#fff' },
}
const FLOOR_BG: Record<RoomType, string> = { REGULAR: '#ffffff', VIP: '#fff8e6' }
const VOID_BG = '#e2e4e8'

function cellKey(col: number, row: number) { return `${col},${row}` }
function wallKey(o: 'H' | 'V', col: number, row: number) { return `${o}:${col},${row}` }

function parseFloorplanItems(raw: unknown, width: number, height: number, gridSize: number): FloorplanItem[] {
  if (!raw || typeof raw !== 'object') return []
  const d = raw as { items?: unknown[] }
  if (!Array.isArray(d.items)) return []
  const result: FloorplanItem[] = []
  for (const it of d.items) {
    if (!it || typeof it !== 'object') continue
    const item = it as Record<string, unknown>
    if (item.type === 'FLOOR') {
      if (typeof item.col === 'number' && typeof item.row === 'number' && (item.roomType === 'REGULAR' || item.roomType === 'VIP'))
        result.push({ type: 'FLOOR', col: item.col, row: item.row, roomType: item.roomType })
      continue
    }
    if (item.type === 'WALL') {
      if ((item.orientation === 'H' || item.orientation === 'V') && typeof item.col === 'number' && typeof item.row === 'number')
        result.push({ type: 'WALL', orientation: item.orientation as 'H' | 'V', col: item.col, row: item.row, auto: item.auto === true })
      continue
    }
    if (item.type !== 'SEAT') continue
    const seatId = typeof item.seatId === 'number' ? item.seatId : null
    if (seatId === null) continue
    if (typeof item.col === 'number' && typeof item.row === 'number') {
      result.push({ type: 'SEAT', seatId, col: item.col, row: item.row }); continue
    }
    // legacy x/y/w/h
    const x = typeof item.x === 'number' ? item.x : 0
    const y = typeof item.y === 'number' ? item.y : 0
    const w = typeof item.w === 'number' ? item.w : 0
    result.push({ type: 'SEAT', seatId, col: w >= 1 ? Math.round(x / gridSize) : Math.round(x * width / gridSize), row: w >= 1 ? Math.round(y / gridSize) : Math.round(y * height / gridSize) })
  }
  return result
}

function getSeatState(seatId: number, seatById: Map<number, AdminSeatResponse>, bookingBySeatId: Map<number, FloorplanBookingEntry>): SeatState {
  const seat = seatById.get(seatId)
  if (!seat || !seat.isActive) return 'UNAVAILABLE'
  if (bookingBySeatId.has(seatId)) return 'OCCUPIED'
  return 'FREE'
}

function clampZoom(value: number) {
  return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Number.isFinite(value) ? value : 1))
}

function formatZoomLabel(value: number) {
  return `${Math.round(value * 100)}%`
}

// ─── FloorplanViewer ─────────────────────────────────────────────────────────

interface ViewerProps {
  floorplan: FloorplanResponse
  seatById: Map<number, AdminSeatResponse>
  bookingBySeatId: Map<number, FloorplanBookingEntry>
  selectedSeatId: number | null
  onSeatClick: (seatId: number) => void
}

function FloorplanViewer({ floorplan, seatById, bookingBySeatId, selectedSeatId, onSeatClick }: ViewerProps) {
  const { width, height, gridSize } = floorplan
  const cols = Math.max(1, Math.floor(width / gridSize))
  const rows = Math.max(1, Math.floor(height / gridSize))
  const viewportRef = useRef<HTMLDivElement | null>(null)
  const [fitZoom, setFitZoom] = useState(1)
  const [autoFit, setAutoFit] = useState(true)
  const [zoom, setZoom] = useState(1)
  const cellPx = gridSize * zoom

  const items = useMemo(
    () => parseFloorplanItems(floorplan.data, width, height, gridSize),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [floorplan.id, floorplan.version],
  )

  const cellMap = useMemo(() => {
    const m = new Map<string, number>()
    for (const i of items) if (i.type === 'SEAT') m.set(cellKey(i.col, i.row), i.seatId)
    return m
  }, [items])

  const floorMap = useMemo(() => {
    const m = new Map<string, FloorItem>()
    for (const i of items) if (i.type === 'FLOOR') m.set(cellKey(i.col, i.row), i)
    return m
  }, [items])

  const hasFloor = floorMap.size > 0
  const fontSize = Math.max(8, Math.min(11, cellPx * 0.28))
  const showEndTime = cellPx >= 48
  const cLeft = (c: number) => PADDING + c * (cellPx + GAP)
  const cTop = (r: number) => PADDING + r * (cellPx + GAP)

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
      {/* Легенда + управление зумом в одну строку */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', gap: 14 }}>
          {(Object.entries(SEAT_COLORS) as [SeatState, typeof SEAT_COLORS[SeatState]][]).map(([state, c]) => (
            <span key={state} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12, color: tokens.colors.textSecondary }}>
              <span style={{ display: 'inline-block', width: 13, height: 13, background: c.bg, border: `1px solid ${c.border}`, borderRadius: 3 }} />
              {{ FREE: 'Свободно', OCCUPIED: 'Занято', UNAVAILABLE: 'Недоступно' }[state]}
            </span>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
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
      </div>

      <div ref={viewportRef} style={{ overflowX: 'auto', overflowY: 'auto', maxHeight: 'calc(100vh - 360px)', border: `1px solid ${tokens.colors.border}`, borderRadius: tokens.radius.sm, background: VOID_BG }}>
        <div style={{ position: 'relative', display: 'inline-block', padding: PADDING, minWidth: '100%', boxSizing: 'border-box' }}>
          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, ${cellPx}px)`, gap: GAP }}>
            {Array.from({ length: rows * cols }, (_, i) => {
              const row = Math.floor(i / cols)
              const col = i % cols
              const key = cellKey(col, row)
              const seatId = cellMap.get(key) ?? null
              const isVoid = hasFloor && !floorMap.get(key)
              const floorCell = floorMap.get(key)
              const state: SeatState | null = seatId !== null ? getSeatState(seatId, seatById, bookingBySeatId) : null
              const isSelected = seatId !== null && seatId === selectedSeatId
              const booking = seatId !== null ? bookingBySeatId.get(seatId) : undefined
              const colors = state ? SEAT_COLORS[state] : null

              const bgColor = colors ? colors.bg : isVoid ? VOID_BG : floorCell ? FLOOR_BG[floorCell.roomType] : FLOOR_BG.REGULAR
              const borderColor = isSelected ? tokens.colors.info : colors ? colors.border : tokens.colors.border
              const borderWidth = isSelected ? 2 : 1
              const seat = seatId !== null ? seatById.get(seatId) : undefined

              return (
                <div
                  key={key}
                  onClick={() => seatId !== null && !isVoid && state !== 'UNAVAILABLE' && onSeatClick(seatId)}
                  style={{
                    width: cellPx, height: cellPx,
                    boxSizing: 'border-box',
                    borderRadius: isVoid ? 3 : 5,
                    border: `${borderWidth}px solid ${borderColor}`,
                    background: bgColor,
                    cursor: seatId !== null && !isVoid && state !== 'UNAVAILABLE' ? 'pointer' : 'default',
                    display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                    gap: 1, userSelect: 'none', overflow: 'hidden',
                    opacity: isVoid ? 0.5 : state === 'UNAVAILABLE' ? 0.7 : 1,
                    boxShadow: isSelected ? `0 0 0 2px ${tokens.colors.infoSoft}` : seatId ? '0 1px 2px rgba(0,0,0,0.1)' : 'none',
                  }}
                >
                  {seatId !== null && cellPx >= 28 && colors && (
                    <>
                      <span style={{ fontSize, fontWeight: 700, color: colors.text, lineHeight: 1, textAlign: 'center', maxWidth: '100%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', padding: '0 2px' }}>
                        {seat?.label ?? `#${seatId}`}
                      </span>
                      {showEndTime && booking && (
                        <span style={{ fontSize: Math.max(7, fontSize - 2), color: colors.text, lineHeight: 1, opacity: 0.85 }}>
                          до {dayjs(booking.endAt).format('HH:mm')}
                        </span>
                      )}
                    </>
                  )}
                </div>
              )
            })}
          </div>

          {/* Стены */}
          <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
            {items.filter((i): i is WallItem => i.type === 'WALL').map(w => {
              const key = wallKey(w.orientation, w.col, w.row)
              return w.orientation === 'H' ? (
                <div key={key} style={{ position: 'absolute', left: cLeft(w.col), top: cTop(w.row) - GAP / 2 - WALL_THICKNESS / 2, width: cellPx, height: WALL_THICKNESS, background: '#2a2a2a', borderRadius: 2 }} />
              ) : (
                <div key={key} style={{ position: 'absolute', left: cLeft(w.col) - GAP / 2 - WALL_THICKNESS / 2, top: cTop(w.row), width: WALL_THICKNESS, height: cellPx, background: '#2a2a2a', borderRadius: 2 }} />
              )
            })}
          </div>
        </div>
      </div>

    </div>
  )
}

// ─── InfoPanel ────────────────────────────────────────────────────────────────

const PAYMENT_LABELS: Record<string, { label: string; color: string }> = {
  PAID:     { label: 'Оплачено',        color: 'success' },
  CREATED:  { label: 'Ожидает оплаты',  color: 'warning' },
  FAILED:   { label: 'Не оплачено',     color: 'error' },
  CANCELED: { label: 'Отменено',        color: 'default' },
  REFUND:   { label: 'Возврат',         color: 'purple' },
}

interface InfoPanelProps {
  seat: AdminSeatResponse
  booking: FloorplanBookingEntry | undefined
  onNavigate: (bookingId: number) => void
}

function InfoPanel({ seat, booking, onNavigate }: InfoPanelProps) {
  const state: SeatState = !seat.isActive ? 'UNAVAILABLE' : booking ? 'OCCUPIED' : 'FREE'
  const payment = booking?.paymentStatus ? PAYMENT_LABELS[booking.paymentStatus] : null

  const stateStyle: Record<SeatState, { bg: string; color: string; label: string }> = {
    FREE:        { bg: tokens.colors.successSoft, color: tokens.colors.success, label: 'Свободно' },
    OCCUPIED:    { bg: tokens.colors.errorSoft,   color: tokens.colors.error,   label: 'Занято' },
    UNAVAILABLE: { bg: '#f5f5f5',                 color: tokens.colors.textMuted, label: 'Неактивно' },
  }
  const ss = stateStyle[state]

  return (
    <div style={{ fontSize: 13 }}>
      {/* Заголовок места */}
      <div style={{
        background: ss.bg,
        borderRadius: tokens.radius.sm,
        padding: '12px 14px',
        marginBottom: 14,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <div>
          <div style={{ fontSize: 24, fontWeight: 800, color: tokens.colors.text, lineHeight: 1 }}>
            {seat.label}
          </div>
          <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 3 }}>
            {seat.type === 'VIP' ? 'VIP' : 'Стандартное'}
          </div>
        </div>
        <div style={{
          fontSize: 11, fontWeight: 700,
          color: ss.color,
          background: 'white',
          border: `1.5px solid ${ss.color}`,
          borderRadius: 20,
          padding: '3px 10px',
          textTransform: 'uppercase',
          letterSpacing: '0.04em',
        }}>
          {ss.label}
        </div>
      </div>

      {/* Пустые состояния */}
      {state !== 'OCCUPIED' && (
        <div style={{ color: tokens.colors.textMuted, textAlign: 'center', padding: '12px 0', fontSize: 13 }}>
          {state === 'FREE' ? 'Место свободно' : 'Место не используется'}
        </div>
      )}

      {/* Активная бронь */}
      {state === 'OCCUPIED' && booking && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {/* Время — главный блок */}
          <div style={{
            background: tokens.colors.errorSoft,
            border: `1px solid ${tokens.colors.error}30`,
            borderRadius: tokens.radius.sm,
            padding: '10px 14px',
          }}>
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginBottom: 4, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Время сеанса
            </div>
            <div style={{ fontSize: 20, fontWeight: 800, color: tokens.colors.text, lineHeight: 1 }}>
              {dayjs(booking.startAt).format('HH:mm')}
              <span style={{ fontWeight: 400, color: tokens.colors.textMuted, fontSize: 16 }}> — </span>
              {dayjs(booking.endAt).format('HH:mm')}
            </div>
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 4 }}>
              {dayjs(booking.startAt).format('DD.MM.YYYY')}
            </div>
          </div>

          {/* Детали */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: tokens.colors.textMuted }}>Бронь</span>
              <span style={{ fontWeight: 700 }}>#{booking.bookingId}</span>
            </div>

            {booking.userPhone && (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: tokens.colors.textMuted }}>Пользователь</span>
                <span style={{ fontWeight: 600 }}>{booking.userPhone}</span>
              </div>
            )}

            {payment && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: tokens.colors.textMuted }}>Оплата</span>
                <StatusBadge
                  label={payment.label}
                  variant={payment.color === 'success' ? 'success' : payment.color === 'error' ? 'error' : payment.color === 'warning' ? 'warning' : 'info'}
                />
              </div>
            )}
          </div>

          <Button
            type="primary"
            block
            size="large"
            style={{ marginTop: 4 }}
            onClick={() => onNavigate(booking.bookingId)}
          >
            Открыть бронь →
          </Button>
        </div>
      )}
    </div>
  )
}

// ─── FloorplanTab ─────────────────────────────────────────────────────────────

function FloorplanTab({ clubId }: { clubId: string }) {
  const navigate = useNavigate()

  const [floorplans, setFloorplans] = useState<FloorplanSummaryResponse[]>([])
  const [selectedFloorplanId, setSelectedFloorplanId] = useState<number | null>(null)
  const [floorplan, setFloorplan] = useState<FloorplanResponse | null>(null)
  const [seats, setSeats] = useState<AdminSeatResponse[]>([])
  const [bookings, setBookings] = useState<FloorplanBookingEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedSeatId, setSelectedSeatId] = useState<number | null>(null)
  const [lastRefreshed, setLastRefreshed] = useState<Dayjs | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const bookingBySeatId = useMemo(() => new Map(bookings.map(b => [b.seatId, b])), [bookings])
  const seatById = useMemo(() => new Map(seats.map(s => [s.id, s])), [seats])

  const refreshBookings = useCallback(async () => {
    setRefreshing(true)
    try {
      const res = await apiClient.get(`/admin/clubs/${clubId}/floorplan-bookings`, {
        params: { at: dayjs().format('YYYY-MM-DDTHH:mm:ss') },
      })
      setBookings(res.data)
      setLastRefreshed(dayjs())
    } catch {
      // не блокируем UI
    } finally {
      setRefreshing(false)
    }
  }, [clubId])

  // Начальная загрузка
  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [floorplansRes, seatsRes] = await Promise.all([
          apiClient.get(`/admin/clubs/${clubId}/floorplans`),
          apiClient.get(`/admin/clubs/${clubId}/seats`),
        ])
        if (cancelled) return
        const published = (floorplansRes.data as FloorplanSummaryResponse[]).filter(f => f.status === 'PUBLISHED')
        setFloorplans(published)
        setSeats(seatsRes.data)
        if (published.length === 0) { setLoading(false); return }

        const first = published[0]
        setSelectedFloorplanId(first.id)
        const [fpRes, bookingsRes] = await Promise.all([
          apiClient.get(`/admin/clubs/${clubId}/floorplans/${first.id}`),
          apiClient.get(`/admin/clubs/${clubId}/floorplan-bookings`, {
            params: { at: dayjs().format('YYYY-MM-DDTHH:mm:ss') },
          }),
        ])
        if (cancelled) return
        setFloorplan(fpRes.data)
        setBookings(bookingsRes.data)
        setLastRefreshed(dayjs())
      } catch {
        if (!cancelled) setError('Не удалось загрузить данные')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [clubId])

  // Авто-обновление каждые 30 с
  useEffect(() => {
    timerRef.current = setInterval(refreshBookings, 30_000)
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  }, [refreshBookings])

  async function handleHallChange(id: number) {
    setSelectedFloorplanId(id)
    setSelectedSeatId(null)
    try {
      const res = await apiClient.get(`/admin/clubs/${clubId}/floorplans/${id}`)
      setFloorplan(res.data)
      await refreshBookings()
    } catch {
      setError('Не удалось загрузить схему зала')
    }
  }

  // Статистика
  const items = useMemo(
    () => floorplan ? parseFloorplanItems(floorplan.data, floorplan.width, floorplan.height, floorplan.gridSize) : [],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [floorplan?.id, floorplan?.version],
  )
  const placedSeatIds = useMemo(
    () => new Set(items.filter((i): i is SeatItem => i.type === 'SEAT').map(i => i.seatId)),
    [items],
  )
  const activePlaced = useMemo(
    () => seats.filter(s => s.isActive && placedSeatIds.has(s.id)),
    [seats, placedSeatIds],
  )
  const totalSeats = activePlaced.length
  const occupiedCount = activePlaced.filter(s => bookingBySeatId.has(s.id)).length
  const freeCount = totalSeats - occupiedCount

  const selectedSeat = selectedSeatId ? seatById.get(selectedSeatId) : undefined
  const selectedBooking = selectedSeatId ? bookingBySeatId.get(selectedSeatId) : undefined

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />
  if (error) return <Alert type="error" message={error} />

  return (
    <div>
      {/* Топ-бар: выбор зала + сводка + обновление */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 16, flexWrap: 'wrap' }}>
        {floorplans.length > 1 && (
          <Select
            value={selectedFloorplanId}
            onChange={handleHallChange}
            options={floorplans.map(f => ({ value: f.id, label: f.name }))}
            style={{ width: 200 }}
            prefix="Зал:"
          />
        )}
        {floorplans.length === 1 && (
          <span style={{ fontSize: 14, fontWeight: 600 }}>Зал: {floorplans[0].name}</span>
        )}

        <div style={{ display: 'flex', gap: 16 }}>
          <span style={{ fontSize: 14 }}>Всего мест: <strong>{totalSeats}</strong></span>
          <span style={{ fontSize: 14, color: '#237804' }}>Свободно: <strong>{freeCount}</strong></span>
          <span style={{ fontSize: 14, color: '#cf1322' }}>Занято: <strong>{occupiedCount}</strong></span>
        </div>

        <Space size={8}>
          <Button
            size="small"
            icon={<ReloadOutlined spin={refreshing} />}
            loading={refreshing}
            onClick={refreshBookings}
          >
            Обновить
          </Button>
          {lastRefreshed && (
            <span style={{ fontSize: 12, color: '#999' }}>
              {lastRefreshed.format('HH:mm:ss')}
            </span>
          )}
        </Space>
      </div>

      {/* Основной контент: схема + панель */}
      {!floorplan ? (
        <Alert type="info" message="Нет опубликованных схем зала" description='Опубликуйте схему в разделе «Схемы зала»' />
      ) : (
        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
          {/* Схема зала */}
          <div style={{ flex: 1, minWidth: 0 }}>
            <FloorplanViewer
              floorplan={floorplan}
              seatById={seatById}
              bookingBySeatId={bookingBySeatId}
              selectedSeatId={selectedSeatId}
              onSeatClick={setSelectedSeatId}
            />
          </div>

          {/* Панель информации о месте */}
          <div style={{ width: 280, flexShrink: 0 }}>
            <Card
              style={{ minHeight: 260, borderColor: tokens.colors.border }}
              styles={{ body: { padding: 0 } }}
            >
              {!selectedSeat ? (
                <div style={{
                  textAlign: 'center',
                  color: tokens.colors.textMuted,
                  padding: '40px 16px',
                  fontSize: 13,
                  lineHeight: 1.6,
                }}>
                  <div style={{ fontSize: 28, marginBottom: 10, opacity: 0.4 }}>⊙</div>
                  Нажмите на место<br />для просмотра информации
                </div>
              ) : (
                <div style={{ padding: 14 }}>
                  <InfoPanel
                    seat={selectedSeat}
                    booking={selectedBooking}
                    onNavigate={id => navigate(`/admin/club/${clubId}/bookings/${id}`)}
                  />
                </div>
              )}
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Главная страница бронирований ───────────────────────────────────────────

export default function ClubBookingsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const [view, setView] = useState<'list' | 'floorplan'>('list')

  // --- List state ---
  const [bookings, setBookings] = useState<AdminBookingResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<BookingStatus | null>(null)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [activeQuick, setActiveQuick] = useState<string>('ALL')

  function applyQuickFilter(qf: string) {
    setActiveQuick(qf)
    switch (qf) {
      case 'ACTIVE':    setStatusFilter('ACTIVE');    setDateRange(null); break
      case 'UPCOMING':  setStatusFilter('UPCOMING');  setDateRange(null); break
      case 'CANCELED':  setStatusFilter('CANCELED');  setDateRange(null); break
      case 'TODAY':
        setStatusFilter(null)
        setDateRange([dayjs().startOf('day'), dayjs().endOf('day')])
        break
      default:          setStatusFilter(null);        setDateRange(null); break
    }
  }

  const fetchBookings = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    try {
      const params: Record<string, string> = {}
      if (dateRange) {
        params.from = dateRange[0].format('YYYY-MM-DDTHH:mm:ss')
        params.to = dateRange[1].format('YYYY-MM-DDTHH:mm:ss')
      }
      if (statusFilter) params.status = statusFilter
      const res = await apiClient.get<AdminBookingResponse[]>(`/admin/clubs/${clubId}/bookings`, { params })
      setBookings(res.data)
    } catch {
      message.error('Не удалось загрузить бронирования')
    } finally {
      setLoading(false)
    }
  }, [clubId, dateRange, statusFilter])

  useEffect(() => {
    if (view === 'list') fetchBookings()
  }, [fetchBookings, view])

  const filtered = useMemo(() => {
    if (!searchText.trim()) return bookings
    const q = searchText.trim().toLowerCase()
    return bookings.filter(b => b.id.toString().includes(q) || (b.userPhone ?? '').toLowerCase().includes(q))
  }, [bookings, searchText])

  const stats = useMemo(() => ({
    total: filtered.length,
    upcoming: filtered.filter(b => b.status === 'UPCOMING').length,
    active: filtered.filter(b => b.status === 'ACTIVE').length,
    done: filtered.filter(b => b.status === 'DONE').length,
    canceled: filtered.filter(b => b.status === 'CANCELED').length,
  }), [filtered])

  const columns: ColumnsType<AdminBookingResponse> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 70, sorter: (a, b) => Number(a.id) - Number(b.id) },
    { title: 'Пользователь', key: 'user', render: (_, r) => r.userPhone ?? `#${r.userId}`, width: 150 },
    { title: 'Места', key: 'seats', render: (_, r) => r.seatLabels.length <= 3 ? r.seatLabels.join(', ') || '—' : `${r.seatLabels.slice(0, 3).join(', ')} +${r.seatLabels.length - 3}` },
    { title: 'Начало', key: 'startAt', render: (_, r) => new Date(r.startAt).toLocaleString('ru-RU'), width: 160 },
    { title: 'Конец', key: 'endAt', render: (_, r) => new Date(r.endAt).toLocaleString('ru-RU'), width: 160 },
    { title: 'Длит.', key: 'dur', render: (_, r) => formatDuration(r.durationHours), width: 100 },
    { title: 'Сумма', key: 'total', render: (_, r) => `${r.totalRub} ₽`, width: 90 },
    {
      title: 'Статус', key: 'status', width: 140,
      render: (_, r) => {
        const { label, variant } = { UPCOMING: { label: 'Предстоящее', variant: 'info' as const }, ACTIVE: { label: 'Активное', variant: 'success' as const }, DONE: { label: 'Завершено', variant: 'default' as const }, CANCELED: { label: 'Отменено', variant: 'error' as const } }[r.status] ?? { label: r.status, variant: 'default' as const }
        return <StatusBadge label={label} variant={variant} />
      },
    },
    {
      title: 'Покупка', key: 'purchase', width: 90,
      render: (_, r) => r.purchaseId
        ? <Button type="link" size="small" style={{ padding: 0 }} onClick={() => navigate(`/admin/club/${clubId}/purchases/${r.purchaseId}`)}>#{r.purchaseId}</Button>
        : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: '', key: 'actions', width: 90,
      render: (_, r) => <Button size="small" onClick={() => navigate(`/admin/club/${clubId}/bookings/${r.id}`)}>Открыть</Button>,
    },
  ]

  return (
    <div>
      <PageHeader
        title="Бронирования"
        extra={
          <Segmented
            value={view}
            onChange={v => setView(v as 'list' | 'floorplan')}
            options={[{ value: 'list', label: 'Список' }, { value: 'floorplan', label: 'Схема зала' }]}
          />
        }
      />

      {view === 'list' ? (
        <>
          {/* Мини-статистика */}
          <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
            <Col xs={12} sm={8} md={4}>
              <StatCard label="Всего" value={stats.total} />
            </Col>
            <Col xs={12} sm={8} md={5}>
              <StatCard label="Предстоящие" value={stats.upcoming} icon={<ClockCircleOutlined />} accentColor={tokens.colors.info} />
            </Col>
            <Col xs={12} sm={8} md={5}>
              <StatCard label="Активные" value={stats.active} icon={<CalendarOutlined />} accentColor={tokens.colors.success} />
            </Col>
            <Col xs={12} sm={8} md={5}>
              <StatCard label="Завершённые" value={stats.done} icon={<CheckCircleOutlined />} accentColor={tokens.colors.textSecondary} />
            </Col>
            <Col xs={12} sm={8} md={5}>
              <StatCard label="Отменённые" value={stats.canceled} icon={<CloseCircleOutlined />} accentColor={tokens.colors.error} />
            </Col>
          </Row>

          {/* Sticky панель фильтров */}
          <div style={{ position: 'sticky', top: 0, zIndex: 9, background: tokens.colors.background, paddingBottom: 12 }}>
            <SectionCard>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                {/* Быстрые фильтры */}
                <Space size={4}>
                  {([
                    { value: 'ALL',      label: 'Все' },
                    { value: 'ACTIVE',   label: 'Активные' },
                    { value: 'UPCOMING', label: 'Предстоящие' },
                    { value: 'TODAY',    label: 'Сегодня' },
                    { value: 'CANCELED', label: 'Отменённые' },
                  ] as const).map(qf => (
                    <Button
                      key={qf.value}
                      size="small"
                      type={activeQuick === qf.value ? 'primary' : 'default'}
                      onClick={() => applyQuickFilter(qf.value)}
                    >
                      {qf.label}
                    </Button>
                  ))}
                </Space>

                {/* Разделитель */}
                <div style={{ width: 1, height: 22, background: tokens.colors.border, flexShrink: 0 }} />

                {/* Поиск + дата + статус */}
                <Input.Search
                  placeholder="ID, телефон"
                  allowClear
                  size="small"
                  style={{ width: 180 }}
                  value={searchText}
                  onChange={e => setSearchText(e.target.value)}
                />
                <DatePicker.RangePicker
                  size="small"
                  showTime
                  value={dateRange}
                  onChange={val => {
                    setDateRange(val as [Dayjs, Dayjs] | null)
                    setActiveQuick('ALL')
                  }}
                />
                <Select
                  size="small"
                  style={{ width: 160 }}
                  placeholder="Все статусы"
                  allowClear
                  value={statusFilter}
                  onChange={v => {
                    setStatusFilter(v ?? null)
                    setActiveQuick('ALL')
                  }}
                  options={[
                    { value: 'UPCOMING', label: 'Предстоящие' },
                    { value: 'ACTIVE',   label: 'Активные' },
                    { value: 'DONE',     label: 'Завершённые' },
                    { value: 'CANCELED', label: 'Отменённые' },
                  ]}
                />
              </div>
            </SectionCard>
          </div>

          <SectionCard noPadding>
            <Table
              columns={columns}
              dataSource={filtered}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 20, showSizeChanger: false }}
              rowClassName={(r: AdminBookingResponse) =>
                r.status === 'ACTIVE'   ? 'booking-row-active' :
                r.status === 'CANCELED' ? 'booking-row-canceled' : ''
              }
            />
          </SectionCard>
        </>
      ) : (
        clubId ? <FloorplanTab clubId={clubId} /> : null
      )}
    </div>
  )
}
