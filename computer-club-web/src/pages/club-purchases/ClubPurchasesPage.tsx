import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Col,
  DatePicker,
  Descriptions,
  Drawer,
  Input,
  message,
  Popconfirm,
  Row,
  Space,
  Spin,
  Table,
  Tag,
} from 'antd'
import {
  DollarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  ExportOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import { BOOKING_STATUS, PAYMENT_STATUS } from '../../utils/statusMaps'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import { tokens } from '../../theme/tokens'
import type {
  AdminPurchaseDetailResponse,
  AdminPurchaseResponse,
  PaymentStatus,
} from '../../types'


// --- Quick-filter кнопки ---

interface QuickFilter {
  label: string
  status: PaymentStatus | null
  pendingOnly?: boolean
}

const QUICK_FILTERS: QuickFilter[] = [
  { label: 'Все',             status: null  },
  { label: 'Ожидают оплаты', status: 'CREATED' },
  { label: 'Оплачено',        status: 'PAID' },
  { label: 'Ошибка',          status: 'FAILED' },
  { label: 'Отменено',        status: 'CANCELED' },
  { label: 'Возврат',         status: 'REFUND' },
]

function QuickFilters({
  active,
  onChange,
}: {
  active: PaymentStatus | null
  onChange: (s: PaymentStatus | null) => void
}) {
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {QUICK_FILTERS.map((f) => {
        const isActive = f.status === active
        return (
          <button
            key={f.label}
            onClick={() => onChange(f.status)}
            style={{
              padding: '4px 12px',
              borderRadius: 20,
              fontSize: 13,
              fontWeight: isActive ? 600 : 400,
              cursor: 'pointer',
              border: `1px solid ${isActive ? tokens.colors.primary : tokens.colors.border}`,
              background: isActive ? tokens.colors.primarySoft : tokens.colors.surface,
              color: isActive ? tokens.colors.primary : tokens.colors.textSecondary,
              transition: 'all 0.15s',
              outline: 'none',
            }}
          >
            {f.label}
          </button>
        )
      })}
    </div>
  )
}

// --- Drawer быстрого просмотра покупки ---

function PurchaseDrawer({
  purchaseId,
  clubId,
  onClose,
  onCancel,
}: {
  purchaseId: number | null
  clubId: string
  onClose: () => void
  onCancel: (id: number) => void
}) {
  const navigate = useNavigate()
  const [detail, setDetail] = useState<AdminPurchaseDetailResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [canceling, setCanceling] = useState(false)

  useEffect(() => {
    if (!purchaseId) return
    setDetail(null)
    setLoading(true)
    apiClient
      .get<AdminPurchaseDetailResponse>(`/admin/clubs/${clubId}/purchases/${purchaseId}`)
      .then(({ data }) => setDetail(data))
      .catch(() => message.error('Не удалось загрузить данные покупки'))
      .finally(() => setLoading(false))
  }, [purchaseId, clubId])

  async function handleCancel() {
    if (!detail) return
    setCanceling(true)
    try {
      await apiClient.post(`/admin/clubs/${clubId}/purchases/${detail.id}/cancel`)
      message.success('Покупка отменена')
      onCancel(detail.id)
      onClose()
    } catch {
      message.error('Не удалось отменить покупку')
    } finally {
      setCanceling(false)
    }
  }

  const ps = detail ? PAYMENT_STATUS[detail.paymentStatus] : null

  return (
    <Drawer
      open={purchaseId !== null}
      onClose={onClose}
      width={480}
      title={detail ? `Покупка #${detail.id}` : 'Покупка'}
      styles={{ body: { padding: '16px 20px' } }}
      footer={
        detail && (
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
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
            <Button
              type="primary"
              icon={<ExportOutlined />}
              onClick={() => navigate(`/admin/club/${clubId}/purchases/${detail.id}`)}
            >
              Открыть полностью
            </Button>
          </div>
        )
      }
    >
      {loading && (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      )}

      {!loading && detail && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* Статус + дата */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {ps && <StatusBadge label={ps.label} variant={ps.variant} />}
            <span style={{ fontSize: 13, color: tokens.colors.textMuted }}>
              {dayjs(detail.createdAt).format('DD.MM.YYYY HH:mm')}
            </span>
          </div>

          {/* Пользователь */}
          <Descriptions size="small" column={1} bordered styles={{ label: { width: 140 } }}>
            <Descriptions.Item label="Пользователь">
              {detail.userPhone ?? `#${detail.userId}`}
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

          {/* Бронирование */}
          {detail.booking && (
            <div>
              <div
                style={{
                  fontSize: 12,
                  fontWeight: 600,
                  color: tokens.colors.textMuted,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  marginBottom: 8,
                }}
              >
                Бронирование #{detail.booking.bookingId}
              </div>
              <div
                style={{
                  background: tokens.colors.surfaceAlt,
                  borderRadius: tokens.radius.md,
                  padding: '12px 14px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 6,
                  fontSize: 13,
                }}
              >
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <Tag color={BOOKING_STATUS[detail.booking.status].tagColor} style={{ margin: 0 }}>
                    {BOOKING_STATUS[detail.booking.status].label}
                  </Tag>
                  <span style={{ color: tokens.colors.textMuted }}>
                    {detail.booking.durationHours.toFixed(1)} ч. · {detail.booking.rateRubPerHour} ₽/ч.
                  </span>
                </div>
                <div style={{ color: tokens.colors.textSecondary }}>
                  {dayjs(detail.booking.startAt).format('DD.MM HH:mm')} → {dayjs(detail.booking.endAt).format('DD.MM HH:mm')}
                </div>
                {detail.booking.seats.length > 0 && (
                  <div style={{ color: tokens.colors.text }}>
                    Места: {detail.booking.seats.map((s) => s.label).join(', ')}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Товары */}
          {detail.productOrder && detail.productOrder.items.length > 0 && (
            <div>
              <div
                style={{
                  fontSize: 12,
                  fontWeight: 600,
                  color: tokens.colors.textMuted,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  marginBottom: 8,
                }}
              >
                Товары
              </div>
              <div
                style={{
                  background: tokens.colors.surfaceAlt,
                  borderRadius: tokens.radius.md,
                  overflow: 'hidden',
                }}
              >
                {detail.productOrder.items.map((item, i) => (
                  <div
                    key={i}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '8px 14px',
                      fontSize: 13,
                      borderBottom:
                        i < detail.productOrder!.items.length - 1
                          ? `1px solid ${tokens.colors.border}`
                          : 'none',
                    }}
                  >
                    <span style={{ color: tokens.colors.text }}>
                      {item.title}
                      {item.qty > 1 && (
                        <span style={{ color: tokens.colors.textMuted }}> × {item.qty}</span>
                      )}
                    </span>
                    <span style={{ color: tokens.colors.textSecondary, flexShrink: 0 }}>
                      {item.subtotalRub} ₽
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </Drawer>
  )
}

// --- Вспомогательные компоненты ---

function SeatDisplay({ labels }: { labels: string[] }) {
  if (labels.length === 0) return <span style={{ color: tokens.colors.textMuted }}>—</span>
  if (labels.length <= 3) return <span>{labels.join(', ')}</span>
  return <span>{labels.slice(0, 3).join(', ')} +{labels.length - 3}</span>
}

// --- Основной компонент ---

export default function ClubPurchasesPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const [purchases, setPurchases] = useState<AdminPurchaseResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | null>(null)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [drawerPurchaseId, setDrawerPurchaseId] = useState<number | null>(null)

  const fetchPurchases = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    try {
      const params: Record<string, string> = {}
      if (dateRange) {
        params.from = dateRange[0].format('YYYY-MM-DDTHH:mm:ss')
        params.to = dateRange[1].format('YYYY-MM-DDTHH:mm:ss')
      }
      if (statusFilter) params.status = statusFilter
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
  }, [clubId, dateRange, statusFilter])

  useEffect(() => {
    fetchPurchases()
  }, [fetchPurchases])

  const handleCancelFromTable = async (id: number) => {
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

  // после отмены из drawer — обновляем строку в таблице
  const handleCancelFromDrawer = (canceledId: number) => {
    setPurchases((prev) =>
      prev.map((p) => (p.id === canceledId ? { ...p, paymentStatus: 'CANCELED' } : p))
    )
  }

  const filteredPurchases = useMemo(() => {
    if (!searchText.trim()) return purchases
    const q = searchText.trim().toLowerCase()
    return purchases.filter(
      (p) =>
        p.id.toString().includes(q) ||
        (p.userPhone ?? '').toLowerCase().includes(q)
    )
  }, [purchases, searchText])

  const stats = useMemo(() => {
    const all = purchases
    return {
      total:        all.length,
      createdCount: all.filter((p) => p.paymentStatus === 'CREATED').length,
      paidCount:    all.filter((p) => p.paymentStatus === 'PAID').length,
      canceledCount:all.filter((p) => p.paymentStatus === 'CANCELED').length,
      totalSum:     all.filter((p) => p.paymentStatus === 'PAID').reduce((s, p) => s + p.totalAmountRub, 0),
    }
  }, [purchases])

  const columns: ColumnsType<AdminPurchaseResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
      sorter: (a, b) => Number(a.id) - Number(b.id),
    },
    {
      title: 'Дата',
      key: 'createdAt',
      width: 150,
      render: (_, r) => dayjs(r.createdAt).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: 'Пользователь',
      key: 'user',
      render: (_, r) => r.userPhone ?? `#${r.userId}`,
    },
    {
      title: 'Состав',
      key: 'content',
      render: (_, r) => (
        <div style={{ fontSize: 13 }}>
          {r.seatLabels?.length > 0 && (
            <div style={{ color: tokens.colors.text }}>
              <SeatDisplay labels={r.seatLabels} />
            </div>
          )}
          {r.productCount > 0 && (
            <div style={{ color: tokens.colors.textMuted }}>
              {r.productCount} товар{r.productCount > 1 ? 'а' : ''}
            </div>
          )}
          {(!r.seatLabels?.length && !r.productCount) && (
            <span style={{ color: tokens.colors.textMuted }}>—</span>
          )}
        </div>
      ),
    },
    {
      title: 'Сумма',
      key: 'total',
      width: 110,
      render: (_, r) => (
        <span style={{ fontWeight: 600 }}>{r.totalAmountRub.toLocaleString('ru-RU')} ₽</span>
      ),
    },
    {
      title: 'Статус',
      key: 'status',
      width: 160,
      render: (_, r) => {
        const s = PAYMENT_STATUS[r.paymentStatus] ?? { label: r.paymentStatus, variant: 'default' as const }
        return <StatusBadge label={s.label} variant={s.variant} />
      },
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" onClick={() => setDrawerPurchaseId(record.id)}>
            Просмотр
          </Button>
          {record.paymentStatus !== 'CANCELED' && (
            <Popconfirm
              title="Отменить покупку?"
              onConfirm={() => handleCancelFromTable(record.id)}
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
    <div>
      <PageHeader
        title="Покупки"
        subtitle="Список покупок клуба с фильтрацией по статусу и периоду"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchPurchases} loading={loading}>
            Обновить
          </Button>
        }
      />

      {/* KPI */}
      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col xs={12} sm={8} md={5}>
          <StatCard label="Всего" value={stats.total} />
        </Col>
        <Col xs={12} sm={8} md={5}>
          <StatCard
            label="Ожидают оплаты"
            value={stats.createdCount}
            icon={<ClockCircleOutlined />}
            accentColor={tokens.colors.warning}
          />
        </Col>
        <Col xs={12} sm={8} md={5}>
          <StatCard
            label="Оплачено"
            value={stats.paidCount}
            icon={<CheckCircleOutlined />}
            accentColor={tokens.colors.success}
          />
        </Col>
        <Col xs={12} sm={8} md={4}>
          <StatCard
            label="Отменено"
            value={stats.canceledCount}
            icon={<CloseCircleOutlined />}
            accentColor={tokens.colors.error}
          />
        </Col>
        <Col xs={24} sm={8} md={5}>
          <StatCard
            label="Сумма (оплачено)"
            value={`${stats.totalSum.toLocaleString('ru-RU')} ₽`}
            icon={<DollarOutlined />}
            accentColor={tokens.colors.success}
          />
        </Col>
      </Row>

      {/* Фильтры */}
      <SectionCard style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <QuickFilters active={statusFilter} onChange={setStatusFilter} />
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <Input.Search
              placeholder="ID или телефон"
              allowClear
              style={{ width: 200 }}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              size="small"
            />
            <DatePicker.RangePicker
              showTime
              size="small"
              value={dateRange}
              onChange={(v) => setDateRange(v as [Dayjs, Dayjs] | null)}
              format="DD.MM.YYYY HH:mm"
              allowClear
            />
            {(searchText || dateRange) && (
              <Button
                size="small"
                type="link"
                onClick={() => { setSearchText(''); setDateRange(null) }}
              >
                Сбросить
              </Button>
            )}
          </div>
        </div>
      </SectionCard>

      {/* Таблица */}
      <SectionCard noPadding>
        <Table
          columns={columns}
          dataSource={filteredPurchases}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          onRow={(record) => ({
            style: { cursor: 'pointer' },
            onClick: (e) => {
              // не открывать drawer при клике на кнопки действий
              const target = e.target as HTMLElement
              if (target.closest('button') || target.closest('.ant-popconfirm')) return
              setDrawerPurchaseId(record.id)
            },
          })}
          locale={{
            emptyText: (
              <EmptyState
                icon={<DollarOutlined />}
                title="Покупок нет"
                description="Покупки клиентов будут отображаться здесь"
              />
            ),
          }}
        />
      </SectionCard>

      {/* Drawer быстрого просмотра */}
      {clubId && (
        <PurchaseDrawer
          purchaseId={drawerPurchaseId}
          clubId={clubId}
          onClose={() => setDrawerPurchaseId(null)}
          onCancel={handleCancelFromDrawer}
        />
      )}
    </div>
  )
}
