import { CSSProperties, ReactNode, useCallback, useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Alert, Button, Col, Row, Spin, Table } from 'antd'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  RiseOutlined,
  BarChartOutlined,
  LayoutOutlined,
  MessageOutlined,
  ExclamationCircleOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import { BOOKING_STATUS, PAYMENT_STATUS } from '../../utils/statusMaps'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatusBadge from '../../components/ui/StatusBadge'
import { tokens } from '../../theme/tokens'
import type {
  AdminPurchaseResponse,
  BookingStatus,
  ClubDashboardResponse,
  ClubUserReportResponse,
  DashboardBookingPreview,
  DashboardPurchasePreview,
  PaymentStatus,
} from '../../types'

// --- KpiCard ---

interface KpiCardProps {
  value: ReactNode
  label: string
  hint?: string
  icon: ReactNode
  accentColor: string
  onClick?: () => void
  style?: CSSProperties
}

function KpiCard({ value, label, hint, icon, accentColor, onClick, style }: KpiCardProps) {
  const softBg =
    accentColor === tokens.colors.success  ? tokens.colors.successSoft  :
    accentColor === tokens.colors.warning  ? tokens.colors.warningSoft  :
    accentColor === tokens.colors.error    ? tokens.colors.errorSoft    :
    accentColor === tokens.colors.info     ? tokens.colors.infoSoft     :
    tokens.colors.primarySoft

  return (
    <div
      onClick={onClick}
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderLeft: `4px solid ${accentColor}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        padding: '20px 24px',
        cursor: onClick ? 'pointer' : 'default',
        transition: 'box-shadow 0.18s',
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
        ...style,
      }}
      onMouseEnter={e => { if (onClick) (e.currentTarget as HTMLDivElement).style.boxShadow = tokens.shadow.hover }}
      onMouseLeave={e => { if (onClick) (e.currentTarget as HTMLDivElement).style.boxShadow = tokens.shadow.card }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 36, fontWeight: 700, color: tokens.colors.text, lineHeight: 1, marginBottom: 6 }}>
            {value}
          </div>
          <div style={{ fontSize: 14, color: tokens.colors.textSecondary, fontWeight: 500 }}>
            {label}
          </div>
          {hint && (
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 4 }}>
              {hint}
            </div>
          )}
        </div>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: tokens.radius.md,
            background: softBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: accentColor,
            fontSize: 22,
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
      </div>
    </div>
  )
}

// --- AlertKpiCard (для тревожных метрик) ---

interface AlertCardProps {
  // undefined = загружается, null = ошибка загрузки, number = значение
  count: number | null | undefined
  label: string
  hint: string
  icon: ReactNode
  accentColor: string
  onClick: () => void
}

function AlertKpiCard({ count, label, hint, icon, accentColor, onClick }: AlertCardProps) {
  const isZero = count === 0
  const color = isZero ? tokens.colors.success : accentColor
  const display = count === undefined
    ? <Spin size="small" />
    : count === null
      ? <span style={{ color: tokens.colors.textMuted }}>—</span>
      : count

  return (
    <KpiCard
      value={display}
      label={label}
      hint={hint}
      icon={icon}
      accentColor={color}
      onClick={onClick}
    />
  )
}

// --- Колонки таблиц ---

function bookingColumns(navigate: (p: string) => void, clubId: string): ColumnsType<DashboardBookingPreview> {
  return [
    {
      title: 'Статус',
      dataIndex: 'status',
      width: 130,
      render: (s: BookingStatus) => {
        const { label, variant } = BOOKING_STATUS[s] ?? { label: s, variant: 'default' as const }
        return <StatusBadge label={label} variant={variant} />
      },
    },
    {
      title: 'Пользователь',
      dataIndex: 'userPhone',
      render: (p: string | null) => p ?? <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: 'Время',
      key: 'time',
      render: (_, r) => (
        <span style={{ fontSize: 13 }}>
          {dayjs(r.startAt).format('HH:mm')}
          <span style={{ color: tokens.colors.textMuted }}> — </span>
          {dayjs(r.endAt).format('HH:mm')}
          <span style={{ color: tokens.colors.textMuted, fontSize: 11, marginLeft: 4 }}>
            {dayjs(r.startAt).format('DD.MM')}
          </span>
        </span>
      ),
    },
    {
      title: '№',
      dataIndex: 'id',
      width: 60,
      render: (id: number) => (
        <Button type="link" size="small" style={{ padding: 0 }} onClick={() => navigate(`/admin/club/${clubId}/bookings/${id}`)}>
          #{id}
        </Button>
      ),
    },
  ]
}

function purchaseColumns(navigate: (p: string) => void, clubId: string): ColumnsType<DashboardPurchasePreview> {
  return [
    {
      title: 'Статус',
      dataIndex: 'paymentStatus',
      width: 120,
      render: (s: PaymentStatus) => {
        const { label, variant } = PAYMENT_STATUS[s] ?? { label: s, variant: 'default' as const }
        return <StatusBadge label={label} variant={variant} />
      },
    },
    {
      title: 'Пользователь',
      dataIndex: 'userPhone',
      render: (p: string | null) => p ?? <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: 'Сумма',
      dataIndex: 'totalRub',
      width: 110,
      render: (v: number) => (
        <span style={{ fontWeight: 600 }}>{v.toLocaleString('ru-RU')} ₽</span>
      ),
    },
    {
      title: '№',
      dataIndex: 'id',
      width: 60,
      render: (id: number) => (
        <Button type="link" size="small" style={{ padding: 0 }} onClick={() => navigate(`/admin/club/${clubId}/purchases/${id}`)}>
          #{id}
        </Button>
      ),
    },
  ]
}

// --- Основной компонент ---

export default function ClubDashboardPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()

  const [data, setData] = useState<ClubDashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [pendingCount, setPendingCount] = useState<number | null | undefined>(undefined)
  const [reportsCount, setReportsCount] = useState<number | null | undefined>(undefined)

  const load = useCallback(() => {
    if (!clubId) return
    setLoading(true)
    setError(false)
    setPendingCount(undefined)
    setReportsCount(undefined)

    // основные данные дашборда
    apiClient
      .get<ClubDashboardResponse>(`/admin/clubs/${clubId}/dashboard`)
      .then(r => setData(r.data))
      .catch(() => setError(true))
      .finally(() => setLoading(false))

    // дополнительные счётчики — параллельно, не блокируют рендер
    apiClient
      .get<AdminPurchaseResponse[]>(`/admin/clubs/${clubId}/purchases`, { params: { paymentStatus: 'CREATED' } })
      .then(r => setPendingCount(r.data.length))
      .catch(() => setPendingCount(null))

    apiClient
      .get<ClubUserReportResponse[]>(`/admin/clubs/${clubId}/user-reports`)
      .then(r => setReportsCount(r.data.length))
      .catch(() => setReportsCount(null))
  }, [clubId])

  useEffect(() => { load() }, [load])

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />
  if (error || !data || !clubId) return (
    <Alert
      type="error"
      message="Не удалось загрузить дашборд"
      description="Проверьте соединение с сервером."
      action={<Button size="small" onClick={load}>Повторить</Button>}
      style={{ maxWidth: 480 }}
    />
  )

  const occupancyPct = data.totalSeats > 0 ? Math.round(data.occupiedSeats / data.totalSeats * 100) : 0
  const occupancyColor =
    occupancyPct >= 80 ? tokens.colors.error :
    occupancyPct >= 50 ? tokens.colors.warning :
    tokens.colors.success

  return (
    <div>
      <PageHeader title="Дашборд" subtitle="Текущее состояние клуба" />

      {/* --- Основные KPI --- */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} xl={6}>
          <KpiCard
            value={data.activeBookingsCount}
            label="Активных броней"
            hint="прямо сейчас"
            icon={<CalendarOutlined />}
            accentColor={data.activeBookingsCount > 0 ? tokens.colors.success : tokens.colors.textMuted}
            onClick={() => navigate(`/admin/club/${clubId}/bookings`)}
          />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <KpiCard
            value={`${data.occupiedSeats} / ${data.totalSeats}`}
            label="Занято мест"
            hint={`${occupancyPct}% заполнено`}
            icon={<EnvironmentOutlined />}
            accentColor={occupancyColor}
            onClick={() => navigate(`/admin/club/${clubId}/bookings`)}
          />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <KpiCard
            value={data.upcomingTodayCount}
            label="Предстоящих сегодня"
            hint="запланированных броней"
            icon={<ClockCircleOutlined />}
            accentColor={tokens.colors.info}
          />
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <KpiCard
            value={`${data.todayRevenueRub.toLocaleString('ru-RU')} ₽`}
            label="Выручка сегодня"
            hint="оплаченные заказы"
            icon={<RiseOutlined />}
            accentColor={tokens.colors.success}
          />
        </Col>
      </Row>

      {/* --- Внимание + расширенная выручка --- */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={8}>
          <AlertKpiCard
            count={pendingCount}
            label="Ожидают оплаты"
            hint="незакрытые заказы"
            icon={<ShoppingCartOutlined />}
            accentColor={tokens.colors.warning}
            onClick={() => navigate(`/admin/club/${clubId}/purchases`)}
          />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <AlertKpiCard
            count={reportsCount}
            label="Жалобы пользователей"
            hint="нерассмотренные"
            icon={<ExclamationCircleOutlined />}
            accentColor={tokens.colors.error}
            onClick={() => navigate(`/admin/club/${clubId}/messages`)}
          />
        </Col>
        {data.weekRevenueRub !== null && (
          <Col xs={24} sm={12} lg={8}>
            <KpiCard
              value={`${data.weekRevenueRub.toLocaleString('ru-RU')} ₽`}
              label="Выручка за 7 дней"
              hint={data.monthRevenueRub !== null ? `за месяц: ${data.monthRevenueRub.toLocaleString('ru-RU')} ₽` : undefined}
              icon={<BarChartOutlined />}
              accentColor={tokens.colors.primary}
            />
          </Col>
        )}
      </Row>

      {/* --- Быстрые действия --- */}
      <SectionCard style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <span style={{ fontSize: 12, fontWeight: 600, color: tokens.colors.textMuted, textTransform: 'uppercase', letterSpacing: '0.05em', marginRight: 4 }}>
            Быстрые действия
          </span>
          <Button
            icon={<LayoutOutlined />}
            onClick={() => navigate(`/admin/club/${clubId}/bookings`)}
          >
            Схема зала
          </Button>
          <Button
            icon={<CalendarOutlined />}
            onClick={() => navigate(`/admin/club/${clubId}/bookings`)}
          >
            Активные брони
          </Button>
          <Button
            icon={<MessageOutlined />}
            onClick={() => navigate(`/admin/club/${clubId}/messages`)}
          >
            Сообщения
          </Button>
        </div>
      </SectionCard>

      {/* --- Последние операции --- */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <SectionCard
            title="Последние бронирования"
            extra={
              <Button type="link" size="small" onClick={() => navigate(`/admin/club/${clubId}/bookings`)}>
                Все →
              </Button>
            }
            noPadding
          >
            <Table
              dataSource={data.recentBookings}
              columns={bookingColumns(navigate, clubId)}
              rowKey="id"
              pagination={false}
              size="small"
              rowClassName={(r: DashboardBookingPreview) =>
                r.status === 'ACTIVE' ? 'dashboard-row-active' :
                r.status === 'CANCELED' ? 'dashboard-row-canceled' : ''
              }
            />
          </SectionCard>
        </Col>
        <Col xs={24} lg={10}>
          <SectionCard
            title="Последние покупки"
            extra={
              <Button type="link" size="small" onClick={() => navigate(`/admin/club/${clubId}/purchases`)}>
                Все →
              </Button>
            }
            noPadding
          >
            <Table
              dataSource={data.recentPurchases}
              columns={purchaseColumns(navigate, clubId)}
              rowKey="id"
              pagination={false}
              size="small"
              rowClassName={(r: DashboardPurchasePreview) =>
                r.paymentStatus === 'CREATED' ? 'dashboard-row-pending' :
                r.paymentStatus === 'FAILED'  ? 'dashboard-row-canceled' : ''
              }
            />
          </SectionCard>
        </Col>
      </Row>
    </div>
  )
}
