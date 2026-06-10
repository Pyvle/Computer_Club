import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert, Button, Col, Descriptions, Modal, Row, Spin, Switch, App, Tag, Space, Table, Tabs, Empty,
} from 'antd'
import {
  ArrowLeftOutlined,
  UserOutlined,
  CalendarOutlined,
  ShopOutlined,
  EnvironmentOutlined,
  ClockCircleOutlined,
  RiseOutlined,
  StopOutlined,
  TeamOutlined,
  EyeOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatusBadge from '../../components/ui/StatusBadge'
import StatCard from '../../components/ui/StatCard'
import { tokens } from '../../theme/tokens'
import { aggregateUserClubStats } from '../../utils/adminUsers'
import { BOOKING_STATUS, PAYMENT_STATUS } from '../../utils/statusMaps'
import type {
  AdminPurchaseDetailResponse,
  AdminPurchaseOrderItemDetail,
  AdminPurchaseSeatDetail,
  AdminUserDetailsResponse,
  BookingStatus,
  GlobalAdminUserBookingItem,
  GlobalAdminUserPurchaseItem,
  GlobalAdminUserReportItem,
  GlobalRole,
  PaymentStatus,
  UserClubRoleInfo,
} from '../../types'

// --- Константы ---

const ROLE_LABEL: Record<GlobalRole, string> = {
  GLOBAL_ADMIN: 'Менеджер платформы',
  USER: 'Пользователь приложения',
}

const CLUB_ROLE_META: Record<string, { label: string; color: string; variant: 'success' | 'default' | 'warning' | 'error' | 'info' }> = {
  OWNER: { label: 'Владелец', color: 'gold', variant: 'warning' },
  ADMIN: { label: 'Персонал', color: 'blue', variant: 'info' },
}

const REPORT_STATUS: Record<string, { label: string; color: string }> = {
  NEW:         { label: 'Новая',    color: 'orange' },
  IN_PROGRESS: { label: 'В работе', color: 'blue' },
  RESOLVED:    { label: 'Решена',   color: 'green' },
}

// --- Утилиты ---

function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
      padding: '9px 0', gap: 16,
      borderBottom: `1px solid ${tokens.colors.border}`,
    }}>
      <span style={{ fontSize: 13, color: tokens.colors.textSecondary, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 13, fontWeight: 500, color: tokens.colors.text, textAlign: 'right' }}>{children}</span>
    </div>
  )
}

const seatColumns: ColumnsType<AdminPurchaseSeatDetail> = [
  { title: 'Место', dataIndex: 'label' },
  {
    title: 'Тип',
    dataIndex: 'type',
    render: (t: string) => t === 'VIP' ? <Tag color="gold">VIP</Tag> : <Tag>Стандарт</Tag>,
  },
]

const productItemColumns: ColumnsType<AdminPurchaseOrderItemDetail> = [
  { title: 'Название', dataIndex: 'title' },
  { title: 'Кол-во', dataIndex: 'qty', width: 80 },
  { title: 'Цена', dataIndex: 'priceRub', width: 110, render: (v: number) => `${v} ₽` },
  { title: 'Итого', dataIndex: 'subtotalRub', width: 110, render: (v: number) => `${v} ₽` },
]

// --- Основной компонент ---

export default function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [user, setUser] = useState<AdminUserDetailsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [togglingActive, setTogglingActive] = useState(false)

  const [activeTab, setActiveTab] = useState('overview')

  const [allBookings, setAllBookings] = useState<GlobalAdminUserBookingItem[] | null>(null)
  const [allBookingsLoading, setAllBookingsLoading] = useState(false)

  const [allPurchases, setAllPurchases] = useState<GlobalAdminUserPurchaseItem[] | null>(null)
  const [allPurchasesLoading, setAllPurchasesLoading] = useState(false)

  const [userReports, setUserReports] = useState<GlobalAdminUserReportItem[] | null>(null)
  const [userReportsLoading, setUserReportsLoading] = useState(false)

  const [purchaseDetailOpen, setPurchaseDetailOpen] = useState(false)
  const [purchaseDetail, setPurchaseDetail] = useState<AdminPurchaseDetailResponse | null>(null)
  const [purchaseDetailLoading, setPurchaseDetailLoading] = useState(false)

  const id = Number(userId)

  const load = useCallback(() => {
    setLoading(true)
    setLoadError(false)
    apiClient.get<AdminUserDetailsResponse>(`/admin/global/users/${id}`)
      .then(({ data }) => setUser(data))
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => { load() }, [load])

  async function loadBookings() {
    if (allBookings !== null || allBookingsLoading) return
    setAllBookingsLoading(true)
    try {
      const { data } = await apiClient.get<GlobalAdminUserBookingItem[]>(`/admin/global/users/${id}/bookings`)
      setAllBookings(data)
    } catch {
      message.error('Не удалось загрузить бронирования')
      setAllBookings([])
    } finally {
      setAllBookingsLoading(false)
    }
  }

  async function loadPurchases() {
    if (allPurchases !== null || allPurchasesLoading) return
    setAllPurchasesLoading(true)
    try {
      const { data } = await apiClient.get<GlobalAdminUserPurchaseItem[]>(`/admin/global/users/${id}/purchases`)
      setAllPurchases(data)
    } catch {
      message.error('Не удалось загрузить покупки')
      setAllPurchases([])
    } finally {
      setAllPurchasesLoading(false)
    }
  }

  async function loadReports() {
    if (userReports !== null || userReportsLoading) return
    setUserReportsLoading(true)
    try {
      const { data } = await apiClient.get<GlobalAdminUserReportItem[]>(`/admin/global/users/${id}/reports`)
      setUserReports(data)
    } catch {
      message.error('Не удалось загрузить жалобы')
      setUserReports([])
    } finally {
      setUserReportsLoading(false)
    }
  }

  function handleTabChange(key: string) {
    setActiveTab(key)
    if (key === 'bookings') loadBookings()
    if (key === 'purchases') loadPurchases()
    if (key === 'clubs') { loadBookings(); loadPurchases() }
    if (key === 'restrictions') loadReports()
  }

  async function handleToggleActive(checked: boolean) {
    if (!user) return
    setTogglingActive(true)
    try {
      await apiClient.put(`/admin/global/users/${id}/active`, { isActive: checked })
      setUser((prev) => prev ? { ...prev, isActive: checked } : prev)
      message.success(checked ? 'Пользователь активирован' : 'Пользователь заблокирован')
    } catch {
      message.error('Не удалось изменить статус')
    } finally {
      setTogglingActive(false)
    }
  }

  async function openPurchaseDetail(purchaseId: number) {
    setPurchaseDetailOpen(true)
    setPurchaseDetail(null)
    setPurchaseDetailLoading(true)
    try {
      const { data } = await apiClient.get<AdminPurchaseDetailResponse>(`/admin/global/purchases/${purchaseId}`)
      setPurchaseDetail(data)
    } catch {
      message.error('Не удалось загрузить детали покупки')
    } finally {
      setPurchaseDetailLoading(false)
    }
  }

  // --- Статистика по клубам ---

  const clubStats = useMemo(
    () => aggregateUserClubStats(allBookings, allPurchases),
    [allBookings, allPurchases],
  )

  // --- Колонки таблиц ---

  const bookingColumns: ColumnsType<GlobalAdminUserBookingItem> = [
    {
      title: 'Клуб',
      dataIndex: 'clubName',
      render: (v: string) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Начало',
      dataIndex: 'startAt',
      defaultSortOrder: 'descend',
      sorter: (a, b) => a.startAt.localeCompare(b.startAt),
      render: (v: string) => dayjs(v).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: 'Конец',
      dataIndex: 'endAt',
      render: (v: string) => dayjs(v).format('HH:mm'),
    },
    {
      title: 'Места',
      dataIndex: 'seatLabels',
      render: (v: string[]) => v.length > 0 ? v.join(', ') : '—',
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      render: (v: string) => {
        const meta = BOOKING_STATUS[v as BookingStatus]
        return <Tag color={meta?.tagColor ?? 'default'}>{meta?.label ?? v}</Tag>
      },
    },
    {
      title: 'Сумма',
      dataIndex: 'totalRub',
      align: 'right',
      sorter: (a, b) => a.totalRub - b.totalRub,
      render: (v: number) => v > 0
        ? <span style={{ fontWeight: 600 }}>{v.toLocaleString('ru-RU')} ₽</span>
        : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
  ]

  const purchaseColumns: ColumnsType<GlobalAdminUserPurchaseItem> = [
    {
      title: 'Клуб',
      dataIndex: 'clubName',
      render: (v: string) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Дата',
      dataIndex: 'createdAt',
      defaultSortOrder: 'descend',
      sorter: (a, b) => a.createdAt.localeCompare(b.createdAt),
      render: (v: string) => dayjs(v).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: 'Итого',
      dataIndex: 'totalRub',
      align: 'right',
      sorter: (a, b) => a.totalRub - b.totalRub,
      render: (v: number) => <span style={{ fontWeight: 600 }}>{v.toLocaleString('ru-RU')} ₽</span>,
    },
    {
      title: 'Статус оплаты',
      dataIndex: 'paymentStatus',
      render: (v: string) => {
        const meta = PAYMENT_STATUS[v as PaymentStatus]
        return <Tag color={meta?.tagColor ?? 'default'}>{meta?.label ?? v}</Tag>
      },
    },
    {
      title: '',
      key: 'actions',
      width: 90,
      render: (_, record) => (
        <Button size="small" icon={<EyeOutlined />} onClick={() => openPurchaseDetail(record.purchaseId)}>
          Детали
        </Button>
      ),
    },
  ]

  const clubStatsColumns: ColumnsType<{ clubId: number; clubName: string; bookingsCount: number; paidRub: number; lastVisit: string }> = [
    {
      title: 'Клуб',
      dataIndex: 'clubName',
      render: (v: string) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Бронирований',
      dataIndex: 'bookingsCount',
      align: 'right',
      sorter: (a, b) => a.bookingsCount - b.bookingsCount,
      render: (v: number) => <span style={{ fontWeight: 600 }}>{v}</span>,
    },
    {
      title: 'Оплачено',
      dataIndex: 'paidRub',
      align: 'right',
      sorter: (a, b) => a.paidRub - b.paidRub,
      render: (v: number) => v > 0
        ? <span style={{ fontWeight: 600, color: tokens.colors.success }}>{v.toLocaleString('ru-RU')} ₽</span>
        : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: 'Последний визит',
      dataIndex: 'lastVisit',
      sorter: (a, b) => a.lastVisit.localeCompare(b.lastVisit),
      render: (v: string) => v ? dayjs(v).format('DD.MM.YYYY') : '—',
    },
  ]

  // --- Рендер ---

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />

  if (loadError || !user) return (
    <Alert
      type="error"
      message="Не удалось загрузить профиль пользователя"
      description="Проверьте соединение с сервером."
      action={<Button size="small" onClick={load}>Повторить</Button>}
      style={{ maxWidth: 480 }}
    />
  )

  const restrictionsCount = user.activeBlocks.length
  const rolesCount = user.clubRoles.length

  return (
    <div style={{ maxWidth: 960 }}>

      {/* Назад */}
      <button
        onClick={() => navigate('/admin/platform/users')}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', gap: 6,
          color: tokens.colors.textSecondary, fontSize: 13,
          padding: '0 0 16px', fontWeight: 500,
        }}
      >
        <ArrowLeftOutlined />
        К списку пользователей
      </button>

      {/* Заголовок */}
      <PageHeader
        title={user.phone ?? `Пользователь #${user.id}`}
        subtitle={`ID: ${user.id} · ${ROLE_LABEL[user.globalRole as GlobalRole] ?? user.globalRole}`}
        extra={
          <Space>
            <StatusBadge
              label={user.isActive ? 'Активен' : 'Заблокирован'}
              variant={user.isActive ? 'success' : 'error'}
            />
            <Switch
              checked={user.isActive}
              size="small"
              loading={togglingActive}
              onChange={handleToggleActive}
            />
          </Space>
        }
      />

      {/* Вкладки */}
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        style={{ marginTop: 4 }}
        items={[

          // ─── Обзор ───────────────────────────────────────────────────────
          {
            key: 'overview',
            label: 'Обзор',
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <Row gutter={[12, 12]}>
                  <Col xs={12} sm={6}>
                    <StatCard label="Бронирований" value={user.bookingsCount} icon={<CalendarOutlined />} accentColor={tokens.colors.primary} />
                  </Col>
                  <Col xs={12} sm={6}>
                    <StatCard label="Покупок" value={user.purchasesCount} icon={<ShopOutlined />} accentColor={tokens.colors.info} />
                  </Col>
                  <Col xs={12} sm={6}>
                    <StatCard
                      label="Оплачено"
                      value={`${user.totalSpentRub.toLocaleString('ru-RU')} ₽`}
                      icon={<RiseOutlined />}
                      accentColor={tokens.colors.success}
                    />
                  </Col>
                  <Col xs={12} sm={6}>
                    <StatCard label="Клубов посещено" value={user.visitedClubsCount} icon={<EnvironmentOutlined />} accentColor={tokens.colors.warning} />
                  </Col>
                </Row>

                <SectionCard>
                  <InfoRow label="Телефон">{user.phone ?? '—'}</InfoRow>
                  <InfoRow label="Статус">
                    <StatusBadge
                      label={user.isActive ? 'Активен' : 'Заблокирован'}
                      variant={user.isActive ? 'success' : 'error'}
                    />
                  </InfoRow>
                  <InfoRow label="Роль">
                    <StatusBadge
                      label={ROLE_LABEL[user.globalRole as GlobalRole] ?? user.globalRole}
                      variant={user.globalRole === 'GLOBAL_ADMIN' ? 'error' : 'default'}
                    />
                  </InfoRow>
                  <InfoRow label="Тип аккаунта">
                    {user.hasPassword ? 'Панель администратора' : 'Мобильное приложение'}
                  </InfoRow>
                  <InfoRow label="Дата регистрации">
                    {dayjs(user.createdAt).format('D MMMM YYYY, HH:mm')}
                  </InfoRow>
                  <InfoRow label="Последняя активность">
                    {user.lastActivityAt
                      ? dayjs(user.lastActivityAt).format('D MMMM YYYY, HH:mm')
                      : <span style={{ color: tokens.colors.textMuted }}>Нет активности</span>}
                  </InfoRow>
                </SectionCard>
              </div>
            ),
          },

          // ─── Бронирования ────────────────────────────────────────────────
          {
            key: 'bookings',
            label: `Бронирования${user.bookingsCount > 0 ? ` (${user.bookingsCount})` : ''}`,
            children: (
              <SectionCard noPadding>
                <Table
                  rowKey="bookingId"
                  columns={bookingColumns}
                  dataSource={allBookings ?? []}
                  loading={allBookingsLoading}
                  size="middle"
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  scroll={{ x: 700 }}
                  locale={{
                    emptyText: allBookingsLoading
                      ? ' '
                      : <Empty description="Бронирований нет" />,
                  }}
                />
              </SectionCard>
            ),
          },

          // ─── Покупки ─────────────────────────────────────────────────────
          {
            key: 'purchases',
            label: `Покупки${user.purchasesCount > 0 ? ` (${user.purchasesCount})` : ''}`,
            children: (
              <SectionCard noPadding>
                <Table
                  rowKey="purchaseId"
                  columns={purchaseColumns}
                  dataSource={allPurchases ?? []}
                  loading={allPurchasesLoading}
                  size="middle"
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  scroll={{ x: 650 }}
                  locale={{
                    emptyText: allPurchasesLoading
                      ? ' '
                      : <Empty description="Покупок нет" />,
                  }}
                />
              </SectionCard>
            ),
          },

          // ─── Клубы ───────────────────────────────────────────────────────
          {
            key: 'clubs',
            label: `Клубы${user.visitedClubsCount > 0 ? ` (${user.visitedClubsCount})` : ''}`,
            children: (
              <SectionCard noPadding>
                <Table
                  rowKey="clubId"
                  columns={clubStatsColumns}
                  dataSource={clubStats ?? []}
                  loading={allBookingsLoading || allPurchasesLoading}
                  size="middle"
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  locale={{
                    emptyText: (allBookingsLoading || allPurchasesLoading)
                      ? ' '
                      : <Empty description="Посещений нет" />,
                  }}
                />
              </SectionCard>
            ),
          },

          // ─── Ограничения ─────────────────────────────────────────────────
          {
            key: 'restrictions',
            label: (
              <span>
                Ограничения
                {restrictionsCount > 0 && (
                  <span style={{
                    marginLeft: 6,
                    background: tokens.colors.error,
                    color: '#fff',
                    fontSize: 11,
                    fontWeight: 700,
                    padding: '1px 6px',
                    borderRadius: 10,
                  }}>
                    {restrictionsCount}
                  </span>
                )}
              </span>
            ),
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

                {/* Блокировки */}
                <SectionCard
                  title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <StopOutlined style={{ color: tokens.colors.error }} />
                      Блокировки
                    </span>
                  }
                >
                  {user.activeBlocks.length === 0 ? (
                    <Empty description="Активных блокировок нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                      {user.activeBlocks.map((b, idx) => (
                        <div
                          key={b.clubId}
                          style={{
                            padding: '10px 0',
                            borderBottom: idx < user.activeBlocks.length - 1
                              ? `1px solid ${tokens.colors.border}`
                              : 'none',
                          }}
                        >
                          <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
                            {b.clubName}
                          </div>
                          {b.reason && (
                            <div style={{ fontSize: 13, color: tokens.colors.textSecondary, marginTop: 3 }}>
                              <span style={{ fontWeight: 500 }}>Причина: </span>{b.reason}
                            </div>
                          )}
                          <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 4, display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                              <ClockCircleOutlined />
                              Заблокирован: {dayjs(b.createdAt).format('D MMM YYYY')}
                            </span>
                            {b.blockedUntil ? (
                              <span>До: {dayjs(b.blockedUntil).format('D MMM YYYY HH:mm')}</span>
                            ) : (
                              <span style={{ color: tokens.colors.error, fontWeight: 500 }}>Бессрочно</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </SectionCard>

                {/* Жалобы */}
                <SectionCard
                  title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <WarningOutlined style={{ color: tokens.colors.warning }} />
                      Жалобы пользователя
                    </span>
                  }
                >
                  {userReportsLoading ? (
                    <div style={{ textAlign: 'center', padding: 24 }}>
                      <Spin />
                    </div>
                  ) : userReports === null ? (
                    <div style={{ color: tokens.colors.textMuted, fontSize: 13 }}>Загрузка...</div>
                  ) : userReports.length === 0 ? (
                    <Empty description="Жалоб нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                      {userReports.map((r, idx) => {
                        const statusMeta = REPORT_STATUS[r.status] ?? { label: r.status, color: 'default' }
                        return (
                          <div
                            key={r.reportId}
                            style={{
                              padding: '10px 0',
                              borderBottom: idx < userReports.length - 1
                                ? `1px solid ${tokens.colors.border}`
                                : 'none',
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                              <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 600, fontSize: 13, color: tokens.colors.text, marginBottom: 4 }}>
                                  {r.clubName}
                                </div>
                                <div style={{ fontSize: 13, color: tokens.colors.textSecondary, lineHeight: 1.5 }}>
                                  {r.message}
                                </div>
                              </div>
                              <Tag color={statusMeta.color} style={{ flexShrink: 0 }}>
                                {statusMeta.label}
                              </Tag>
                            </div>
                            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 6, display: 'flex', alignItems: 'center', gap: 4 }}>
                              <ClockCircleOutlined />
                              {dayjs(r.createdAt).format('D MMM YYYY, HH:mm')}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </SectionCard>
              </div>
            ),
          },

          // ─── Роли ────────────────────────────────────────────────────────
          {
            key: 'roles',
            label: (
              <span>
                Роли
                {rolesCount > 0 && (
                  <span style={{
                    marginLeft: 6,
                    background: tokens.colors.primarySoft,
                    color: tokens.colors.primary,
                    fontSize: 11,
                    fontWeight: 700,
                    padding: '1px 6px',
                    borderRadius: 10,
                  }}>
                    {rolesCount}
                  </span>
                )}
              </span>
            ),
            children: (
              <SectionCard
                title={
                  <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <TeamOutlined style={{ color: tokens.colors.primary }} />
                    Роли в клубах
                  </span>
                }
              >
                {user.clubRoles.length === 0 ? (
                  <Empty description="Ролей нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                    {user.clubRoles.map((cr: UserClubRoleInfo, idx: number) => {
                      const roleMeta = CLUB_ROLE_META[cr.role]
                      return (
                        <div
                          key={cr.clubId}
                          style={{
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            padding: '10px 0', gap: 12,
                            borderBottom: idx < user.clubRoles.length - 1
                              ? `1px solid ${tokens.colors.border}`
                              : 'none',
                          }}
                        >
                          <div>
                            <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
                              {cr.clubName}
                            </div>
                            <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 2 }}>
                              ID: {cr.clubId}
                            </div>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            {cr.role === 'OWNER' && (
                              <span style={{ fontSize: 12, color: tokens.colors.textMuted }}>
                                Является владельцем
                              </span>
                            )}
                            {cr.role === 'ADMIN' && (
                              <span style={{ fontSize: 12, color: tokens.colors.textMuted }}>
                                Является администратором
                              </span>
                            )}
                            <Tag color={roleMeta?.color ?? 'default'}>
                              {roleMeta?.label ?? cr.role}
                            </Tag>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </SectionCard>
            ),
          },
        ]}
      />

      {/* Модал деталей покупки */}
      <Modal
        open={purchaseDetailOpen}
        title={purchaseDetail ? `Покупка #${purchaseDetail.id}` : 'Загрузка...'}
        onCancel={() => { setPurchaseDetailOpen(false); setPurchaseDetail(null) }}
        footer={null}
        width={720}
        destroyOnClose
      >
        {purchaseDetailLoading && (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <Spin size="large" />
          </div>
        )}
        {!purchaseDetailLoading && purchaseDetail && (() => {
          const ps = PAYMENT_STATUS[purchaseDetail.paymentStatus as PaymentStatus]
          return (
            <div>
              <SectionCard title="Основная информация" style={{ marginBottom: 16 }}>
                <Descriptions size="small" column={2}>
                  <Descriptions.Item label="Клуб">{purchaseDetail.clubId}</Descriptions.Item>
                  <Descriptions.Item label="Дата">
                    {dayjs(purchaseDetail.createdAt).format('DD.MM.YYYY HH:mm')}
                  </Descriptions.Item>
                  <Descriptions.Item label="Статус">
                    <Tag color={ps?.tagColor ?? 'default'}>{ps?.label ?? purchaseDetail.paymentStatus}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Итого">
                    <strong>{purchaseDetail.totalRub.toLocaleString('ru-RU')} ₽</strong>
                  </Descriptions.Item>
                  {purchaseDetail.bookingTotalRub > 0 && (
                    <Descriptions.Item label="Бронирование">
                      {purchaseDetail.bookingTotalRub} ₽
                    </Descriptions.Item>
                  )}
                  {purchaseDetail.productsTotalRub > 0 && (
                    <Descriptions.Item label="Товары">
                      {purchaseDetail.productsTotalRub} ₽
                    </Descriptions.Item>
                  )}
                </Descriptions>
              </SectionCard>

              {purchaseDetail.booking && (
                <SectionCard
                  title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      Бронирование #{purchaseDetail.booking.bookingId}
                      <Tag color={BOOKING_STATUS[purchaseDetail.booking.status]?.tagColor}>
                        {BOOKING_STATUS[purchaseDetail.booking.status]?.label}
                      </Tag>
                    </span>
                  }
                  style={{ marginBottom: 16 }}
                >
                  <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
                    <Descriptions.Item label="Начало">
                      {dayjs(purchaseDetail.booking.startAt).format('DD.MM.YYYY HH:mm')}
                    </Descriptions.Item>
                    <Descriptions.Item label="Конец">
                      {dayjs(purchaseDetail.booking.endAt).format('DD.MM.YYYY HH:mm')}
                    </Descriptions.Item>
                    <Descriptions.Item label="Длительность">
                      {purchaseDetail.booking.durationHours.toFixed(1)} ч.
                    </Descriptions.Item>
                    <Descriptions.Item label="Тариф">
                      {purchaseDetail.booking.rateRubPerHour} ₽/ч.
                    </Descriptions.Item>
                    <Descriptions.Item label="Сумма">
                      {purchaseDetail.booking.totalRub} ₽
                    </Descriptions.Item>
                  </Descriptions>
                  <Table
                    size="small"
                    columns={seatColumns}
                    dataSource={purchaseDetail.booking.seats}
                    rowKey="seatId"
                    pagination={false}
                  />
                </SectionCard>
              )}

              {purchaseDetail.productOrder && (
                <SectionCard title={`Заказ товаров #${purchaseDetail.productOrder.orderId}`}>
                  <Table
                    size="small"
                    columns={productItemColumns}
                    dataSource={purchaseDetail.productOrder.items}
                    rowKey="title"
                    pagination={false}
                    summary={() => (
                      <Table.Summary.Row>
                        <Table.Summary.Cell index={0} colSpan={3} align="right">
                          <strong>Итого:</strong>
                        </Table.Summary.Cell>
                        <Table.Summary.Cell index={1}>
                          <strong>{purchaseDetail.productOrder!.totalRub} ₽</strong>
                        </Table.Summary.Cell>
                      </Table.Summary.Row>
                    )}
                  />
                </SectionCard>
              )}
            </div>
          )
        })()}
      </Modal>
    </div>
  )
}
