import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Alert, Button, Modal, Spin, App } from 'antd'
import {
  ArrowLeftOutlined,
  CalendarOutlined,
  ShopOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../../utils/apiClient'
import PageHeader from '../../../components/ui/PageHeader'
import SectionCard from '../../../components/ui/SectionCard'
import StatusBadge from '../../../components/ui/StatusBadge'
import { tokens } from '../../../theme/tokens'
import type { ClientPurchaseDetails, PaymentStatus } from '../../../types'

const STATUS_MAP: Record<PaymentStatus, { variant: 'success' | 'warning' | 'error' | 'info' | 'default'; label: string }> = {
  CREATED:  { variant: 'info',    label: 'Ожидает оплаты' },
  PAID:     { variant: 'success', label: 'Оплачен' },
  FAILED:   { variant: 'error',   label: 'Ошибка оплаты' },
  CANCELED: { variant: 'default', label: 'Отменён' },
  REFUND:   { variant: 'warning', label: 'Возврат' },
}

// --- Строка ключ-значение ---

function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      padding: '8px 0',
      gap: 16,
      borderBottom: `1px solid ${tokens.colors.border}`,
    }}>
      <span style={{ fontSize: 13, color: tokens.colors.textSecondary, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 13, fontWeight: 500, color: tokens.colors.text, textAlign: 'right' }}>{children}</span>
    </div>
  )
}

// --- Карточка бронирования в деталях ---

function BookingDetailCard({ booking }: { booking: ClientPurchaseDetails['bookingItems'][number] }) {
  const start = dayjs(booking.startAt)
  const end = dayjs(booking.endAt)
  const durationHours = end.diff(start, 'minute') / 60
  const durationLabel = durationHours % 1 === 0
    ? `${durationHours} ч`
    : `${durationHours.toFixed(1)} ч`

  return (
    <div style={{
      background: tokens.colors.surfaceAlt,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.md,
      padding: '14px 16px',
      marginBottom: 10,
    }}>
      {/* Дата и время */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: 14, color: tokens.colors.text, marginBottom: 2 }}>
            {start.format('D MMMM YYYY, dddd')}
          </div>
          <div style={{ fontSize: 13, color: tokens.colors.textSecondary, display: 'flex', alignItems: 'center', gap: 6 }}>
            <ClockCircleOutlined />
            {start.format('HH:mm')} — {end.format('HH:mm')}
            <span style={{
              background: tokens.colors.primarySoft,
              color: tokens.colors.primary,
              fontSize: 11, fontWeight: 600,
              padding: '1px 8px', borderRadius: 10,
            }}>
              {durationLabel}
            </span>
          </div>
        </div>
        <div style={{ fontSize: 16, fontWeight: 800, color: tokens.colors.text }}>
          {booking.totalRub.toLocaleString('ru-RU')} ₽
        </div>
      </div>

      {/* Места */}
      {booking.seatLabels.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {booking.seatLabels.map((label) => (
            <span key={label} style={{
              padding: '3px 10px',
              background: tokens.colors.surface,
              border: `1px solid ${tokens.colors.border}`,
              borderRadius: 14,
              fontSize: 12, fontWeight: 600,
              color: tokens.colors.text,
            }}>
              {label}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

// --- Основной компонент ---

export default function PurchaseDetailPage() {
  const { purchaseId } = useParams<{ purchaseId: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [purchase, setPurchase] = useState<ClientPurchaseDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  const id = Number(purchaseId)

  const loadPurchase = useCallback(() => {
    setLoading(true)
    setLoadError(false)
    apiClient.get<ClientPurchaseDetails>(`/purchases/${id}`)
      .then(({ data }) => setPurchase(data))
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { loadPurchase() }, [loadPurchase])

  async function handlePay() {
    setActionLoading(true)
    try {
      const { data } = await apiClient.post<{ paymentStatus: PaymentStatus }>(`/purchases/${id}/pay`)
      setPurchase((prev) => prev ? { ...prev, paymentStatus: data.paymentStatus } : prev)
      message.success('Заказ оплачен')
    } catch {
      message.error('Не удалось оплатить заказ')
    } finally {
      setActionLoading(false)
    }
  }

  function confirmCancel() {
    Modal.confirm({
      title: 'Отменить заказ?',
      icon: <ExclamationCircleOutlined style={{ color: tokens.colors.error }} />,
      content: 'Все бронирования будут освобождены. Действие необратимо.',
      okText: 'Отменить заказ',
      okButtonProps: { danger: true },
      cancelText: 'Назад',
      onOk: async () => {
        setActionLoading(true)
        try {
          const { data } = await apiClient.post<{ paymentStatus: PaymentStatus }>(`/purchases/${id}/cancel`)
          setPurchase((prev) => prev ? { ...prev, paymentStatus: data.paymentStatus } : prev)
          message.success('Заказ отменён')
        } catch {
          message.error('Не удалось отменить заказ')
        } finally {
          setActionLoading(false)
        }
      },
    })
  }

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />
  if (loadError || !purchase) return (
    <Alert
      type="error"
      message="Не удалось загрузить заказ"
      description="Проверьте соединение с сервером."
      action={<Button size="small" onClick={loadPurchase}>Повторить</Button>}
      style={{ maxWidth: 480 }}
    />
  )

  const statusInfo = STATUS_MAP[purchase.paymentStatus] ?? { variant: 'default' as const, label: purchase.paymentStatus }
  const canPay = purchase.paymentStatus === 'CREATED'
  const canCancel = purchase.paymentStatus === 'CREATED' || purchase.paymentStatus === 'PAID'

  return (
    <div style={{ maxWidth: 720 }}>
      {/* Назад */}
      <button
        onClick={() => navigate('/history')}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', gap: 6,
          color: tokens.colors.textSecondary, fontSize: 13,
          padding: '0 0 16px', fontWeight: 500,
        }}
      >
        <ArrowLeftOutlined />
        К истории
      </button>

      <PageHeader
        title={`Заказ #${purchase.purchaseId}`}
        subtitle={
          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <EnvironmentOutlined />
            {purchase.clubName}
          </span>
        }
        extra={<StatusBadge label={statusInfo.label} variant={statusInfo.variant} />}
      />

      {/* Основная информация */}
      <SectionCard style={{ marginBottom: 16 }}>
        <InfoRow label="Дата оформления">
          {dayjs(purchase.createdAt).format('D MMMM YYYY, HH:mm')}
        </InfoRow>
        <InfoRow label="Клуб">{purchase.clubName}</InfoRow>
        <InfoRow label="Статус">
          <StatusBadge label={statusInfo.label} variant={statusInfo.variant} />
        </InfoRow>
      </SectionCard>

      {/* Бронирования */}
      {purchase.bookingItems.length > 0 && (
        <SectionCard
          title={
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <CalendarOutlined style={{ color: tokens.colors.primary }} />
              Бронирования
              <span style={{
                background: tokens.colors.primarySoft, color: tokens.colors.primary,
                fontSize: 11, fontWeight: 700, padding: '1px 7px', borderRadius: 10,
              }}>
                {purchase.bookingItems.length}
              </span>
            </span>
          }
          style={{ marginBottom: 16 }}
        >
          {purchase.bookingItems.map((b) => (
            <BookingDetailCard key={b.bookingId} booking={b} />
          ))}
        </SectionCard>
      )}

      {/* Товары */}
      {purchase.productItems.length > 0 && (
        <SectionCard
          title={
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <ShopOutlined style={{ color: tokens.colors.primary }} />
              Товары
            </span>
          }
          style={{ marginBottom: 16 }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            {purchase.productItems.map((p, idx) => (
              <div
                key={p.productId}
                style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '10px 0', gap: 12,
                  borderBottom: idx < purchase.productItems.length - 1
                    ? `1px solid ${tokens.colors.border}`
                    : 'none',
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
                    {p.name}
                  </div>
                  <div style={{ fontSize: 12, color: tokens.colors.textSecondary, marginTop: 2 }}>
                    {p.unitRub} ₽ × {p.qty} шт.
                  </div>
                </div>
                <div style={{ fontWeight: 700, fontSize: 14, color: tokens.colors.text }}>
                  {p.totalRub.toLocaleString('ru-RU')} ₽
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      )}

      {/* Итого */}
      <SectionCard>
        {/* Строки итогов */}
        {purchase.bookingTotalRub > 0 && purchase.productsTotalRub > 0 && (
          <div style={{ marginBottom: 14 }}>
            {purchase.bookingTotalRub > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 13 }}>
                <span style={{ color: tokens.colors.textSecondary }}>
                  <CalendarOutlined style={{ marginRight: 5 }} />Бронирования
                </span>
                <span style={{ fontWeight: 600 }}>{purchase.bookingTotalRub.toLocaleString('ru-RU')} ₽</span>
              </div>
            )}
            {purchase.productsTotalRub > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 13 }}>
                <span style={{ color: tokens.colors.textSecondary }}>
                  <ShopOutlined style={{ marginRight: 5 }} />Товары
                </span>
                <span style={{ fontWeight: 600 }}>{purchase.productsTotalRub.toLocaleString('ru-RU')} ₽</span>
              </div>
            )}
            <div style={{ height: 1, background: tokens.colors.border, margin: '8px 0' }} />
          </div>
        )}

        {/* Итого крупно */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <span style={{ fontWeight: 700, fontSize: 16, color: tokens.colors.text }}>Итого</span>
          <span style={{ fontSize: 26, fontWeight: 800, color: tokens.colors.primary }}>
            {purchase.totalRub.toLocaleString('ru-RU')} ₽
          </span>
        </div>

        {/* Действия */}
        {(canPay || canCancel) && (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            {canPay && (
              <Button
                type="primary"
                size="large"
                icon={<CheckCircleOutlined />}
                loading={actionLoading}
                onClick={handlePay}
                style={{ fontWeight: 600 }}
              >
                Оплатить
              </Button>
            )}
            {canCancel && (
              <Button
                danger
                size="large"
                loading={actionLoading}
                onClick={confirmCancel}
              >
                Отменить заказ
              </Button>
            )}
          </div>
        )}
      </SectionCard>
    </div>
  )
}
