import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Descriptions,
  message,
  Popconfirm,
  Spin,
  Table,
  Tag,
} from 'antd'
import {
  ArrowLeftOutlined,
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
  AdminPurchaseDetailResponse,
  AdminPurchaseOrderItemDetail,
  AdminPurchaseSeatDetail,
} from '../../types'


const seatColumns: ColumnsType<AdminPurchaseSeatDetail> = [
  { title: 'Место', dataIndex: 'label', key: 'label' },
  {
    title: 'Тип',
    dataIndex: 'type',
    key: 'type',
    render: (type: string) =>
      type === 'VIP' ? <Tag color="gold">VIP</Tag> : <Tag>Стандарт</Tag>,
  },
]

const itemColumns: ColumnsType<AdminPurchaseOrderItemDetail> = [
  { title: 'Название', dataIndex: 'title', key: 'title' },
  { title: 'Кол-во', dataIndex: 'qty', key: 'qty', width: 80 },
  {
    title: 'Цена',
    dataIndex: 'priceRub',
    key: 'priceRub',
    width: 110,
    render: (v: number) => `${v} ₽`,
  },
  {
    title: 'Итого',
    dataIndex: 'subtotalRub',
    key: 'subtotalRub',
    width: 110,
    render: (v: number) => `${v} ₽`,
  },
]

export default function ClubPurchaseDetailPage() {
  const { clubId, purchaseId } = useParams<{ clubId: string; purchaseId: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<AdminPurchaseDetailResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [canceling, setCanceling] = useState(false)

  const fetchDetail = async () => {
    if (!clubId || !purchaseId) return
    setLoading(true)
    try {
      const res = await apiClient.get<AdminPurchaseDetailResponse>(
        `/admin/clubs/${clubId}/purchases/${purchaseId}`
      )
      setDetail(res.data)
    } catch {
      message.error('Не удалось загрузить данные покупки')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDetail()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clubId, purchaseId])

  const handleCancel = async () => {
    if (!clubId || !purchaseId) return
    setCanceling(true)
    try {
      await apiClient.post(`/admin/clubs/${clubId}/purchases/${purchaseId}/cancel`)
      message.success('Покупка отменена')
      await fetchDetail()
    } catch {
      message.error('Не удалось отменить покупку')
    } finally {
      setCanceling(false)
    }
  }

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
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(`/admin/club/${clubId}/purchases`)}>
          Назад
        </Button>
        <div style={{ marginTop: 16, color: tokens.colors.textMuted }}>Покупка не найдена</div>
      </div>
    )
  }

  const ps = PAYMENT_STATUS[detail.paymentStatus]

  return (
    <div style={{ maxWidth: 860 }}>
      <PageHeader
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Button
              icon={<ArrowLeftOutlined />}
              size="small"
              onClick={() => navigate(`/admin/club/${clubId}/purchases`)}
            />
            Покупка #{detail.id}
            <StatusBadge label={ps.label} variant={ps.variant} />
          </div>
        }
        extra={
          detail.paymentStatus !== 'CANCELED' && (
            <Popconfirm
              title="Отменить покупку?"
              description="Бронирование и заказ товаров также будут отменены."
              onConfirm={handleCancel}
              okText="Да, отменить"
              cancelText="Нет"
              okButtonProps={{ danger: true }}
            >
              <Button danger loading={canceling}>
                Отменить покупку
              </Button>
            </Popconfirm>
          )
        }
      />

      {/* Основная информация */}
      <SectionCard title="Основная информация" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Пользователь">
            {detail.userPhone ?? `#${detail.userId}`}
          </Descriptions.Item>
          <Descriptions.Item label="Дата">
            {dayjs(detail.createdAt).format('DD.MM.YYYY HH:mm')}
          </Descriptions.Item>
          <Descriptions.Item label="Бронирование">
            {detail.bookingTotalRub} ₽
          </Descriptions.Item>
          <Descriptions.Item label="Товары">
            {detail.productsTotalRub} ₽
          </Descriptions.Item>
          <Descriptions.Item label="Итого">
            <strong>{detail.totalRub} ₽</strong>
          </Descriptions.Item>
        </Descriptions>
      </SectionCard>

      {/* Бронирование */}
      {detail.booking && (
        <SectionCard
          title={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              Бронирование #{detail.booking.bookingId}
              <Tag color={BOOKING_STATUS[detail.booking.status].tagColor}>
                {BOOKING_STATUS[detail.booking.status].label}
              </Tag>
            </div>
          }
          style={{ marginBottom: 16 }}
        >
          <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
            <Descriptions.Item label="Начало">
              {dayjs(detail.booking.startAt).format('DD.MM.YYYY HH:mm')}
            </Descriptions.Item>
            <Descriptions.Item label="Конец">
              {dayjs(detail.booking.endAt).format('DD.MM.YYYY HH:mm')}
            </Descriptions.Item>
            <Descriptions.Item label="Длительность">
              {detail.booking.durationHours.toFixed(1)} ч.
            </Descriptions.Item>
            <Descriptions.Item label="Тариф">
              {detail.booking.rateRubPerHour} ₽/ч.
            </Descriptions.Item>
            <Descriptions.Item label="Сумма">
              {detail.booking.totalRub} ₽
            </Descriptions.Item>
          </Descriptions>
          <Table
            size="small"
            columns={seatColumns}
            dataSource={detail.booking.seats}
            rowKey="seatId"
            pagination={false}
          />
        </SectionCard>
      )}

      {/* Заказ товаров */}
      {detail.productOrder && (
        <SectionCard title={`Заказ товаров #${detail.productOrder.orderId}`}>
          <Table
            size="small"
            columns={itemColumns}
            dataSource={detail.productOrder.items}
            rowKey="title"
            pagination={false}
            summary={() => (
              <Table.Summary.Row>
                <Table.Summary.Cell index={0} colSpan={3} align="right">
                  <strong>Итого:</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={1}>
                  <strong>{detail.productOrder!.totalRub} ₽</strong>
                </Table.Summary.Cell>
              </Table.Summary.Row>
            )}
          />
        </SectionCard>
      )}
    </div>
  )
}
