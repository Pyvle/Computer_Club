import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Modal, Spin, App } from 'antd'
import {
  DeleteOutlined,
  ShoppingOutlined,
  CalendarOutlined,
  TagOutlined,
  CheckCircleOutlined,
  HeartOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../../utils/apiClient'
import { calculateCartBookingTotal, calculateCartProductTotal } from '../../../utils/clientBooking'
import { useClient } from '../../../contexts/ClientContext'
import PageHeader from '../../../components/ui/PageHeader'
import SectionCard from '../../../components/ui/SectionCard'
import EmptyState from '../../../components/ui/EmptyState'
import { tokens } from '../../../theme/tokens'
import type {
  CartClientResponse,
  CartBookingLineClientResponse,
  CartProductLineClientResponse,
  SeatClientResponse,
  SeatPriceClientResponse,
  CheckoutClientResponse,
  TimePackageClientResponse,
} from '../../../types'

// --- Карточка одного бронирования ---

function BookingCard({
  booking,
  seats,
  seatPrices,
  timePackages,
  onDelete,
}: {
  booking: CartBookingLineClientResponse
  seats: Map<number, SeatClientResponse>
  seatPrices: SeatPriceClientResponse[]
  timePackages: TimePackageClientResponse[]
  onDelete: () => void
}) {
  const start = dayjs(booking.startAt)
  const end = dayjs(booking.endAt)
  const durationHours = end.diff(start, 'minute') / 60
  const durationLabel = durationHours % 1 === 0
    ? `${durationHours} ч`
    : `${durationHours.toFixed(1)} ч`

  // вычисляем стоимость из цен на типы мест
  const estimatedPrice = calculateCartBookingTotal(booking, seats, seatPrices, timePackages)

  return (
    <div style={{
      background: tokens.colors.surface,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.lg,
      padding: '16px 18px',
      marginBottom: 10,
    }}>
      {/* Заголовок: дата + удалить */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
        <div>
          <div style={{ fontSize: 15, fontWeight: 700, color: tokens.colors.text }}>
            {start.format('D MMMM, dddd')}
          </div>
          <div style={{ fontSize: 13, color: tokens.colors.textSecondary, marginTop: 2 }}>
            <CalendarOutlined style={{ marginRight: 5 }} />
            {start.format('HH:mm')} — {end.format('HH:mm')}
            <span style={{
              marginLeft: 8, fontSize: 11,
              background: tokens.colors.primarySoft,
              color: tokens.colors.primary,
              padding: '1px 8px',
              borderRadius: 10,
              fontWeight: 600,
            }}>
              {durationLabel}
            </span>
            {booking.packageHours && (
              <span style={{
                marginLeft: 6, fontSize: 11,
                background: tokens.colors.surfaceAlt,
                color: tokens.colors.textSecondary,
                padding: '1px 8px',
                borderRadius: 10,
              }}>
                пакет {booking.packageHours} ч
              </span>
            )}
          </div>
        </div>
        <button
          onClick={onDelete}
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            color: tokens.colors.error, padding: 4, borderRadius: 6,
            display: 'flex', alignItems: 'center',
            opacity: 0.7,
          }}
          title="Удалить"
        >
          <DeleteOutlined style={{ fontSize: 16 }} />
        </button>
      </div>

      {/* Места */}
      {booking.seatIds.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
          {booking.seatIds.map((id) => {
            const seat = seats.get(id)
            const isVip = seat?.type === 'VIP'
            return (
              <span key={id} style={{
                padding: '3px 10px',
                borderRadius: 16,
                fontSize: 12, fontWeight: 600,
                background: isVip ? '#FEF3C7' : tokens.colors.surfaceAlt,
                border: `1px solid ${isVip ? '#FDE68A' : tokens.colors.border}`,
                color: isVip ? tokens.colors.warning : tokens.colors.text,
              }}>
                {seat?.label ?? `#${id}`}
                {isVip && <span style={{ marginLeft: 4, opacity: 0.8 }}>VIP</span>}
              </span>
            )
          })}
        </div>
      )}

      {/* Стоимость брони */}
      {booking.seatIds.length > 0 && (
        <div style={{
          display: 'flex',
          justifyContent: 'flex-end',
          paddingTop: 8,
          borderTop: `1px solid ${tokens.colors.border}`,
          fontSize: 14,
          fontWeight: 700,
          color: tokens.colors.text,
        }}>
          {estimatedPrice > 0
            ? `${estimatedPrice.toLocaleString('ru-RU')} ₽`
            : <span style={{ fontSize: 12, fontWeight: 500, color: tokens.colors.textMuted }}>цена уточняется</span>
          }
        </div>
      )}
    </div>
  )
}

// --- Строка товара ---

function ProductRow({
  product,
  onQtyChange,
  onDelete,
}: {
  product: CartProductLineClientResponse
  onQtyChange: (qty: number) => void
  onDelete: () => void
}) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '12px 18px',
      background: tokens.colors.surface,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.md,
      marginBottom: 8,
      gap: 12,
    }}>
      {/* Название и цена за штуку */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: tokens.colors.text, marginBottom: 2 }}>
          {product.title}
        </div>
        <div style={{ fontSize: 12, color: tokens.colors.textSecondary }}>
          {product.priceRub} ₽/шт.
        </div>
      </div>

      {/* Счётчик количества */}
      <div style={{
        display: 'flex', alignItems: 'center',
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.sm,
        overflow: 'hidden',
      }}>
        <button
          onClick={() => onQtyChange(product.qty - 1)}
          style={{
            width: 32, height: 32, border: 'none',
            background: tokens.colors.surfaceAlt,
            color: tokens.colors.text,
            cursor: 'pointer', fontSize: 16,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          −
        </button>
        <span style={{
          padding: '0 12px',
          fontSize: 14, fontWeight: 700,
          color: tokens.colors.text,
          minWidth: 32, textAlign: 'center',
          borderLeft: `1px solid ${tokens.colors.border}`,
          borderRight: `1px solid ${tokens.colors.border}`,
          lineHeight: '32px',
        }}>
          {product.qty}
        </span>
        <button
          onClick={() => onQtyChange(product.qty + 1)}
          style={{
            width: 32, height: 32, border: 'none',
            background: tokens.colors.surfaceAlt,
            color: tokens.colors.text,
            cursor: 'pointer', fontSize: 16,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          +
        </button>
      </div>

      {/* Итог строки */}
      <div style={{ fontSize: 15, fontWeight: 700, color: tokens.colors.text, minWidth: 72, textAlign: 'right' }}>
        {product.lineTotalRub.toLocaleString('ru-RU')} ₽
      </div>

      {/* Удалить */}
      <button
        onClick={onDelete}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          color: tokens.colors.error, padding: 4, opacity: 0.7,
          display: 'flex', alignItems: 'center',
        }}
        title="Удалить"
      >
        <DeleteOutlined style={{ fontSize: 15 }} />
      </button>
    </div>
  )
}

// --- Сводная панель заказа ---

function OrderSummary({
  bookingTotal,
  productTotal,
  hasBookings,
  checkingOut,
  onCheckout,
}: {
  bookingTotal: number
  productTotal: number
  hasBookings: boolean
  checkingOut: boolean
  onCheckout: () => void
}) {
  const grandTotal = bookingTotal + productTotal
  // цена не определена когда есть бронирование, но тарифы не настроены
  const priceUnknown = hasBookings && bookingTotal === 0
  const hasBoth = (bookingTotal > 0 || priceUnknown) && productTotal > 0

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
      <div style={{
        padding: '14px 20px',
        borderBottom: `1px solid ${tokens.colors.border}`,
        background: tokens.colors.surfaceAlt,
        fontWeight: 700,
        fontSize: 14,
        color: tokens.colors.text,
      }}>
        Состав заказа
      </div>

      <div style={{ padding: '16px 20px' }}>
        {/* Строки итогов */}
        {hasBoth && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 14 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
              <span style={{ color: tokens.colors.textSecondary }}>
                <CalendarOutlined style={{ marginRight: 5 }} />Бронирования
              </span>
              {priceUnknown
                ? <span style={{ fontSize: 12, color: tokens.colors.textMuted }}>уточняется</span>
                : <span style={{ fontWeight: 600, color: tokens.colors.text }}>{bookingTotal.toLocaleString('ru-RU')} ₽</span>
              }
            </div>
            {productTotal > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                <span style={{ color: tokens.colors.textSecondary }}>
                  <TagOutlined style={{ marginRight: 5 }} />Товары
                </span>
                <span style={{ fontWeight: 600, color: tokens.colors.text }}>
                  {productTotal.toLocaleString('ru-RU')} ₽
                </span>
              </div>
            )}
            <div style={{ height: 1, background: tokens.colors.border }} />
          </div>
        )}

        {/* Итого */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <span style={{ fontWeight: 700, fontSize: 15, color: tokens.colors.text }}>Итого</span>
          {priceUnknown
            ? (
              <div style={{ textAlign: 'right' }}>
                {productTotal > 0 && (
                  <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginBottom: 2 }}>
                    + {productTotal.toLocaleString('ru-RU')} ₽ товары
                  </div>
                )}
                <span style={{ fontSize: 16, fontWeight: 700, color: tokens.colors.textMuted }}>
                  уточняется
                </span>
              </div>
            )
            : (
              <span style={{ fontSize: 24, fontWeight: 800, color: tokens.colors.primary }}>
                {grandTotal.toLocaleString('ru-RU')} ₽
              </span>
            )
          }
        </div>

        {/* Кнопка оформления */}
        <Button
          type="primary"
          size="large"
          block
          loading={checkingOut}
          onClick={onCheckout}
          style={{ fontWeight: 600, height: 44 }}
        >
          Оформить заказ
        </Button>
      </div>
    </div>
  )
}

// --- Основной компонент ---

export default function CartPage() {
  const { clubId: clubIdParam } = useParams<{ clubId: string }>()
  const clubId = Number(clubIdParam)
  const navigate = useNavigate()
  const { refreshCartCount } = useClient()
  const { message } = App.useApp()

  const [cart, setCart] = useState<CartClientResponse | null>(null)
  const [seats, setSeats] = useState<Map<number, SeatClientResponse>>(new Map())
  const [seatPrices, setSeatPrices] = useState<SeatPriceClientResponse[]>([])
  const [timePackages, setTimePackages] = useState<TimePackageClientResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [checkingOut, setCheckingOut] = useState(false)

  const loadCart = useCallback(async () => {
    try {
      const [cartRes, seatsRes, pricesRes, packagesRes] = await Promise.all([
        apiClient.get<CartClientResponse>(`/cart?clubId=${clubId}`),
        apiClient.get<SeatClientResponse[]>(`/clubs/${clubId}/seats`),
        apiClient.get<SeatPriceClientResponse[]>(`/clubs/${clubId}/seat-prices`).catch(() => ({ data: [] as SeatPriceClientResponse[] })),
        apiClient.get<TimePackageClientResponse[]>(`/clubs/${clubId}/time-packages`).catch(() => ({ data: [] as TimePackageClientResponse[] })),
      ])
      setCart(cartRes.data)
      setSeats(new Map(seatsRes.data.map((s) => [s.id, s])))
      setSeatPrices(pricesRes.data)
      setTimePackages(packagesRes.data)
    } catch {
      message.error('Не удалось загрузить корзину')
    } finally {
      setLoading(false)
    }
  }, [clubId]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadCart() }, [loadCart])

  async function deleteItem(type: 'booking' | 'product', id: number) {
    try {
      await apiClient.delete(`/cart/items/${type}/${id}?clubId=${clubId}`)
      await Promise.all([loadCart(), refreshCartCount(clubId)])
    } catch {
      message.error('Не удалось удалить')
    }
  }

  async function updateQty(lineId: number, qty: number) {
    if (qty <= 0) {
      await deleteItem('product', lineId)
      return
    }
    try {
      await apiClient.patch(`/cart/products/${lineId}?clubId=${clubId}`, { qty })
      await loadCart()
    } catch {
      message.error('Не удалось обновить количество')
    }
  }

  async function handleCheckout() {
    setCheckingOut(true)
    try {
      const { data } = await apiClient.post<CheckoutClientResponse>('/checkout', { clubId })
      await refreshCartCount(clubId)
      Modal.success({
        title: 'Заказ оформлен!',
        icon: <CheckCircleOutlined style={{ color: tokens.colors.success }} />,
        content: (
          <div style={{ marginTop: 8 }}>
            <div style={{ fontSize: 13, color: tokens.colors.textSecondary, marginBottom: 4 }}>
              Номер заказа: <strong style={{ color: tokens.colors.text }}>#{data.purchaseId}</strong>
            </div>
            <div style={{ fontSize: 20, fontWeight: 800, color: tokens.colors.primary }}>
              {data.totalRub.toLocaleString('ru-RU')} ₽
            </div>
          </div>
        ),
        okText: 'Смотреть заказы',
        onOk: () => navigate('/history'),
      })
    } catch {
      message.error('Не удалось оформить заказ')
    } finally {
      setCheckingOut(false)
    }
  }

  if (loading) return <Spin style={{ display: 'block', margin: '64px auto' }} />

  const isEmpty = !cart || (cart.bookings.length === 0 && cart.products.length === 0)

  // суммарная стоимость для панели
  function calcBookingTotal(b: CartBookingLineClientResponse): number {
    return calculateCartBookingTotal(b, seats, seatPrices, timePackages)
  }

  const bookingTotal = cart?.bookings.reduce((sum, b) => sum + calcBookingTotal(b), 0) ?? 0
  const productTotal = cart ? calculateCartProductTotal(cart.products) : 0

  return (
    <div style={{ maxWidth: 920 }}>
      <PageHeader
        title="Корзина"
        subtitle={cart && !isEmpty ? `${(cart.bookings.length + cart.products.length)} позиций` : undefined}
      />

      {isEmpty ? (
        /* Пустая корзина */
        <SectionCard>
          <EmptyState
            icon={<ShoppingOutlined />}
            title="Корзина пуста"
            description="Выберите клуб и забронируйте место или добавьте товары из магазина"
          />
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 20, flexWrap: 'wrap' }}>
            <Button type="primary" onClick={() => navigate('/clubs')}>
              Выбрать клуб
            </Button>
            <Button icon={<ShoppingOutlined />} onClick={() => navigate(`/clubs/${clubId}/shop`)}>
              Открыть магазин
            </Button>
            <Button icon={<HeartOutlined />} onClick={() => navigate('/clubs')}>
              Избранные
            </Button>
          </div>
        </SectionCard>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 280px',
          gap: 24,
          alignItems: 'start',
        }}
          className="cart-layout"
        >
          {/* Левая колонка — позиции */}
          <div>
            {/* Бронирования */}
            {cart!.bookings.length > 0 && (
              <div style={{ marginBottom: 24 }}>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  fontSize: 15, fontWeight: 700, color: tokens.colors.text,
                  marginBottom: 12,
                }}>
                  <CalendarOutlined style={{ color: tokens.colors.primary }} />
                  Бронирования
                  <span style={{
                    background: tokens.colors.primarySoft,
                    color: tokens.colors.primary,
                    fontSize: 12, fontWeight: 700,
                    padding: '1px 8px', borderRadius: 10,
                  }}>
                    {cart!.bookings.length}
                  </span>
                </div>
                {cart!.bookings.map((b) => (
                  <BookingCard
                    key={b.lineId}
                    booking={b}
                    seats={seats}
                    seatPrices={seatPrices}
                    timePackages={timePackages}
                    onDelete={() => deleteItem('booking', b.lineId)}
                  />
                ))}
                <button
                  onClick={() => navigate(`/clubs/${clubId}/booking`)}
                  style={{
                    background: 'none', border: 'none',
                    color: tokens.colors.primary, fontSize: 13,
                    cursor: 'pointer', padding: '4px 0',
                    fontWeight: 500,
                  }}
                >
                  + Добавить бронирование
                </button>
              </div>
            )}

            {/* Товары */}
            {cart!.products.length > 0 && (
              <div>
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  fontSize: 15, fontWeight: 700, color: tokens.colors.text,
                  marginBottom: 12,
                }}>
                  <TagOutlined style={{ color: tokens.colors.primary }} />
                  Товары
                  <span style={{
                    background: tokens.colors.primarySoft,
                    color: tokens.colors.primary,
                    fontSize: 12, fontWeight: 700,
                    padding: '1px 8px', borderRadius: 10,
                  }}>
                    {cart!.products.reduce((sum, p) => sum + p.qty, 0)} шт.
                  </span>
                </div>
                {cart!.products.map((p) => (
                  <ProductRow
                    key={p.lineId}
                    product={p}
                    onQtyChange={(qty) => updateQty(p.lineId, qty)}
                    onDelete={() => deleteItem('product', p.lineId)}
                  />
                ))}
                <button
                  onClick={() => navigate(`/clubs/${clubId}/shop`)}
                  style={{
                    background: 'none', border: 'none',
                    color: tokens.colors.primary, fontSize: 13,
                    cursor: 'pointer', padding: '4px 0',
                    fontWeight: 500,
                  }}
                >
                  + Добавить товары
                </button>
              </div>
            )}
          </div>

          {/* Правая колонка — сводка */}
          <OrderSummary
            bookingTotal={bookingTotal}
            productTotal={productTotal}
            hasBookings={(cart?.bookings.length ?? 0) > 0}
            checkingOut={checkingOut}
            onCheckout={handleCheckout}
          />
        </div>
      )}

      <style>{`
        @media (max-width: 680px) {
          .cart-layout {
            grid-template-columns: 1fr !important;
          }
          .cart-layout > div:last-child {
            position: static !important;
          }
        }
      `}</style>
    </div>
  )
}
