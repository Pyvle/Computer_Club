import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  App, Button, Col, Form, Input, Modal,
  Popconfirm, Row, Select, Space, Switch, Table, Tabs, Tag, Tooltip,
} from 'antd'
import {
  PlusOutlined, DeleteOutlined, ReloadOutlined,
  UserOutlined, TeamOutlined, SafetyCertificateOutlined, CheckCircleOutlined,
  EyeOutlined, CalendarOutlined, ShopOutlined, RiseOutlined, EnvironmentOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { AdminUserResponse, CreateUserRequest, GlobalRole } from '../../types'
import { tokens } from '../../theme/tokens'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'

// --- Роли ---

const ROLE_META: Record<GlobalRole, { label: string; variant: 'error' | 'default' }> = {
  GLOBAL_ADMIN: { label: 'Менеджер платформы', variant: 'error' },
  USER:         { label: 'Пользователь',       variant: 'default' },
}

// --- Quick-filter по активности ---

type ActiveFilter = 'all' | 'active' | 'inactive'

function ActiveQuickFilter({ value, onChange }: { value: ActiveFilter; onChange: (v: ActiveFilter) => void }) {
  const opts: { label: string; value: ActiveFilter }[] = [
    { label: 'Все',         value: 'all' },
    { label: 'Активные',    value: 'active' },
    { label: 'Заблокированные', value: 'inactive' },
  ]
  return (
    <div style={{ display: 'flex', gap: 6 }}>
      {opts.map((o) => {
        const isSelected = o.value === value
        return (
          <button
            key={o.value}
            onClick={() => onChange(o.value)}
            style={{
              padding: '4px 12px',
              borderRadius: 20,
              fontSize: 13,
              fontWeight: isSelected ? 600 : 400,
              cursor: 'pointer',
              border: `1px solid ${isSelected ? tokens.colors.primary : tokens.colors.border}`,
              background: isSelected ? tokens.colors.primarySoft : tokens.colors.surface,
              color: isSelected ? tokens.colors.primary : tokens.colors.textSecondary,
              transition: 'all 0.15s',
              outline: 'none',
            }}
          >
            {o.label}
          </button>
        )
      })}
    </div>
  )
}

// --- Основной компонент ---

export default function UsersPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()

  const [users, setUsers] = useState<AdminUserResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)

  // фильтры — общие для обоих табов
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('all')

  // --- Модал создания ---
  const [createOpen, setCreateOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createForm] = Form.useForm()

  // --- Модал смены роли ---
  const [roleModalOpen, setRoleModalOpen] = useState(false)
  const [roleTarget, setRoleTarget] = useState<AdminUserResponse | null>(null)
  const [roleSubmitting, setRoleSubmitting] = useState(false)
  const [roleForm] = Form.useForm()

  async function fetchUsers() {
    setLoading(true)
    try {
      const res = await apiClient.get<AdminUserResponse[]>('/admin/global/users')
      setUsers(res.data)
    } catch {
      message.error('Не удалось загрузить пользователей')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchUsers() }, [])

  async function handleToggleActive(user: AdminUserResponse, checked: boolean) {
    setTogglingId(user.id)
    try {
      await apiClient.put(`/admin/global/users/${user.id}/active`, { isActive: checked })
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, isActive: checked } : u)))
    } catch {
      message.error('Не удалось изменить статус')
    } finally {
      setTogglingId(null)
    }
  }

  async function handleDelete(userId: number) {
    try {
      await apiClient.delete(`/admin/global/users/${userId}`)
      message.success('Пользователь удалён')
      setUsers((prev) => prev.filter((u) => u.id !== userId))
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Не удалось удалить пользователя')
    }
  }

  function openRoleModal(user: AdminUserResponse) {
    setRoleTarget(user)
    roleForm.setFieldsValue({ globalRole: user.globalRole })
    setRoleModalOpen(true)
  }

  async function handleRoleSubmit(values: { globalRole: GlobalRole }) {
    if (!roleTarget) return
    setRoleSubmitting(true)
    try {
      await apiClient.put(`/admin/global/users/${roleTarget.id}/global-role`, { role: values.globalRole })
      message.success('Роль обновлена')
      setUsers((prev) =>
        prev.map((u) => (u.id === roleTarget.id ? { ...u, globalRole: values.globalRole } : u))
      )
      setRoleModalOpen(false)
    } catch {
      message.error('Не удалось сменить роль')
    } finally {
      setRoleSubmitting(false)
    }
  }

  function openCreateModal() {
    createForm.resetFields()
    createForm.setFieldsValue({ globalRole: 'USER' })
    setCreateOpen(true)
  }

  async function handleCreate(values: CreateUserRequest) {
    setCreating(true)
    try {
      await apiClient.post('/admin/global/users', values)
      message.success('Администратор создан')
      setCreateOpen(false)
      fetchUsers()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Не удалось создать пользователя')
    } finally {
      setCreating(false)
    }
  }

  // --- Статистика ---

  const stats = useMemo(() => ({
    total:    users.length,
    active:   users.filter((u) => u.isActive).length,
    admins:   users.filter((u) => u.globalRole === 'GLOBAL_ADMIN').length,
    appUsers: users.filter((u) => u.globalRole !== 'GLOBAL_ADMIN').length,
  }), [users])

  // --- Фильтрация ---

  function applyFilters(list: AdminUserResponse[]) {
    let result = list
    if (activeFilter === 'active')   result = result.filter((u) => u.isActive)
    if (activeFilter === 'inactive') result = result.filter((u) => !u.isActive)
    if (search.trim()) {
      const q = search.trim().toLowerCase()
      result = result.filter((u) => (u.phone ?? '').toLowerCase().includes(q) || String(u.id).includes(q))
    }
    return result
  }

  const admins   = useMemo(() => applyFilters(users.filter((u) => u.hasPassword)),              [users, activeFilter, search])
  const appUsers = useMemo(() => applyFilters(users.filter((u) => u.globalRole !== 'GLOBAL_ADMIN')), [users, activeFilter, search])

  // --- Переключатель активности ---

  function ActiveSwitch({ user }: { user: AdminUserResponse }) {
    return (
      <Space size={8}>
        <Switch
          checked={user.isActive}
          size="small"
          loading={togglingId === user.id}
          onChange={(checked) => handleToggleActive(user, checked)}
        />
        <StatusBadge
          label={user.isActive ? 'Активен' : 'Заблокирован'}
          variant={user.isActive ? 'success' : 'error'}
        />
      </Space>
    )
  }

  // --- Колонки администраторов ---

  const adminColumns: ColumnsType<AdminUserResponse> = [
    { title: 'ID', dataIndex: 'id', width: 70, sorter: (a, b) => Number(a.id) - Number(b.id) },
    {
      title: 'Телефон',
      dataIndex: 'phone',
      render: (v: string | null) => (
        <span style={{ fontWeight: 500 }}>{v ?? '—'}</span>
      ),
    },
    {
      title: 'Роль / Клуб',
      render: (_, record) => {
        if (record.globalRole === 'GLOBAL_ADMIN') {
          const m = ROLE_META['GLOBAL_ADMIN']
          return <StatusBadge label={m.label} variant={m.variant} />
        }
        if (record.clubRoles.length > 0) {
          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {record.clubRoles.map((cr) => (
                <div key={cr.clubId} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Tag
                    color={cr.role === 'OWNER' ? 'gold' : 'blue'}
                    style={{ margin: 0, flexShrink: 0 }}
                  >
                    {cr.role === 'OWNER' ? 'Владелец' : 'Персонал'}
                  </Tag>
                  <span style={{ fontSize: 13, color: tokens.colors.text }}>{cr.clubName}</span>
                </div>
              ))}
            </div>
          )
        }
        return <span style={{ color: tokens.colors.textMuted }}>—</span>
      },
    },
    {
      title: 'Статус',
      width: 175,
      render: (_, record) => <ActiveSwitch user={record} />,
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      width: 110,
      render: (v: string) => dayjs(v).format('DD.MM.YYYY'),
    },
    {
      title: '',
      key: 'actions',
      width: 130,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" onClick={() => openRoleModal(record)}>
            Роль
          </Button>
          <Popconfirm
            title="Удалить пользователя?"
            description="Это действие нельзя отменить."
            okText="Удалить"
            cancelText="Отмена"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record.id)}
          >
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // --- Колонки пользователей приложения ---

  const appUserColumns: ColumnsType<AdminUserResponse> = [
    { title: 'ID', dataIndex: 'id', width: 60, sorter: (a, b) => Number(a.id) - Number(b.id) },
    {
      title: 'Телефон',
      dataIndex: 'phone',
      render: (v: string | null) => (
        <span style={{ fontWeight: 500 }}>{v ?? '—'}</span>
      ),
    },
    {
      title: 'Статус',
      width: 160,
      render: (_, record) => <ActiveSwitch user={record} />,
    },
    {
      title: (
        <Tooltip title="Всего бронирований">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <CalendarOutlined />Брони
          </span>
        </Tooltip>
      ),
      dataIndex: 'bookingsCount',
      width: 80,
      align: 'right' as const,
      sorter: (a, b) => a.bookingsCount - b.bookingsCount,
      render: (v: number) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: (
        <Tooltip title="Всего покупок">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <ShopOutlined />Покупки
          </span>
        </Tooltip>
      ),
      dataIndex: 'purchasesCount',
      width: 90,
      align: 'right' as const,
      sorter: (a, b) => a.purchasesCount - b.purchasesCount,
      render: (v: number) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: (
        <Tooltip title="Посещённых клубов">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <EnvironmentOutlined />Клубы
          </span>
        </Tooltip>
      ),
      dataIndex: 'visitedClubsCount',
      width: 80,
      align: 'right' as const,
      sorter: (a: AdminUserResponse, b: AdminUserResponse) => a.visitedClubsCount - b.visitedClubsCount,
      render: (v: number) => <span style={{ fontWeight: 500 }}>{v > 0 ? v : <span style={{ color: tokens.colors.textMuted }}>—</span>}</span>,
    },
    {
      title: (
        <Tooltip title="Сумма оплаченных покупок">
          <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <RiseOutlined />Оплачено
          </span>
        </Tooltip>
      ),
      dataIndex: 'totalSpentRub',
      width: 110,
      align: 'right' as const,
      sorter: (a, b) => a.totalSpentRub - b.totalSpentRub,
      render: (v: number) => (
        <span style={{ fontWeight: 600, color: v > 0 ? tokens.colors.success : tokens.colors.textMuted }}>
          {v > 0 ? `${v.toLocaleString('ru-RU')} ₽` : '—'}
        </span>
      ),
    },
    {
      title: 'Последняя активность',
      dataIndex: 'lastActivityAt',
      width: 140,
      sorter: (a, b) => (a.lastActivityAt ?? '').localeCompare(b.lastActivityAt ?? ''),
      render: (v: string | null) =>
        v ? dayjs(v).format('DD.MM.YYYY HH:mm') : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: 'Регистрация',
      dataIndex: 'createdAt',
      width: 110,
      render: (v: string) => dayjs(v).format('DD.MM.YYYY'),
    },
    {
      title: '',
      key: 'details',
      width: 80,
      render: (_, record) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/admin/platform/users/${record.id}`)}
        >
          Детали
        </Button>
      ),
    },
  ]

  // --- Панель фильтров ---

  const FilterBar = (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
      <ActiveQuickFilter value={activeFilter} onChange={setActiveFilter} />
      <Input.Search
        placeholder="Поиск по телефону или ID"
        allowClear
        style={{ maxWidth: 280 }}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        size="small"
      />
    </div>
  )

  return (
    <>
      <PageHeader
        title="Пользователи"
        subtitle="Менеджеры платформы, персонал клубов и пользователи мобильного приложения"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchUsers} loading={loading}>
            Обновить
          </Button>
        }
      />

      {/* Статистика */}
      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col xs={12} sm={6}>
          <StatCard label="Всего" value={stats.total} icon={<UserOutlined />} />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Активных"
            value={stats.active}
            icon={<CheckCircleOutlined />}
            accentColor={tokens.colors.success}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Менеджеры платформы"
            value={stats.admins}
            icon={<SafetyCertificateOutlined />}
            accentColor={tokens.colors.error}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard
            label="Пользователи"
            value={stats.appUsers}
            icon={<TeamOutlined />}
            accentColor={tokens.colors.info}
          />
        </Col>
      </Row>

      {/* Таблицы */}
      <SectionCard>
        <Tabs
          defaultActiveKey="admins"
          items={[
            {
              key: 'admins',
              label: `Администраторы (${users.filter((u) => u.hasPassword).length})`,
              children: (
                <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 4, flexWrap: 'wrap', gap: 12 }}>
                    {FilterBar}
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
                      Создать администратора
                    </Button>
                  </div>
                  <Table
                    rowKey="id"
                    columns={adminColumns}
                    dataSource={admins}
                    loading={loading}
                    pagination={{ pageSize: 20 }}
                    size="middle"
                    locale={{
                      emptyText: (
                        <EmptyState
                          icon={<SafetyCertificateOutlined />}
                          title="Менеджеров платформы нет"
                          description={search || activeFilter !== 'all' ? 'Нет совпадений по фильтру' : 'Создайте первого менеджера платформы'}
                          actionLabel={!search && activeFilter === 'all' ? 'Создать менеджера' : undefined}
                          onAction={!search && activeFilter === 'all' ? openCreateModal : undefined}
                        />
                      ),
                    }}
                  />
                </>
              ),
            },
            {
              key: 'app-users',
              label: `Пользователи (${users.filter((u) => u.globalRole !== 'GLOBAL_ADMIN').length})`,
              children: (
                <>
                  {FilterBar}
                  <Table
                    rowKey="id"
                    columns={appUserColumns}
                    dataSource={appUsers}
                    loading={loading}
                    pagination={{ pageSize: 20 }}
                    size="middle"
                    locale={{
                      emptyText: (
                        <EmptyState
                          icon={<UserOutlined />}
                          title="Пользователей нет"
                          description={search || activeFilter !== 'all' ? 'Нет совпадений по фильтру' : 'Пользователи появятся после регистрации в приложении'}
                        />
                      ),
                    }}
                  />
                </>
              ),
            },
          ]}
        />
      </SectionCard>

      {/* Модал создания администратора */}
      <Modal
        open={createOpen}
        title="Новый администратор"
        onCancel={() => setCreateOpen(false)}
        onOk={() => createForm.submit()}
        okText="Создать"
        cancelText="Отмена"
        confirmLoading={creating}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate} style={{ marginTop: 16 }}>
          <Form.Item
            name="phone"
            label="Номер телефона"
            rules={[{ required: true, message: 'Введите номер телефона' }]}
          >
            <Input placeholder="+7XXXXXXXXXX" autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Пароль"
            rules={[
              { required: true, message: 'Введите пароль' },
              { min: 6, message: 'Минимум 6 символов' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="globalRole" label="Роль" rules={[{ required: true }]}>
            <Select
              options={[
                { label: ROLE_META.USER.label,         value: 'USER' },
                { label: ROLE_META.GLOBAL_ADMIN.label, value: 'GLOBAL_ADMIN' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Модал смены роли */}
      <Modal
        open={roleModalOpen}
        title={`Роль: ${roleTarget?.phone ?? `ID ${roleTarget?.id}`}`}
        onCancel={() => setRoleModalOpen(false)}
        onOk={() => roleForm.submit()}
        okText="Сохранить"
        cancelText="Отмена"
        confirmLoading={roleSubmitting}
        destroyOnClose
      >
        <Form form={roleForm} layout="vertical" onFinish={handleRoleSubmit} style={{ marginTop: 16 }}>
          <Form.Item name="globalRole" label="Роль" rules={[{ required: true }]}>
            <Select
              options={[
                { label: ROLE_META.USER.label,         value: 'USER' },
                { label: ROLE_META.GLOBAL_ADMIN.label, value: 'GLOBAL_ADMIN' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
