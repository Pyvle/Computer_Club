import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Input,
  message,
  Select,
  Segmented,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
} from 'antd'
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

const STATUS_LABELS: Record<BookingStatus, string> = {
  UPCOMING: 'Предстоящее',
  ACTIVE: 'Активное',
  DONE: 'Завершено',
  CANCELED: 'Отменено',
}

const STATUS_COLORS: Record<BookingStatus, string> = {
  UPCOMING: 'blue',
  ACTIVE: 'success',
  DONE: 'default',
  CANCELED: 'error',
}

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

// цвета состояний мест
const SEAT_COLORS: Record<SeatState, { bg: string; border: string; text: string }> = {
  FREE:        { bg: '#f6ffed', border: '#52c41a', text: '#237804' },
  OCCUPIED:    { bg: '#fff1f0', border: '#ff4d4f', text: '#cf1322' },
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
  const defaultZoom = Math.max(1, Math.ceil(MIN_CELL_PX / gridSize))
  const [zoom, setZoom] = useState(defaultZoom)
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

  return (
    <div>
      <div style={{ marginBottom: 6, display: 'flex', gap: 6, alignItems: 'center' }}>
        <Button size="small" onClick={() => setZoom(z => Math.max(1, z - 1))}>−</Button>
        <span style={{ fontSize: 12, color: '#666', minWidth: 22, textAlign: 'center' }}>{zoom}×</span>
        <Button size="small" onClick={() => setZoom(z => Math.min(8, z + 1))}>+</Button>
        <Button size="small" onClick={() => setZoom(defaultZoom)}>сброс</Button>
      </div>

      <div style={{ overflowX: 'auto', overflowY: 'auto', maxHeight: 'calc(100vh - 360px)', border: '1px solid #d9d9d9', borderRadius: 6, background: VOID_BG }}>
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
              const borderColor = isSelected ? '#1677ff' : colors ? colors.border : '#d9d9d9'
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
                    boxShadow: isSelected ? '0 0 0 2px rgba(22,119,255,0.2)' : seatId ? '0 1px 2px rgba(0,0,0,0.1)' : 'none',
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

      {/* Легенда */}
      <div style={{ display: 'flex', gap: 14, marginTop: 10 }}>
        {(Object.entries(SEAT_COLORS) as [SeatState, typeof SEAT_COLORS[SeatState]][]).map(([state, c]) => (
          <span key={state} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12, color: '#666' }}>
            <span style={{ display: 'inline-block', width: 12, height: 12, background: c.bg, border: `1px solid ${c.border}`, borderRadius: 2 }} />
            {{ FREE: 'Свободно', OCCUPIED: 'Занято', UNAVAILABLE: 'Недоступно' }[state]}
          </span>
        ))}
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
  clubId: string
  onNavigate: (bookingId: number) => void
}

function InfoPanel({ seat, booking, clubId, onNavigate }: InfoPanelProps) {
  const state: SeatState = !seat.isActive ? 'UNAVAILABLE' : booking ? 'OCCUPIED' : 'FREE'
  const payment = booking?.paymentStatus ? PAYMENT_LABELS[booking.paymentStatus] : null

  return (
    <div>
      <div style={{ marginBottom: 12 }}>
        <div style={{ fontSize: 16, fontWeight: 700 }}>{seat.label}</div>
        <div style={{ fontSize: 12, color: '#888' }}>Тип: {seat.type === 'VIP' ? 'VIP' : 'Обычное'}</div>
      </div>

      {state === 'UNAVAILABLE' && (
        <Tag color="default" style={{ fontSize: 13 }}>Недоступно</Tag>
      )}

      {state === 'FREE' && (
        <Tag color="success" style={{ fontSize: 13 }}>✓ Свободно</Tag>
      )}

      {state === 'OCCUPIED' && booking && (
        <div>
          <Tag color="error" style={{ fontSize: 13, marginBottom: 12 }}>Занято</Tag>

          <div style={{ fontSize: 13, marginBottom: 6 }}>
            <span style={{ color: '#888' }}>Бронь</span>{' '}
            <strong>#{booking.bookingId}</strong>
          </div>

          {booking.userPhone && (
            <div style={{ fontSize: 13, marginBottom: 6 }}>
              <span style={{ color: '#888' }}>Пользователь</span><br />
              <strong>{booking.userPhone}</strong>
            </div>
          )}

          <div style={{ fontSize: 13, marginBottom: 6 }}>
            <span style={{ color: '#888' }}>Время</span><br />
            <strong>
              {dayjs(booking.startAt).format('HH:mm')} — {dayjs(booking.endAt).format('HH:mm')}
            </strong>
            <span style={{ color: '#888', fontSize: 12 }}>
              {' '}({dayjs(booking.startAt).format('DD.MM')})
            </span>
          </div>

          {payment && (
            <div style={{ fontSize: 13, marginBottom: 12 }}>
              <span style={{ color: '#888' }}>Оплата</span>{' '}
              <Tag color={payment.color}>{payment.label}</Tag>
            </div>
          )}

          <Button
            type="primary"
            block
            onClick={() => onNavigate(booking.bookingId)}
          >
            Открыть бронь
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
          <div style={{ width: 260, flexShrink: 0 }}>
            <Card size="small" style={{ minHeight: 220 }}>
              {!selectedSeat ? (
                <div style={{ textAlign: 'center', color: '#bbb', padding: '32px 0', fontSize: 13 }}>
                  Нажмите на место<br />для просмотра информации
                </div>
              ) : (
                <InfoPanel
                  seat={selectedSeat}
                  booking={selectedBooking}
                  clubId={clubId}
                  onNavigate={id => navigate(`/admin/club/${clubId}/bookings/${id}`)}
                />
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
    { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
    { title: 'Пользователь', key: 'user', render: (_, r) => r.userPhone ?? `#${r.userId}`, width: 150 },
    { title: 'Места', key: 'seats', render: (_, r) => r.seatLabels.length <= 3 ? r.seatLabels.join(', ') || '—' : `${r.seatLabels.slice(0, 3).join(', ')} +${r.seatLabels.length - 3}` },
    { title: 'Начало', key: 'startAt', render: (_, r) => new Date(r.startAt).toLocaleString('ru-RU'), width: 160 },
    { title: 'Конец', key: 'endAt', render: (_, r) => new Date(r.endAt).toLocaleString('ru-RU'), width: 160 },
    { title: 'Длит.', key: 'dur', render: (_, r) => formatDuration(r.durationHours), width: 100 },
    { title: 'Сумма', key: 'total', render: (_, r) => `${r.totalRub} ₽`, width: 90 },
    { title: 'Статус', key: 'status', render: (_, r) => <Tag color={STATUS_COLORS[r.status]}>{STATUS_LABELS[r.status]}</Tag>, width: 140 },
    {
      title: 'Покупка', key: 'purchase', width: 90,
      render: (_, r) => r.purchaseId
        ? <Button type="link" size="small" style={{ padding: 0 }} onClick={() => navigate(`/admin/club/${clubId}/purchases/${r.purchaseId}`)}>#{r.purchaseId}</Button>
        : <span style={{ color: '#999' }}>—</span>,
    },
    {
      title: '', key: 'actions', width: 90,
      render: (_, r) => <Button size="small" onClick={() => navigate(`/admin/club/${clubId}/bookings/${r.id}`)}>Открыть</Button>,
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Бронирования</h2>
        <Segmented
          value={view}
          onChange={v => setView(v as 'list' | 'floorplan')}
          options={[{ value: 'list', label: 'Список' }, { value: 'floorplan', label: 'Схема зала' }]}
        />
      </div>

      {view === 'list' ? (
        <>
          <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
            {([['Всего', stats.total, undefined], ['Предстоящие', stats.upcoming, '#1677ff'], ['Активные', stats.active, '#52c41a'], ['Завершённые', stats.done, undefined], ['Отменённые', stats.canceled, undefined]] as const).map(([title, value, color]) => (
              <Card key={title} size="small" style={{ flex: 1, minWidth: 110 }}>
                <Statistic title={title} value={value} valueStyle={color ? { color } : undefined} />
              </Card>
            ))}
          </div>

          <Space wrap style={{ marginBottom: 16 }}>
            <Input.Search placeholder="ID, телефон" allowClear style={{ width: 200 }} value={searchText} onChange={e => setSearchText(e.target.value)} />
            <DatePicker.RangePicker showTime onChange={val => setDateRange(val as [Dayjs, Dayjs] | null)} />
            <Select style={{ width: 180 }} placeholder="Все статусы" allowClear value={statusFilter} onChange={v => setStatusFilter(v ?? null)}
              options={[{ value: 'UPCOMING', label: 'Предстоящие' }, { value: 'ACTIVE', label: 'Активные' }, { value: 'DONE', label: 'Завершённые' }, { value: 'CANCELED', label: 'Отменённые' }]} />
          </Space>

          <Table columns={columns} dataSource={filtered} rowKey="id" loading={loading} pagination={{ pageSize: 20, showSizeChanger: false }} />
        </>
      ) : (
        clubId ? <FloorplanTab clubId={clubId} /> : null
      )}
    </div>
  )
}
