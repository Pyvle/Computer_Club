import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Card,
  DatePicker,
  Input,
  message,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { AdminPurchaseResponse, PaymentStatus } from '../../types'

// --- Вспомогательные компоненты ---

const STATUS_LABELS: Record<PaymentStatus, string> = {
  CREATED: 'Ожидает оплаты',
  PAID: 'Оплачено',
  CANCELED: 'Отменено',
  FAILED: 'Ошибка оплаты',
  REFUND: 'Возврат',
}

const STATUS_COLORS: Record<PaymentStatus, string> = {
  CREATED: 'warning',
  PAID: 'success',
  CANCELED: 'default',
  FAILED: 'error',
  REFUND: 'processing',
}

function StatusTag({ status }: { status: PaymentStatus }) {
  return <Tag color={STATUS_COLORS[status]}>{STATUS_LABELS[status]}</Tag>
}

function SeatDisplay({ labels }: { labels: string[] }) {
  if (labels.length === 0) return <span style={{ color: '#999' }}>—</span>
  if (labels.length <= 3) return <span>{labels.join(', ')}</span>
  const visible = labels.slice(0, 3)
  return <span>{visible.join(', ')} +{labels.length - 3}</span>
}

// --- Основной компонент ---

export default function ClubPurchasesPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const [purchases, setPurchases] = useState<AdminPurchaseResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | null>(null)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [onlyPending, setOnlyPending] = useState(false)

  const fetchPurchases = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    try {
      const params: Record<string, string> = {}
      if (dateRange) {
        params.from = dateRange[0].format('YYYY-MM-DDTHH:mm:ss')
        params.to = dateRange[1].format('YYYY-MM-DDTHH:mm:ss')
      }
      if (onlyPending) {
        params.status = 'CREATED'
      } else if (statusFilter) {
        params.status = statusFilter
      }
      const res = await apiClient.get<AdminPurchaseResponse[]>(
        `/admin/clubs/${clubId}/purchases`,
        { params }
      )
      setPurchases(res.data)
    } catch {
      message.error('Не удалось загрузить покупки')
    } finally {
      setLoading(false)
    }
  }, [clubId, dateRange, statusFilter, onlyPending])

  useEffect(() => {
    fetchPurchases()
  }, [fetchPurchases])

  const handleCancel = async (id: number) => {
    setActionLoadingId(id)
    try {
      await apiClient.post(`/admin/clubs/${clubId}/purchases/${id}/cancel`)
      message.success('Покупка отменена')
      fetchPurchases()
    } catch {
      message.error('Не удалось отменить покупку')
    } finally {
      setActionLoadingId(null)
    }
  }

  const filteredPurchases = useMemo(() => {
    if (!searchText.trim()) return purchases
    const q = searchText.trim().toLowerCase()
    return purchases.filter(
      p =>
        p.id.toString().includes(q) ||
        (p.userPhone ?? '').toLowerCase().includes(q)
    )
  }, [purchases, searchText])

  const stats = useMemo(() => {
    const total = filteredPurchases.length
    const createdCount = filteredPurchases.filter(p => p.paymentStatus === 'CREATED').length
    const paidCount = filteredPurchases.filter(p => p.paymentStatus === 'PAID').length
    const canceledCount = filteredPurchases.filter(p => p.paymentStatus === 'CANCELED').length
    const totalSum = filteredPurchases.reduce((sum, p) => sum + p.totalAmountRub, 0)
    return { total, createdCount, paidCount, canceledCount, totalSum }
  }, [filteredPurchases])

  const columns: ColumnsType<AdminPurchaseResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: 'Дата',
      key: 'createdAt',
      render: (_, r) => new Date(r.createdAt).toLocaleString('ru-RU'),
      width: 170,
    },
    {
      title: 'Пользователь',
      key: 'user',
      render: (_, r) => r.userPhone ?? `Пользователь #${r.userId}`,
    },
    {
      title: 'Места',
      key: 'seats',
      render: (_, r) => <SeatDisplay labels={r.seatLabels ?? []} />,
    },
    {
      title: 'Товары',
      key: 'products',
      render: (_, r) => (r.productCount ? `${r.productCount} шт.` : '—'),
      width: 100,
    },
    {
      title: 'Сумма',
      key: 'total',
      render: (_, r) => `${r.totalAmountRub} ₽`,
      width: 110,
    },
    {
      title: 'Статус',
      key: 'status',
      render: (_, r) => <StatusTag status={r.paymentStatus} />,
      width: 160,
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button
            size="small"
            onClick={() => navigate(`/admin/club/${clubId}/purchases/${record.id}`)}
          >
            Открыть
          </Button>
          {record.paymentStatus !== 'CANCELED' && (
            <Popconfirm
              title="Отменить покупку?"
              onConfirm={() => handleCancel(record.id)}
              okText="Да"
              cancelText="Нет"
            >
              <Button
                size="small"
                danger
                loading={actionLoadingId === record.id}
                disabled={actionLoadingId !== null && actionLoadingId !== record.id}
              >
                Отменить
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>Покупки</h2>

      {/* Карточки статистики */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic title="Всего покупок" value={stats.total} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 140 }}>
          <Statistic
            title="Ожидают оплаты"
            value={stats.createdCount}
            valueStyle={{ color: '#faad14' }}
          />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic
            title="Оплачено"
            value={stats.paidCount}
            valueStyle={{ color: '#52c41a' }}
          />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic title="Отменено" value={stats.canceledCount} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 150 }}>
          <Statistic title="Общая сумма" value={stats.totalSum} suffix="₽" />
        </Card>
      </div>

      {/* Фильтры */}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="ID, телефон"
          allowClear
          style={{ width: 200 }}
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
        />
        <DatePicker.RangePicker
          showTime
          onChange={val => setDateRange(val as [Dayjs, Dayjs] | null)}
        />
        <Select
          style={{ width: 180 }}
          placeholder="Все статусы"
          allowClear
          disabled={onlyPending}
          value={onlyPending ? undefined : statusFilter}
          onChange={v => setStatusFilter(v ?? null)}
          options={[
            { value: 'CREATED', label: 'Ожидает оплаты' },
            { value: 'PAID', label: 'Оплачено' },
            { value: 'FAILED', label: 'Ошибка оплаты' },
            { value: 'CANCELED', label: 'Отменено' },
            { value: 'REFUND', label: 'Возврат' },
          ]}
        />
        <Button
          type={onlyPending ? 'primary' : 'default'}
          onClick={() => setOnlyPending(v => !v)}
        >
          Ожидают оплаты
        </Button>
      </Space>

      <Table
        columns={columns}
        dataSource={filteredPurchases}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
      />
    </div>
  )
}
