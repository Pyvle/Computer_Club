import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Card,
  Descriptions,
  message,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import type {
  AdminBookingDetailResponse,
  AdminPurchaseSeatDetail,
  BookingStatus,
} from '../../types'

const BOOKING_STATUS: Record<BookingStatus, string> = {
  UPCOMING: 'Предстоящее',
  ACTIVE: 'Активное',
  DONE: 'Завершено',
  CANCELED: 'Отменено',
}

const BOOKING_STATUS_COLOR: Record<BookingStatus, string> = {
  UPCOMING: 'blue',
  ACTIVE: 'success',
  DONE: 'default',
  CANCELED: 'error',
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDuration(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (m === 0) return `${h} ч`
  return `${h} ч ${m} мин`
}

const seatColumns: ColumnsType<AdminPurchaseSeatDetail> = [
  {
    title: 'Место',
    dataIndex: 'label',
    key: 'label',
  },
  {
    title: 'Тип',
    dataIndex: 'type',
    key: 'type',
    width: 120,
    render: (type: string) =>
      type === 'VIP' ? <Tag color="gold">VIP</Tag> : <Tag>Стандарт</Tag>,
  },
]

export default function ClubBookingDetailPage() {
  const { clubId, bookingId } = useParams<{ clubId: string; bookingId: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<AdminBookingDetailResponse | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!clubId || !bookingId) return
    setLoading(true)
    apiClient
      .get<AdminBookingDetailResponse>(`/admin/clubs/${clubId}/bookings/${bookingId}`)
      .then(res => setDetail(res.data))
      .catch(() => message.error('Не удалось загрузить данные бронирования'))
      .finally(() => setLoading(false))
  }, [clubId, bookingId])

  if (loading && !detail) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!detail) {
    return (
      <div style={{ padding: 24 }}>
        <Button onClick={() => navigate(`/admin/club/${clubId}/bookings`)}>← Назад</Button>
        <div style={{ marginTop: 16, color: '#999' }}>Бронирование не найдено</div>
      </div>
    )
  }

  return (
    <div style={{ padding: 24, maxWidth: 800 }}>
      {/* Заголовок */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <Button onClick={() => navigate(`/admin/club/${clubId}/bookings`)}>← Назад</Button>
        <Typography.Title level={4} style={{ margin: 0 }}>
          Бронирование #{detail.id}
        </Typography.Title>
        <Tag color={BOOKING_STATUS_COLOR[detail.status]}>
          {BOOKING_STATUS[detail.status]}
        </Tag>
      </div>

      {/* Основная информация */}
      <Card size="small" title="Основная информация" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Пользователь">
            {detail.userPhone ?? `#${detail.userId}`}
          </Descriptions.Item>
          <Descriptions.Item label="Статус">
            <Tag color={BOOKING_STATUS_COLOR[detail.status]}>
              {BOOKING_STATUS[detail.status]}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Время и тариф */}
      <Card size="small" title="Время и стоимость" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Начало">{formatDateTime(detail.startAt)}</Descriptions.Item>
          <Descriptions.Item label="Конец">{formatDateTime(detail.endAt)}</Descriptions.Item>
          <Descriptions.Item label="Длительность">
            {formatDuration(detail.durationHours)}
          </Descriptions.Item>
          <Descriptions.Item label="Тариф">{detail.rateRubPerHour} ₽/ч</Descriptions.Item>
          <Descriptions.Item label="Сумма">
            <strong>{detail.totalRub} ₽</strong>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Места */}
      <Card size="small" title="Места" style={{ marginBottom: 16 }}>
        {detail.seats.length > 0 ? (
          <Table
            size="small"
            columns={seatColumns}
            dataSource={detail.seats}
            rowKey="seatId"
            pagination={false}
          />
        ) : (
          <span style={{ color: '#999' }}>Места не указаны</span>
        )}
      </Card>

      {/* Связанная покупка */}
      <Card size="small" title="Связанная покупка">
        {detail.purchaseId ? (
          <Space>
            <span>Покупка #{detail.purchaseId}</span>
            <Button
              type="primary"
              size="small"
              onClick={() => navigate(`/admin/club/${clubId}/purchases/${detail.purchaseId}`)}
            >
              Открыть покупку
            </Button>
          </Space>
        ) : (
          <span style={{ color: '#999' }}>Покупка не привязана</span>
        )}
      </Card>
    </div>
  )
}
