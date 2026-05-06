import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Spin, App } from 'antd'
import {
  CalendarOutlined,
  ShopOutlined,
  RightOutlined,
  HistoryOutlined,
  DollarOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../../utils/apiClient'
import PageHeader from '../../../components/ui/PageHeader'
import SectionCard from '../../../components/ui/SectionCard'
import StatCard from '../../../components/ui/StatCard'
import StatusBadge from '../../../components/ui/StatusBadge'
import EmptyState from '../../../components/ui/EmptyState'
import { tokens } from '../../../theme/tokens'
import type { ClientPurchaseListItem, PaymentStatus } from '../../../types'

const STATUS_MAP: Record<PaymentStatus, { variant: 'success' | 'warning' | 'error' | 'info' | 'default'; label: string }> = {
  CREATED:  { variant: 'info',    label: 'Ожидает оплаты' },
  PAID:     { variant: 'success', label: 'Оплачен' },
  FAILED:   { variant: 'error',   label: 'Ошибка оплаты' },
  CANCELED: { variant: 'default', label: 'Отменён' },
  REFUND:   { variant: 'warning', label: 'Возврат' },
}

// --- Карточка заказа ---

function PurchaseCard({
  purchase,
  onClick,
}: {
  purchase: ClientPurchaseListItem
  onClick: () => void
}) {
  const hasBooking = purchase.bookingTotalRub > 0
  const hasProducts = purchase.productsTotalRub > 0
  const { variant, label } = STATUS_MAP[purchase.paymentStatus] ?? { variant: 'default' as const, label: purchase.paymentStatus }
  const isPaid = purchase.paymentStatus === 'PAID'

  return (
    <div
      onClick={onClick}
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        padding: '16px 20px',
        cursor: 'pointer',
        transition: 'box-shadow 0.15s, transform 0.15s',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 16,
        borderLeft: isPaid
          ? `3px solid ${tokens.colors.success}`
          : purchase.paymentStatus === 'CREATED'
            ? `3px solid ${tokens.colors.info}`
            : `1px solid ${tokens.colors.border}`,
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.boxShadow = tokens.shadow.hover
        e.currentTarget.style.transform = 'translateY(-1px)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = tokens.shadow.card
        e.currentTarget.style.transform = ''
      }}
    >
      {/* Левая часть */}
      <div style={{ flex: 1, minWidth: 0 }}>
        {/* Название клуба */}
        <div style={{ fontWeight: 700, fontSize: 15, color: tokens.colors.text, marginBottom: 3 }}>
          {purchase.clubName}
        </div>

        {/* Дата */}
        <div style={{ fontSize: 12, color: tokens.colors.textSecondary, marginBottom: 10 }}>
          {dayjs(purchase.createdAt).format('D MMMM YYYY, HH:mm')}
          <span style={{ marginLeft: 6, color: tokens.colors.textMuted }}>
            #{purchase.purchaseId}
          </span>
        </div>

        {/* Теги состава */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {hasBooking && (
            <span style={{
              fontSize: 11, display: 'flex', alignItems: 'center', gap: 4,
              color: tokens.colors.info, background: tokens.colors.infoSoft,
              padding: '2px 8px', borderRadius: 12, fontWeight: 500,
            }}>
              <CalendarOutlined />
              Бронь · {purchase.bookingTotalRub.toLocaleString('ru-RU')} ₽
            </span>
          )}
          {hasProducts && (
            <span style={{
              fontSize: 11, display: 'flex', alignItems: 'center', gap: 4,
              color: tokens.colors.primary, background: tokens.colors.primarySoft,
              padding: '2px 8px', borderRadius: 12, fontWeight: 500,
            }}>
              <ShopOutlined />
              Товары · {purchase.productsTotalRub.toLocaleString('ru-RU')} ₽
            </span>
          )}
        </div>
      </div>

      {/* Правая часть */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8, flexShrink: 0 }}>
        <span style={{ fontSize: 20, fontWeight: 800, color: tokens.colors.text }}>
          {purchase.totalRub.toLocaleString('ru-RU')} ₽
        </span>
        <StatusBadge label={label} variant={variant} />
        <RightOutlined style={{ color: tokens.colors.textMuted, fontSize: 11 }} />
      </div>
    </div>
  )
}

// --- Основной компонент ---

export default function HistoryPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [purchases, setPurchases] = useState<ClientPurchaseListItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    apiClient.get<ClientPurchaseListItem[]>('/purchases')
      .then(({ data }) => setPurchases(data))
      .catch(() => message.error('Не удалось загрузить историю'))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const stats = useMemo(() => {
    const total = purchases.length
    const paid = purchases.filter((p) => p.paymentStatus === 'PAID').length
    const totalSpent = purchases
      .filter((p) => p.paymentStatus === 'PAID')
      .reduce((sum, p) => sum + p.totalRub, 0)
    return { total, paid, totalSpent }
  }, [purchases])

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />

  return (
    <div style={{ maxWidth: 760 }}>
      <PageHeader title="История заказов" />

      {purchases.length === 0 ? (
        <SectionCard>
          <EmptyState
            icon={<HistoryOutlined />}
            title="Заказов пока нет"
            description="Здесь будут отображаться все ваши бронирования и покупки"
            actionLabel="Выбрать клуб"
            onAction={() => navigate('/clubs')}
          />
        </SectionCard>
      ) : (
        <>
          {/* Статистика */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 12,
            marginBottom: 24,
          }}>
            <StatCard
              label="Заказов"
              value={stats.total}
              icon={<HistoryOutlined />}
              accentColor={tokens.colors.primary}
            />
            <StatCard
              label="Оплачено"
              value={stats.paid}
              icon={<CheckCircleOutlined />}
              accentColor={tokens.colors.success}
            />
            <StatCard
              label="Потрачено"
              value={`${stats.totalSpent.toLocaleString('ru-RU')} ₽`}
              icon={<DollarOutlined />}
              accentColor={tokens.colors.success}
            />
          </div>

          {/* Список заказов */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {purchases.map((p) => (
              <PurchaseCard
                key={p.purchaseId}
                purchase={p}
                onClick={() => navigate(`/history/${p.purchaseId}`)}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
