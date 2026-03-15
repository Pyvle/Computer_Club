import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Card, Col, Row, Spin, Statistic, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type {
  BookingStatus,
  ClubDashboardResponse,
  DashboardBookingPreview,
  DashboardPurchasePreview,
  PaymentStatus,
} from '../../types'

// --- Статусы броней ---

const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  UPCOMING: 'Предстоящее',
  ACTIVE: 'Активное',
  DONE: 'Завершено',
  CANCELED: 'Отменено',
}

const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  UPCOMING: 'blue',
  ACTIVE: 'success',
  DONE: 'default',
  CANCELED: 'error',
}

// --- Статусы покупок ---

const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  CREATED: 'Ожидает',
  PAID: 'Оплачено',
  FAILED: 'Ошибка',
  CANCELED: 'Отменено',
  REFUND: 'Возврат',
}

const PAYMENT_STATUS_COLORS: Record<PaymentStatus, string> = {
  CREATED: 'blue',
  PAID: 'success',
  FAILED: 'error',
  CANCELED: 'default',
  REFUND: 'warning',
}

// --- Колонки таблиц ---

function bookingColumns(
  navigate: (path: string) => void,
  clubId: string
): ColumnsType<DashboardBookingPreview> {
  return [
    { title: '№', dataIndex: 'id', key: 'id', width: 60,
      render: (id: number) => (
        <Button type="link" size="small" onClick={() => navigate(`/admin/club/${clubId}/bookings/${id}`)}>
          #{id}
        </Button>
      ),
    },
    { title: 'Пользователь', dataIndex: 'userPhone', key: 'userPhone',
      render: (p: string | null) => p ?? '—',
    },
    { title: 'Начало', dataIndex: 'startAt', key: 'startAt',
      render: (v: string) => dayjs(v).format('DD.MM HH:mm'),
    },
    { title: 'Статус', dataIndex: 'status', key: 'status',
      render: (s: BookingStatus) => (
        <Tag color={BOOKING_STATUS_COLORS[s]}>{BOOKING_STATUS_LABELS[s]}</Tag>
      ),
    },
  ]
}

function purchaseColumns(
  navigate: (path: string) => void,
  clubId: string
): ColumnsType<DashboardPurchasePreview> {
  return [
    { title: '№', dataIndex: 'id', key: 'id', width: 60,
      render: (id: number) => (
        <Button type="link" size="small" onClick={() => navigate(`/admin/club/${clubId}/purchases/${id}`)}>
          #{id}
        </Button>
      ),
    },
    { title: 'Пользователь', dataIndex: 'userPhone', key: 'userPhone',
      render: (p: string | null) => p ?? '—',
    },
    { title: 'Сумма', dataIndex: 'totalRub', key: 'totalRub',
      render: (v: number) => `${v.toLocaleString('ru-RU')} ₽`,
    },
    { title: 'Статус', dataIndex: 'paymentStatus', key: 'paymentStatus',
      render: (s: PaymentStatus) => (
        <Tag color={PAYMENT_STATUS_COLORS[s]}>{PAYMENT_STATUS_LABELS[s]}</Tag>
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

  useEffect(() => {
    if (!clubId) return
    setLoading(true)
    apiClient
      .get<ClubDashboardResponse>(`/admin/clubs/${clubId}/dashboard`)
      .then((r) => setData(r.data))
      .finally(() => setLoading(false))
  }, [clubId])

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 300 }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!data || !clubId) return null

  return (
    <div>
      <h2 style={{ marginTop: 0, marginBottom: 20 }}>Дашборд</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Активных броней"
              value={data.activeBookingsCount}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Предстоящих сегодня"
              value={data.upcomingTodayCount}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Занято мест"
              value={data.occupiedSeats}
              suffix={`/ ${data.totalSeats}`}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Выручка сегодня"
              value={data.todayRevenueRub}
              suffix="₽"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {(data.weekRevenueRub !== null || data.monthRevenueRub !== null) && (
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {data.weekRevenueRub !== null && (
            <Col span={6}>
              <Card>
                <Statistic
                  title="Выручка за 7 дней"
                  value={data.weekRevenueRub}
                  suffix="₽"
                  valueStyle={{ color: '#52c41a' }}
                />
              </Card>
            </Col>
          )}
          {data.monthRevenueRub !== null && (
            <Col span={6}>
              <Card>
                <Statistic
                  title="Выручка за 30 дней"
                  value={data.monthRevenueRub}
                  suffix="₽"
                  valueStyle={{ color: '#52c41a' }}
                />
              </Card>
            </Col>
          )}
        </Row>
      )}

      <Row gutter={[16, 16]}>
        <Col span={14}>
          <Card
            title="Последние бронирования"
            extra={
              <Button type="link" onClick={() => navigate(`/admin/club/${clubId}/bookings`)}>
                Все бронирования →
              </Button>
            }
          >
            <Table
              dataSource={data.recentBookings}
              columns={bookingColumns(navigate, clubId)}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
        <Col span={10}>
          <Card
            title="Последние покупки"
            extra={
              <Button type="link" onClick={() => navigate(`/admin/club/${clubId}/purchases`)}>
                Все покупки →
              </Button>
            }
          >
            <Table
              dataSource={data.recentPurchases}
              columns={purchaseColumns(navigate, clubId)}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
