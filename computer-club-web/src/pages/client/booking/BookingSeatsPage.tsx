import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Spin, App } from 'antd'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  ShoppingCartOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../../utils/apiClient'
import { useClient } from '../../../contexts/ClientContext'
import ClientFloorplanViewer from '../../../components/ClientFloorplanViewer'
import SectionCard from '../../../components/ui/SectionCard'
import StepBar from '../../../components/ui/StepBar'
import { tokens } from '../../../theme/tokens'
import type {
  FloorplanWithAvailabilityClientResponse,
  SeatClientResponse,
  SeatPriceClientResponse,
} from '../../../types'

const BOOKING_STEPS = ['Время', 'Места', 'Корзина']

// --- Панель выбранных мест ---

function SeatsPanel({
  selectedSeatIds,
  seatById,
  seatPrices,
  durationHours,
  submitting,
  onAddToCart,
  onBack,
}: {
  selectedSeatIds: number[]
  seatById: Map<number, SeatClientResponse>
  seatPrices: SeatPriceClientResponse[]
  durationHours: number
  submitting: boolean
  onAddToCart: () => void
  onBack: () => void
}) {
  function getPricePerHour(seatId: number): number {
    const seat = seatById.get(seatId)
    return seatPrices.find((p) => p.seatType === seat?.type)?.pricePerHourRub ?? 0
  }

  function calcSeatPrice(seatId: number) {
    return Math.round(getPricePerHour(seatId) * durationHours)
  }

  const totalPrice = selectedSeatIds.reduce((sum, id) => sum + calcSeatPrice(id), 0)

  return (
    <div style={{
      background: tokens.colors.surface,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.lg,
      boxShadow: tokens.shadow.card,
      overflow: 'hidden',
      position: 'sticky',
      top: 80,
    }}>
      {/* Заголовок */}
      <div style={{
        padding: '14px 20px',
        borderBottom: `1px solid ${tokens.colors.border}`,
        background: tokens.colors.surfaceAlt,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        <span style={{ fontWeight: 700, fontSize: 14, color: tokens.colors.text }}>
          Выбранные места
        </span>
        {selectedSeatIds.length > 0 && (
          <span style={{
            background: tokens.colors.primary,
            color: '#fff',
            fontSize: 12,
            fontWeight: 700,
            padding: '2px 8px',
            borderRadius: 12,
          }}>
            {selectedSeatIds.length}
          </span>
        )}
      </div>

      {/* Список мест */}
      <div style={{ padding: '12px 20px' }}>
        {selectedSeatIds.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <div style={{
              width: 48, height: 48, borderRadius: '50%',
              background: tokens.colors.primarySoft,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 10px',
            }}>
              <ShoppingCartOutlined style={{ fontSize: 20, color: tokens.colors.primary, opacity: 0.6 }} />
            </div>
            <div style={{ fontSize: 13, color: tokens.colors.textSecondary, lineHeight: 1.5 }}>
              Нажмите на свободное место<br />на схеме зала
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {selectedSeatIds.map((id) => {
              const seat = seatById.get(id)
              const price = calcSeatPrice(id)
              const isVip = seat?.type === 'VIP'
              return (
                <div key={id} style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '7px 10px',
                  background: isVip ? '#FFFBEB' : tokens.colors.surfaceAlt,
                  border: `1px solid ${isVip ? '#FDE68A' : tokens.colors.border}`,
                  borderRadius: tokens.radius.sm,
                  fontSize: 13,
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ fontWeight: 600, color: tokens.colors.text }}>
                      {seat?.label ?? `#${id}`}
                    </span>
                    {isVip && (
                      <span style={{
                        fontSize: 10, fontWeight: 700,
                        color: tokens.colors.warning,
                        background: '#FEF3C7',
                        padding: '1px 6px',
                        borderRadius: 10,
                      }}>
                        VIP
                      </span>
                    )}
                  </div>
                  <span style={{ fontWeight: 700, color: tokens.colors.text }}>
                    {price} ₽
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Итого */}
      {selectedSeatIds.length > 0 && (
        <div style={{
          padding: '10px 20px',
          borderTop: `1px solid ${tokens.colors.border}`,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>
          <span style={{ fontSize: 13, color: tokens.colors.textSecondary }}>
            Итого за {durationHours % 1 === 0 ? durationHours : durationHours.toFixed(1)} ч
          </span>
          <span style={{ fontSize: 18, fontWeight: 800, color: tokens.colors.primary }}>
            {totalPrice.toLocaleString('ru-RU')} ₽
          </span>
        </div>
      )}

      {/* Кнопки */}
      <div style={{ padding: '12px 20px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Button
          type="primary"
          size="large"
          block
          icon={<ShoppingCartOutlined />}
          loading={submitting}
          disabled={selectedSeatIds.length === 0}
          onClick={onAddToCart}
          style={{ fontWeight: 600 }}
        >
          Добавить в корзину
        </Button>
        <Button
          block
          icon={<ArrowLeftOutlined />}
          onClick={onBack}
          style={{ color: tokens.colors.textSecondary }}
        >
          Изменить время
        </Button>
      </div>
    </div>
  )
}

// --- Основной компонент ---

export default function BookingSeatsPage() {
  const { clubId: clubIdParam } = useParams<{ clubId: string }>()
  const clubId = Number(clubIdParam)
  const navigate = useNavigate()
  const { bookingDraft, setBookingDraft, refreshCartCount } = useClient()
  const { message } = App.useApp()

  // все хуки до любых ранних возвратов
  const [availability, setAvailability] = useState<FloorplanWithAvailabilityClientResponse | null>(null)
  const [seats, setSeats] = useState<SeatClientResponse[]>([])
  const [seatPrices, setSeatPrices] = useState<SeatPriceClientResponse[]>([])
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const hasDraft = bookingDraft !== null && bookingDraft.clubId === clubId

  useEffect(() => {
    // инициализируем выбор из черновика
    if (hasDraft) setSelectedSeatIds(bookingDraft!.selectedSeatIds)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!hasDraft) return
    const { date, startTime, endTime } = bookingDraft!
    const fromISO = `${date}T${startTime}:00`
    const toISO = `${date}T${endTime}:00`

    async function load() {
      setLoading(true)
      try {
        const [availRes, seatsRes, pricesRes] = await Promise.all([
          apiClient.get<FloorplanWithAvailabilityClientResponse>(
            `/clubs/${clubId}/floorplan-with-availability?from=${fromISO}&to=${toISO}`
          ),
          apiClient.get<SeatClientResponse[]>(`/clubs/${clubId}/seats`),
          apiClient.get<SeatPriceClientResponse[]>(`/clubs/${clubId}/seat-prices`),
        ])
        setAvailability(availRes.data)
        setSeats(seatsRes.data)
        setSeatPrices(pricesRes.data)
      } catch {
        message.error('Не удалось загрузить схему зала')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [clubId]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!hasDraft) {
    navigate(`/clubs/${clubId}/booking`, { replace: true })
    return null
  }

  const { clubName, date, startTime, endTime, packageHours } = bookingDraft
  const fromISO = `${date}T${startTime}:00`
  const toISO = `${date}T${endTime}:00`
  const durationHours = dayjs(toISO).diff(dayjs(fromISO), 'minute') / 60
  const seatById = new Map(seats.map((s) => [s.id, s]))

  function toggleSeat(seatId: number) {
    setSelectedSeatIds((prev) =>
      prev.includes(seatId) ? prev.filter((id) => id !== seatId) : [...prev, seatId]
    )
  }

  async function handleAddToCart() {
    if (selectedSeatIds.length === 0) {
      message.warning('Выберите хотя бы одно место')
      return
    }
    setSubmitting(true)
    let lineId: number | null = null
    try {
      // 1. добавляем бронирование в корзину
      const bookingRes = await apiClient.post<{ bookings: { lineId: number }[] }>(
        `/cart/bookings?clubId=${clubId}`,
        { startAt: fromISO, endAt: toISO, packageHours: packageHours ?? null }
      )
      const lines = bookingRes.data.bookings
      lineId = lines[lines.length - 1]?.lineId ?? null
      if (!lineId) throw new Error('lineId not found')

      // 2. привязываем места
      await apiClient.post(`/cart/bookings/${lineId}/seats?clubId=${clubId}`, {
        seatIds: selectedSeatIds,
      })

      await refreshCartCount(clubId)
      setBookingDraft(null)
      navigate(`/clubs/${clubId}/cart`)
    } catch {
      // откат: удаляем строку если она была создана
      if (lineId) {
        apiClient.delete(`/cart/items/booking/${lineId}?clubId=${clubId}`).catch(() => {})
      }
      message.error('Не удалось добавить в корзину')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: 1100 }}>
      <StepBar steps={BOOKING_STEPS} current={1} />

      {/* Контекстная полоса с параметрами сеанса */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        padding: '10px 16px',
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        marginBottom: 20,
        flexWrap: 'wrap',
        fontSize: 13,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: tokens.colors.text, fontWeight: 600 }}>
          <EnvironmentOutlined style={{ color: tokens.colors.primary }} />
          {clubName}
        </div>
        <div style={{ width: 1, height: 16, background: tokens.colors.border }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: tokens.colors.textSecondary }}>
          <CalendarOutlined />
          {dayjs(date).format('D MMMM YYYY')}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, color: tokens.colors.textSecondary }}>
          <ClockCircleOutlined />
          {startTime} — {endTime}
          {packageHours ? ` · пакет ${packageHours} ч` : ''}
        </div>
        <button
          onClick={() => navigate(`/clubs/${clubId}/booking`)}
          style={{
            marginLeft: 'auto',
            background: 'none',
            border: 'none',
            color: tokens.colors.primary,
            fontSize: 13,
            cursor: 'pointer',
            fontWeight: 500,
            padding: 0,
          }}
        >
          Изменить
        </button>
      </div>

      {loading ? (
        <Spin style={{ display: 'block', margin: '64px auto' }} />
      ) : !availability?.floorplan ? (
        <Alert
          type="warning"
          message="Схема зала ещё не опубликована"
          description="Администратор клуба ещё не добавил схему зала. Попробуйте позже или обратитесь в клуб."
          showIcon
        />
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 280px',
          gap: 24,
          alignItems: 'start',
        }}
          className="seats-layout"
        >
          {/* Схема зала */}
          <SectionCard title="Схема зала" noPadding>
            <div style={{ padding: '16px' }}>
              <ClientFloorplanViewer
                floorplan={availability.floorplan}
                seats={seats}
                busySeatIds={availability.busySeatIds}
                selectedSeatIds={selectedSeatIds}
                onToggleSeat={toggleSeat}
              />
            </div>
          </SectionCard>

          {/* Панель выбора */}
          <SeatsPanel
            selectedSeatIds={selectedSeatIds}
            seatById={seatById}
            seatPrices={seatPrices}
            durationHours={durationHours}
            submitting={submitting}
            onAddToCart={handleAddToCart}
            onBack={() => navigate(`/clubs/${clubId}/booking`)}
          />
        </div>
      )}

      <style>{`
        @media (max-width: 720px) {
          .seats-layout {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  )
}

