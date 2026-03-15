import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Card,
  Descriptions,
  message,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import type {
  AdminPurchaseDetailResponse,
  AdminPurchaseOrderItemDetail,
  AdminPurchaseSeatDetail,
  BookingStatus,
  PaymentStatus,
} from '../../types'

const PAYMENT_STATUS: Record<PaymentStatus, string> = {
  CREATED: 'Ожидает оплаты',
  PAID: 'Оплачено',
  CANCELED: 'Отменено',
  FAILED: 'Ошибка',
  REFUND: 'Возврат',
}

const PAYMENT_STATUS_COLOR: Record<PaymentStatus, string> = {
  CREATED: 'warning',
  PAID: 'success',
  CANCELED: 'default',
  FAILED: 'error',
  REFUND: 'processing',
}

const BOOKING_STATUS: Record<BookingStatus, string> = {
  UPCOMING: 'Предстоит',
  ACTIVE: 'Активна',
  DONE: 'Завершена',
  CANCELED: 'Отменена',
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
    render: (type: string) => (type === 'VIP' ? <Tag color="gold">VIP</Tag> : <Tag>Стандарт</Tag>),
  },
]

const itemColumns: ColumnsType<AdminPurchaseOrderItemDetail> = [
  {
    title: 'Название',
    dataIndex: 'title',
    key: 'title',
  },
  {
    title: 'Кол-во',
    dataIndex: 'qty',
    key: 'qty',
    width: 90,
  },
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
      // полный рефетч — статусы брони и заказа могли измениться
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
        <Button onClick={() => navigate(`/admin/club/${clubId}/purchases`)}>← Назад</Button>
        <div style={{ marginTop: 16, color: '#999' }}>Покупка не найдена</div>
      </div>
    )
  }

  return (
    <div style={{ padding: 24, maxWidth: 900 }}>
      {/* Заголовок */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <Button onClick={() => navigate(`/admin/club/${clubId}/purchases`)}>← Назад</Button>
        <Typography.Title level={4} style={{ margin: 0 }}>
          Покупка #{detail.id}
        </Typography.Title>
        <Tag color={PAYMENT_STATUS_COLOR[detail.paymentStatus]}>
          {PAYMENT_STATUS[detail.paymentStatus]}
        </Tag>
        <div style={{ flex: 1 }} />
        {detail.paymentStatus !== 'CANCELED' && (
          <Popconfirm
            title="Отменить покупку?"
            description="Бронирование и заказ товаров также будут отменены."
            onConfirm={handleCancel}
            okText="Да, отменить"
            cancelText="Нет"
            okButtonProps={{ danger: true }}
          >
            <Button danger loading={canceling}>
              Отменить
            </Button>
          </Popconfirm>
        )}
      </div>

      {/* Основная информация */}
      <Card size="small" title="Основная информация" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="Пользователь">
            {detail.userPhone ?? `#${detail.userId}`}
          </Descriptions.Item>
          <Descriptions.Item label="Дата">
            {formatDateTime(detail.createdAt)}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Суммы */}
      <Card size="small" title="Суммы" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={3}>
          <Descriptions.Item label="Бронирование">{detail.bookingTotalRub} ₽</Descriptions.Item>
          <Descriptions.Item label="Товары">{detail.productsTotalRub} ₽</Descriptions.Item>
          <Descriptions.Item label="Итого">
            <strong>{detail.totalRub} ₽</strong>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Бронирование */}
      {detail.booking && (
        <Card
          size="small"
          title={
            <Space>
              {`Бронирование #${detail.booking.bookingId}`}
              <Tag color={BOOKING_STATUS_COLOR[detail.booking.status]}>
                {BOOKING_STATUS[detail.booking.status]}
              </Tag>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
            <Descriptions.Item label="Начало">
              {formatDateTime(detail.booking.startAt)}
            </Descriptions.Item>
            <Descriptions.Item label="Конец">
              {formatDateTime(detail.booking.endAt)}
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
        </Card>
      )}

      {/* Заказ товаров */}
      {detail.productOrder && (
        <Card
          size="small"
          title={`Заказ товаров #${detail.productOrder.orderId}`}
        >
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
        </Card>
      )}
    </div>
  )
}
