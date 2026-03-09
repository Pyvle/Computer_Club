import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Card,
  DatePicker,
  Input,
  message,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { AdminBookingResponse, BookingStatus } from '../../types'

// --- Вспомогательные компоненты ---

const STATUS_LABELS: Record<BookingStatus, string> = {
  UPCOMING: 'Предстоящее',
  ACTIVE: 'Активное',
  DONE: 'Завершено',
  CANCELED: 'Отменено',
}

const STATUS_COLORS: Record<BookingStatus, string> = {
  UPCOMING: 'blue',
  ACTIVE: 'success',
  DONE: 'default',
  CANCELED: 'error',
}

function StatusTag({ status }: { status: BookingStatus }) {
  return <Tag color={STATUS_COLORS[status]}>{STATUS_LABELS[status]}</Tag>
}

function SeatDisplay({ labels }: { labels: string[] }) {
  if (labels.length === 0) return <span style={{ color: '#999' }}>—</span>
  if (labels.length <= 3) return <span>{labels.join(', ')}</span>
  return <span>{labels.slice(0, 3).join(', ')} +{labels.length - 3}</span>
}

function formatDuration(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (m === 0) return `${h} ч`
  return `${h} ч ${m} мин`
}

// --- Основной компонент ---

export default function ClubBookingsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const [bookings, setBookings] = useState<AdminBookingResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<BookingStatus | null>(null)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)

  const fetchBookings = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    try {
      const params: Record<string, string> = {}
      if (dateRange) {
        params.from = dateRange[0].format('YYYY-MM-DDTHH:mm:ss')
        params.to = dateRange[1].format('YYYY-MM-DDTHH:mm:ss')
      }
      if (statusFilter) params.status = statusFilter
      const res = await apiClient.get<AdminBookingResponse[]>(
        `/admin/clubs/${clubId}/bookings`,
        { params }
      )
      setBookings(res.data)
    } catch {
      message.error('Не удалось загрузить бронирования')
    } finally {
      setLoading(false)
    }
  }, [clubId, dateRange, statusFilter])

  useEffect(() => {
    fetchBookings()
  }, [fetchBookings])

  const filtered = useMemo(() => {
    if (!searchText.trim()) return bookings
    const q = searchText.trim().toLowerCase()
    return bookings.filter(
      b =>
        b.id.toString().includes(q) ||
        (b.userPhone ?? '').toLowerCase().includes(q)
    )
  }, [bookings, searchText])

  const stats = useMemo(() => ({
    total: filtered.length,
    upcoming: filtered.filter(b => b.status === 'UPCOMING').length,
    active: filtered.filter(b => b.status === 'ACTIVE').length,
    done: filtered.filter(b => b.status === 'DONE').length,
    canceled: filtered.filter(b => b.status === 'CANCELED').length,
  }), [filtered])

  const columns: ColumnsType<AdminBookingResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
    },
    {
      title: 'Пользователь',
      key: 'user',
      render: (_, r) => r.userPhone ?? `#${r.userId}`,
      width: 150,
    },
    {
      title: 'Места',
      key: 'seats',
      render: (_, r) => <SeatDisplay labels={r.seatLabels} />,
    },
    {
      title: 'Начало',
      key: 'startAt',
      render: (_, r) => new Date(r.startAt).toLocaleString('ru-RU'),
      width: 160,
    },
    {
      title: 'Конец',
      key: 'endAt',
      render: (_, r) => new Date(r.endAt).toLocaleString('ru-RU'),
      width: 160,
    },
    {
      title: 'Длительность',
      key: 'duration',
      render: (_, r) => formatDuration(r.durationHours),
      width: 120,
    },
    {
      title: 'Сумма',
      key: 'total',
      render: (_, r) => `${r.totalRub} ₽`,
      width: 100,
    },
    {
      title: 'Статус',
      key: 'status',
      render: (_, r) => <StatusTag status={r.status} />,
      width: 140,
    },
    {
      title: 'Покупка',
      key: 'purchase',
      render: (_, r) =>
        r.purchaseId ? (
          <Button
            type="link"
            size="small"
            style={{ padding: 0 }}
            onClick={() => navigate(`/admin/club/${clubId}/purchases/${r.purchaseId}`)}
          >
            #{r.purchaseId}
          </Button>
        ) : (
          <span style={{ color: '#999' }}>—</span>
        ),
      width: 100,
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Button
          size="small"
          onClick={() => navigate(`/admin/club/${clubId}/bookings/${record.id}`)}
        >
          Открыть
        </Button>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>Бронирования</h2>

      {/* Карточки статистики */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic title="Всего" value={stats.total} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 130 }}>
          <Statistic title="Предстоящие" value={stats.upcoming} valueStyle={{ color: '#1677ff' }} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic title="Активные" value={stats.active} valueStyle={{ color: '#52c41a' }} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 130 }}>
          <Statistic title="Завершённые" value={stats.done} />
        </Card>
        <Card size="small" style={{ flex: 1, minWidth: 120 }}>
          <Statistic title="Отменённые" value={stats.canceled} />
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
          value={statusFilter}
          onChange={v => setStatusFilter(v ?? null)}
          options={[
            { value: 'UPCOMING', label: 'Предстоящие' },
            { value: 'ACTIVE', label: 'Активные' },
            { value: 'DONE', label: 'Завершённые' },
            { value: 'CANCELED', label: 'Отменённые' },
          ]}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={filtered}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
      />
    </div>
  )
}
