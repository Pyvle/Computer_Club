import { useState, useEffect, useCallback, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Table, Button, Tag, Popconfirm, Modal, Form, Input, DatePicker,
  Checkbox, Space, App, Col, Row, Tooltip,
} from 'antd'
import {
  StopOutlined, CheckCircleOutlined, EyeOutlined, ReloadOutlined,
  UserOutlined, CalendarOutlined, ShoppingCartOutlined, RiseOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { ClubUserListItem, UpsertClubUserBlockRequest } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import { tokens } from '../../theme/tokens'

// --- Фильтры ---

type QuickFilter = 'all' | 'active' | 'blocked' | 'with_bookings' | 'with_purchases' | 'inactive' | 'with_cancels'

const FILTERS: { label: string; value: QuickFilter }[] = [
  { label: 'Все',              value: 'all' },
  { label: 'Активные',         value: 'active' },
  { label: 'Заблокированные',  value: 'blocked' },
  { label: 'С бронированиями', value: 'with_bookings' },
  { label: 'С покупками',      value: 'with_purchases' },
  { label: 'Давно не были',    value: 'inactive' },
  { label: 'С отменами',       value: 'with_cancels' },
]

function QuickFilters({ active, onChange }: { active: QuickFilter; onChange: (v: QuickFilter) => void }) {
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {FILTERS.map((f) => {
        const isActive = f.value === active
        return (
          <button
            key={f.value}
            onClick={() => onChange(f.value)}
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

// --- Бейдж блокировки ---

function BlockBadge({ item }: { item: ClubUserListItem }) {
  if (!item.isBlocked) {
    return <StatusBadge label="Не заблокирован" variant="success" />
  }
  if (item.blockedUntil) {
    return (
      <Tooltip title={`До: ${dayjs(item.blockedUntil).format('DD.MM.YYYY HH:mm')}`}>
        <span>
          <StatusBadge label="Временно заблокирован" variant="warning" />
        </span>
      </Tooltip>
    )
  }
  return <StatusBadge label="Заблокирован" variant="error" />
}

// --- Модалка блокировки ---

interface BlockModalProps {
  clubId: number
  target: ClubUserListItem | null
  open: boolean
  onClose: () => void
  onDone: () => void
}

function BlockModal({ clubId, target, open, onClose, onDone }: BlockModalProps) {
  const { message } = App.useApp()
  const [indefinite, setIndefinite] = useState(true)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ reason: string; blockedUntil?: dayjs.Dayjs | null }>()

  useEffect(() => {
    if (!open || !target) return
    setIndefinite(!target.blockedUntil)
    form.setFieldsValue({ reason: target.blockReason ?? '' })
  }, [open, target, form])

  async function handleSave() {
    let values: { reason: string }
    try { values = await form.validateFields() } catch { return }
    if (!target) return
    setSaving(true)
    try {
      const blockedUntil = indefinite
        ? null
        : (form.getFieldValue('blockedUntil') as dayjs.Dayjs | null)?.toISOString() ?? null
      const req: UpsertClubUserBlockRequest = {
        isBlocked: true,
        reason: values.reason || null,
        blockedUntil,
      }
      await apiClient.put(`/admin/clubs/${clubId}/user-blocks/${target.userId}`, req)
      message.success('Пользователь заблокирован')
      onDone()
      onClose()
    } catch {
      message.error('Не удалось заблокировать')
    } finally {
      setSaving(false)
    }
  }

  if (!target) return null
  return (
    <Modal
      title={`Заблокировать: ${target.phone ?? `ID ${target.userId}`}`}
      open={open}
      onCancel={onClose}
      footer={null}
      width={440}
      destroyOnClose
    >
      <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
        <Form.Item name="reason" label="Причина">
          <Input.TextArea rows={3} placeholder="Необязательно" />
        </Form.Item>
        <Form.Item>
          <Checkbox checked={indefinite} onChange={(e) => setIndefinite(e.target.checked)}>
            Бессрочно
          </Checkbox>
        </Form.Item>
        {!indefinite && (
          <Form.Item name="blockedUntil" label="Заблокирован до">
            <DatePicker
              showTime
              format="DD.MM.YYYY HH:mm"
              style={{ width: '100%' }}
              disabledDate={(d) => d.isBefore(dayjs(), 'day')}
            />
          </Form.Item>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button onClick={onClose}>Отмена</Button>
          <Button type="primary" danger loading={saving} onClick={handleSave}>
            Заблокировать
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

// --- Основная страница ---

export default function ClubUsersPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const id = Number(clubId)

  const [users, setUsers] = useState<ClubUserListItem[]>([])
  const [loading, setLoading] = useState(false)
  const [filter, setFilter] = useState<QuickFilter>('all')
  const [search, setSearch] = useState('')
  const [blockTarget, setBlockTarget] = useState<ClubUserListItem | null>(null)
  const [blockModalOpen, setBlockModalOpen] = useState(false)

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await apiClient.get<ClubUserListItem[]>(`/admin/clubs/${id}/users`)
      setUsers(data)
    } catch {
      message.error('Не удалось загрузить пользователей')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { fetchUsers() }, [fetchUsers])

  async function handleUnblock(user: ClubUserListItem) {
    try {
      await apiClient.put(`/admin/clubs/${id}/user-blocks/${user.userId}`, {
        isBlocked: false, reason: null, blockedUntil: null,
      } satisfies UpsertClubUserBlockRequest)
      message.success('Блокировка снята')
      fetchUsers()
    } catch {
      message.error('Не удалось снять блокировку')
    }
  }

  const stats = useMemo(() => ({
    total:    users.length,
    blocked:  users.filter((u) => u.isBlocked).length,
    frequent: users.filter((u) => u.bookingsCount >= 3).length,
    revenue:  users.reduce((sum, u) => sum + u.totalSpentRub, 0),
  }), [users])

  const filtered = useMemo(() => {
    let result = users

    switch (filter) {
      case 'active':        result = result.filter((u) => !u.isBlocked); break
      case 'blocked':       result = result.filter((u) => u.isBlocked); break
      case 'with_bookings': result = result.filter((u) => u.bookingsCount > 0); break
      case 'with_purchases':result = result.filter((u) => u.purchasesCount > 0); break
      case 'inactive':      result = result.filter((u) => {
        if (!u.lastVisitAt) return true
        return dayjs().diff(dayjs(u.lastVisitAt), 'day') > 30
      }); break
      case 'with_cancels':  result = result.filter((u) => u.cancelledBookingsCount > 0); break
    }

    if (search.trim()) {
      const q = search.trim().toLowerCase()
      result = result.filter((u) =>
        (u.phone ?? '').toLowerCase().includes(q) || String(u.userId).includes(q)
      )
    }

    return result
  }, [users, filter, search])

  const columns: ColumnsType<ClubUserListItem> = [
    {
      title: 'ID',
      dataIndex: 'userId',
      width: 60,
      sorter: (a, b) => Number(a.userId) - Number(b.userId),
    },
    {
      title: 'Телефон',
      dataIndex: 'phone',
      render: (v: string | null) => (
        <span style={{ fontWeight: 500 }}>{v ?? <span style={{ color: tokens.colors.textMuted }}>—</span>}</span>
      ),
    },
    {
      title: 'Первый визит',
      dataIndex: 'firstVisitAt',
      sorter: (a, b) => (a.firstVisitAt ?? '').localeCompare(b.firstVisitAt ?? ''),
      render: (v: string | null) => v ? dayjs(v).format('DD.MM.YYYY') : '—',
    },
    {
      title: 'Последний визит',
      dataIndex: 'lastVisitAt',
      defaultSortOrder: 'descend',
      sorter: (a, b) => (a.lastVisitAt ?? '').localeCompare(b.lastVisitAt ?? ''),
      render: (v: string | null) => {
        if (!v) return '—'
        const days = dayjs().diff(dayjs(v), 'day')
        const color = days > 30 ? tokens.colors.textMuted : tokens.colors.text
        return <span style={{ color }}>{dayjs(v).format('DD.MM.YYYY')}</span>
      },
    },
    {
      title: (
        <Tooltip title="Бронирований в этом клубе">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <CalendarOutlined /> Брони
          </span>
        </Tooltip>
      ),
      dataIndex: 'bookingsCount',
      align: 'right',
      sorter: (a, b) => a.bookingsCount - b.bookingsCount,
      render: (v: number) => <span style={{ fontWeight: 500 }}>{v > 0 ? v : <span style={{ color: tokens.colors.textMuted }}>0</span>}</span>,
    },
    {
      title: (
        <Tooltip title="Покупок в этом клубе">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <ShoppingCartOutlined /> Покупки
          </span>
        </Tooltip>
      ),
      dataIndex: 'purchasesCount',
      align: 'right',
      sorter: (a, b) => a.purchasesCount - b.purchasesCount,
      render: (v: number) => <span style={{ fontWeight: 500 }}>{v > 0 ? v : <span style={{ color: tokens.colors.textMuted }}>0</span>}</span>,
    },
    {
      title: (
        <Tooltip title="Сумма оплаченных покупок в клубе">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <RiseOutlined /> Оплачено
          </span>
        </Tooltip>
      ),
      dataIndex: 'totalSpentRub',
      align: 'right',
      sorter: (a, b) => a.totalSpentRub - b.totalSpentRub,
      render: (v: number) => (
        <span style={{ fontWeight: 600, color: v > 0 ? tokens.colors.success : tokens.colors.textMuted }}>
          {v > 0 ? `${v.toLocaleString('ru-RU')} ₽` : '—'}
        </span>
      ),
    },
    {
      title: 'Отмены',
      dataIndex: 'cancelledBookingsCount',
      align: 'right',
      sorter: (a, b) => a.cancelledBookingsCount - b.cancelledBookingsCount,
      render: (v: number) => v > 0
        ? <span style={{ color: tokens.colors.error, fontWeight: 500 }}>{v}</span>
        : <span style={{ color: tokens.colors.textMuted }}>0</span>,
    },
    {
      title: 'Статус',
      width: 170,
      render: (_, record) => <BlockBadge item={record} />,
      filters: [
        { text: 'Активные', value: 'active' },
        { text: 'Заблокированные', value: 'blocked' },
      ],
      onFilter: (value, record) =>
        value === 'active' ? !record.isBlocked : record.isBlocked,
    },
    {
      title: '',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/admin/club/${id}/users/${record.userId}`)}
          >
            Подробнее
          </Button>
          {record.isBlocked ? (
            <Popconfirm
              title="Снять блокировку?"
              okText="Снять"
              cancelText="Отмена"
              onConfirm={() => handleUnblock(record)}
            >
              <Button size="small" icon={<CheckCircleOutlined />}>
                Разблокировать
              </Button>
            </Popconfirm>
          ) : (
            <Button
              size="small"
              danger
              icon={<StopOutlined />}
              onClick={() => { setBlockTarget(record); setBlockModalOpen(true) }}
            >
              Заблокировать
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Пользователи клуба"
        subtitle="Все посетители с метриками по этому клубу"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchUsers} loading={loading}>
            Обновить
          </Button>
        }
      />

      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col xs={12} sm={6}>
          <StatCard label="Всего" value={stats.total} icon={<UserOutlined />} />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Заблокированных"
            value={stats.blocked}
            icon={<StopOutlined />}
            accentColor={stats.blocked > 0 ? tokens.colors.error : tokens.colors.textMuted}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Частых клиентов (3+)"
            value={stats.frequent}
            icon={<CalendarOutlined />}
            accentColor={tokens.colors.success}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Выручка от посетителей"
            value={`${stats.revenue.toLocaleString('ru-RU')} ₽`}
            icon={<RiseOutlined />}
            accentColor={tokens.colors.primary}
          />
        </Col>
      </Row>

      <SectionCard style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <QuickFilters active={filter} onChange={setFilter} />
          <Input.Search
            placeholder="Поиск по телефону или ID"
            allowClear
            style={{ maxWidth: 280 }}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            size="small"
          />
        </div>
      </SectionCard>

      <SectionCard noPadding>
        <Table
          rowKey="userId"
          columns={columns}
          dataSource={filtered}
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          size="middle"
          scroll={{ x: 900 }}
          onRow={(record) => ({
            style: { cursor: 'pointer' },
          })}
        />
      </SectionCard>

      <BlockModal
        clubId={id}
        target={blockTarget}
        open={blockModalOpen}
        onClose={() => { setBlockModalOpen(false); setBlockTarget(null) }}
        onDone={fetchUsers}
      />
    </div>
  )
}
