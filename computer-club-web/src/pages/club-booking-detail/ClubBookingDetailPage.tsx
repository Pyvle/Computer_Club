import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Descriptions, message, Space, Spin, Table, Tag } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import { BOOKING_STATUS } from '../../utils/statusMaps'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatusBadge from '../../components/ui/StatusBadge'
import { tokens } from '../../theme/tokens'
import type { AdminBookingDetailResponse, AdminPurchaseSeatDetail } from '../../types'

function formatDuration(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (m === 0) return `${h} ч`
  return `${h} ч ${m} мин`
}

const seatColumns: ColumnsType<AdminPurchaseSeatDetail> = [
  { title: 'Место', dataIndex: 'label', key: 'label' },
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
      .then((res) => setDetail(res.data))
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
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/admin/club/${clubId}/bookings`)}>
          Назад
        </Button>
        <div style={{ marginTop: 16, color: tokens.colors.textMuted }}>Бронирование не найдено</div>
      </div>
    )
  }

  const bs = BOOKING_STATUS[detail.status]

  return (
    <div style={{ maxWidth: 800 }}>
      <PageHeader
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Button
              icon={<ArrowLeftOutlined />}
              size="small"
              onClick={() => navigate(`/admin/club/${clubId}/bookings`)}
            />
            Бронирование #{detail.id}
            <StatusBadge label={bs.label} variant={bs.variant} />
          </div>
        }
      />

      {/* Основная информация */}
      <SectionCard title="Основная информация" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Пользователь">
            {detail.userPhone ?? `#${detail.userId}`}
          </Descriptions.Item>
          <Descriptions.Item label="Статус">
            <StatusBadge label={bs.label} variant={bs.variant} />
          </Descriptions.Item>
        </Descriptions>
      </SectionCard>

      {/* Время и стоимость */}
      <SectionCard title="Время и стоимость" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Начало">
            {dayjs(detail.startAt).format('DD.MM.YYYY HH:mm')}
          </Descriptions.Item>
          <Descriptions.Item label="Конец">
            {dayjs(detail.endAt).format('DD.MM.YYYY HH:mm')}
          </Descriptions.Item>
          <Descriptions.Item label="Длительность">
            {formatDuration(detail.durationHours)}
          </Descriptions.Item>
          <Descriptions.Item label="Тариф">
            {detail.rateRubPerHour} ₽/ч
          </Descriptions.Item>
          <Descriptions.Item label="Сумма">
            <strong>{detail.totalRub} ₽</strong>
          </Descriptions.Item>
        </Descriptions>
      </SectionCard>

      {/* Места */}
      <SectionCard title="Места" style={{ marginBottom: 16 }}>
        {detail.seats.length > 0 ? (
          <Table
            size="small"
            columns={seatColumns}
            dataSource={detail.seats}
            rowKey="seatId"
            pagination={false}
          />
        ) : (
          <span style={{ color: tokens.colors.textMuted }}>Места не указаны</span>
        )}
      </SectionCard>

      {/* Связанная покупка */}
      <SectionCard title="Связанная покупка">
        {detail.purchaseId ? (
          <Space>
            <span style={{ color: tokens.colors.textSecondary }}>Покупка #{detail.purchaseId}</span>
            <Button
              type="primary"
              size="small"
              onClick={() => navigate(`/admin/club/${clubId}/purchases/${detail.purchaseId}`)}
            >
              Открыть покупку
            </Button>
          </Space>
        ) : (
          <span style={{ color: tokens.colors.textMuted }}>Покупка не привязана</span>
        )}
      </SectionCard>
    </div>
  )
}
