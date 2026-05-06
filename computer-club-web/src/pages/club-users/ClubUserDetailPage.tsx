import { useCallback, useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Alert, Button, Col, DatePicker, Empty, Form, Input, Modal, Popconfirm,
  Row, Space, Spin, Switch, Table, Tag, Tabs, App, Checkbox,
} from 'antd'
import {
  ArrowLeftOutlined, CalendarOutlined, CheckCircleOutlined, ClockCircleOutlined,
  EyeOutlined, RiseOutlined, ShoppingCartOutlined, StopOutlined, UserOutlined, WarningOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { ClubUserDetailResponse, ClubUserBookingItem, ClubUserPurchaseItem, UpsertClubUserBlockRequest } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import { tokens } from '../../theme/tokens'
import { BOOKING_STATUS, PAYMENT_STATUS } from '../../utils/statusMaps'
import type { BookingStatus, PaymentStatus } from '../../types'

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

const REPORT_STATUS: Record<string, { label: string; color: string }> = {
  NEW:         { label: 'Новая',    color: 'orange' },
  IN_PROGRESS: { label: 'В работе', color: 'blue' },
  RESOLVED:    { label: 'Решена',   color: 'green' },
}

// --- Модалка блокировки ---

interface BlockModalProps {
  clubId: number
  userId: number
  isBlocked: boolean
  currentReason: string | null
  currentBlockedUntil: string | null
  open: boolean
  onClose: () => void
  onDone: () => void
}

function BlockModal({ clubId, userId, isBlocked, currentReason, currentBlockedUntil, open, onClose, onDone }: BlockModalProps) {
  const { message } = App.useApp()
  const [indefinite, setIndefinite] = useState(true)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ reason: string; blockedUntil?: dayjs.Dayjs | null }>()

  useEffect(() => {
    if (!open) return
    setIndefinite(!currentBlockedUntil)
    form.setFieldsValue({ reason: currentReason ?? '' })
  }, [open, currentReason, currentBlockedUntil, form])

  async function handleSave() {
    let values: { reason: string }
    try { values = await form.validateFields() } catch { return }
    setSaving(true)
    try {
      const blockedUntil = indefinite
        ? null
        : (form.getFieldValue('blockedUntil') as dayjs.Dayjs | null)?.toISOString() ?? null
      const req: UpsertClubUserBlockRequest = { isBlocked: true, reason: values.reason || null, blockedUntil }
      await apiClient.put(`/admin/clubs/${clubId}/user-blocks/${userId}`, req)
      message.success(isBlocked ? 'Блокировка обновлена' : 'Пользователь заблокирован')
      onDone()
      onClose()
    } catch {
      message.error('Не удалось сохранить')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={isBlocked ? 'Изменить блокировку' : 'Заблокировать пользователя'}
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
            {isBlocked ? 'Сохранить' : 'Заблокировать'}
          </Button>
        </div>
      </Form>
    </Modal>
  )
}

// --- Основной компонент ---

export default function ClubUserDetailPage() {
  const { clubId, userId } = useParams<{ clubId: string; userId: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const cId = Number(clubId)
  const uId = Number(userId)

  const bookingColumns: ColumnsType<ClubUserBookingItem> = [
    {
      title: 'Дата',
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
      title: 'Длит., ч.',
      dataIndex: 'durationHours',
      align: 'right',
      render: (v: number) => v.toFixed(1),
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
      render: (v: number) => v > 0
        ? <span style={{ fontWeight: 600 }}>{v.toLocaleString('ru-RU')} ₽</span>
        : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: '',
      key: 'open',
      width: 90,
      render: (_, record) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/admin/club/${cId}/bookings/${record.bookingId}`)}
        >
          Открыть
        </Button>
      ),
    },
  ]

  const purchaseColumns: ColumnsType<ClubUserPurchaseItem> = [
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
      key: 'open',
      width: 90,
      render: (_, record) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/admin/club/${cId}/purchases/${record.purchaseId}`)}
        >
          Открыть
        </Button>
      ),
    },
  ]

  const [user, setUser] = useState<ClubUserDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [blockModalOpen, setBlockModalOpen] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setLoadError(false)
    apiClient.get<ClubUserDetailResponse>(`/admin/clubs/${cId}/users/${uId}`)
      .then(({ data }) => setUser(data))
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }, [cId, uId])

  useEffect(() => { load() }, [load])

  async function handleUnblock() {
    try {
      await apiClient.put(`/admin/clubs/${cId}/user-blocks/${uId}`, {
        isBlocked: false, reason: null, blockedUntil: null,
      } satisfies UpsertClubUserBlockRequest)
      message.success('Блокировка снята')
      load()
    } catch {
      message.error('Не удалось снять блокировку')
    }
  }

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />

  if (loadError || !user) return (
    <Alert
      type="error"
      message="Не удалось загрузить профиль пользователя"
      action={<Button size="small" onClick={load}>Повторить</Button>}
      style={{ maxWidth: 480 }}
    />
  )

  const blockLabel = user.isBlocked
    ? (user.blockedUntil ? 'Временно заблокирован' : 'Заблокирован')
    : 'Не заблокирован'
  const blockVariant = user.isBlocked ? (user.blockedUntil ? 'warning' : 'error') : 'success'

  return (
    <div style={{ maxWidth: 960 }}>
      {/* Назад */}
      <button
        onClick={() => navigate(`/admin/club/${cId}/users`)}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', gap: 6,
          color: tokens.colors.textSecondary, fontSize: 13,
          padding: '0 0 16px', fontWeight: 500,
        }}
      >
        <ArrowLeftOutlined />
        К пользователям клуба
      </button>

      {/* Заголовок */}
      <PageHeader
        title={user.phone ?? `Пользователь #${user.userId}`}
        subtitle={`ID: ${user.userId}`}
        extra={
          <Space>
            <StatusBadge label={blockLabel} variant={blockVariant} />
            {user.isBlocked ? (
              <Popconfirm
                title="Снять блокировку?"
                okText="Снять"
                cancelText="Отмена"
                onConfirm={handleUnblock}
              >
                <Button size="small" icon={<CheckCircleOutlined />}>Разблокировать</Button>
              </Popconfirm>
            ) : (
              <Button
                size="small"
                danger
                icon={<StopOutlined />}
                onClick={() => setBlockModalOpen(true)}
              >
                Заблокировать
              </Button>
            )}
            {user.isBlocked && (
              <Button size="small" onClick={() => setBlockModalOpen(true)}>
                Изменить
              </Button>
            )}
          </Space>
        }
      />

      {/* Вкладки */}
      <Tabs
        style={{ marginTop: 4 }}
        items={[

          // ─── Общая информация ─────────────────────────────────────────
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
                    <StatCard label="Покупок" value={user.purchasesCount} icon={<ShoppingCartOutlined />} accentColor={tokens.colors.info} />
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
                    <StatCard
                      label="Средний чек"
                      value={user.purchasesCount > 0 ? `${user.avgSpentRub.toLocaleString('ru-RU')} ₽` : '—'}
                      icon={<RiseOutlined />}
                      accentColor={tokens.colors.warning}
                    />
                  </Col>
                </Row>

                <SectionCard title="Аккаунт">
                  <InfoRow label="Телефон">{user.phone ?? '—'}</InfoRow>
                  <InfoRow label="Статус аккаунта">
                    <StatusBadge
                      label={user.isActive ? 'Активен' : 'Заблокирован на платформе'}
                      variant={user.isActive ? 'success' : 'error'}
                    />
                  </InfoRow>
                  <InfoRow label="Статус в клубе">
                    <StatusBadge label={blockLabel} variant={blockVariant} />
                  </InfoRow>
                  <InfoRow label="Первый визит">
                    {user.firstVisitAt ? dayjs(user.firstVisitAt).format('D MMMM YYYY') : '—'}
                  </InfoRow>
                  <InfoRow label="Последний визит">
                    {user.lastVisitAt ? dayjs(user.lastVisitAt).format('D MMMM YYYY') : '—'}
                  </InfoRow>
                </SectionCard>

                <SectionCard title="Метрики по клубу">
                  <InfoRow label="Всего бронирований">{user.bookingsCount}</InfoRow>
                  <InfoRow label="Отмены">{user.cancelledBookingsCount}</InfoRow>
                  <InfoRow label="Часов забронировано">{user.totalHoursBooked.toFixed(1)}</InfoRow>
                  <InfoRow label="Всего покупок">{user.purchasesCount}</InfoRow>
                  <InfoRow label="Сумма оплат">{user.totalSpentRub.toLocaleString('ru-RU')} ₽</InfoRow>
                  <InfoRow label="Средний чек">
                    {user.purchasesCount > 0 ? `${user.avgSpentRub.toLocaleString('ru-RU')} ₽` : '—'}
                  </InfoRow>
                  <InfoRow label="Любимый тип места">
                    {user.favoriteSeatType
                      ? <Tag color={user.favoriteSeatType === 'VIP' ? 'gold' : 'default'}>
                          {user.favoriteSeatType === 'VIP' ? 'VIP' : 'Стандарт'}
                        </Tag>
                      : '—'}
                  </InfoRow>
                </SectionCard>
              </div>
            ),
          },

          // ─── Бронирования ─────────────────────────────────────────────
          {
            key: 'bookings',
            label: `Бронирования (${user.bookingsCount})`,
            children: (
              <SectionCard noPadding>
                <Table
                  rowKey="bookingId"
                  columns={bookingColumns}
                  dataSource={user.recentBookings}
                  size="middle"
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  scroll={{ x: 600 }}
                  locale={{ emptyText: <Empty description="Бронирований нет" /> }}
                />
              </SectionCard>
            ),
          },

          // ─── Покупки ──────────────────────────────────────────────────
          {
            key: 'purchases',
            label: `Покупки (${user.purchasesCount})`,
            children: (
              <SectionCard noPadding>
                <Table
                  rowKey="purchaseId"
                  columns={purchaseColumns}
                  dataSource={user.recentPurchases}
                  size="middle"
                  pagination={{ pageSize: 20, showSizeChanger: false }}
                  locale={{ emptyText: <Empty description="Покупок нет" /> }}
                />
              </SectionCard>
            ),
          },

          // ─── Блокировки ───────────────────────────────────────────────
          {
            key: 'restrictions',
            label: (
              <span>
                Ограничения
                {user.isBlocked && (
                  <span style={{
                    marginLeft: 6, background: tokens.colors.error, color: '#fff',
                    fontSize: 11, fontWeight: 700, padding: '1px 6px', borderRadius: 10,
                  }}>!</span>
                )}
              </span>
            ),
            children: (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <SectionCard
                  title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <StopOutlined style={{ color: tokens.colors.error }} />
                      Блокировка в этом клубе
                    </span>
                  }
                  extra={
                    user.isBlocked ? (
                      <Space size={4}>
                        <Button size="small" onClick={() => setBlockModalOpen(true)}>
                          Изменить
                        </Button>
                        <Popconfirm title="Снять блокировку?" okText="Снять" cancelText="Отмена" onConfirm={handleUnblock}>
                          <Button size="small" icon={<CheckCircleOutlined />}>Разблокировать</Button>
                        </Popconfirm>
                      </Space>
                    ) : (
                      <Button size="small" danger icon={<StopOutlined />} onClick={() => setBlockModalOpen(true)}>
                        Заблокировать
                      </Button>
                    )
                  }
                >
                  {user.isBlocked ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                      <InfoRow label="Статус"><StatusBadge label={blockLabel} variant={blockVariant} /></InfoRow>
                      {user.blockReason && <InfoRow label="Причина">{user.blockReason}</InfoRow>}
                      {user.blockedAt && (
                        <InfoRow label="Дата блокировки">
                          {dayjs(user.blockedAt).format('D MMMM YYYY, HH:mm')}
                        </InfoRow>
                      )}
                      {user.blockedUntil && (
                        <InfoRow label="Действует до">
                          {dayjs(user.blockedUntil).format('D MMMM YYYY, HH:mm')}
                        </InfoRow>
                      )}
                      {!user.blockedUntil && <InfoRow label="Срок"><span style={{ color: tokens.colors.error }}>Бессрочно</span></InfoRow>}
                      {user.blockedByPhone && <InfoRow label="Кто заблокировал">{user.blockedByPhone}</InfoRow>}
                    </div>
                  ) : (
                    <Empty description="Пользователь не заблокирован" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  )}
                </SectionCard>

                {/* Жалобы */}
                <SectionCard
                  title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <WarningOutlined style={{ color: tokens.colors.warning }} />
                      Жалобы на этот клуб
                    </span>
                  }
                >
                  {user.reports.length === 0 ? (
                    <Empty description="Жалоб нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
                      {user.reports.map((r, idx) => {
                        const s = REPORT_STATUS[r.status] ?? { label: r.status, color: 'default' }
                        return (
                          <div
                            key={r.reportId}
                            style={{
                              padding: '10px 0',
                              borderBottom: idx < user.reports.length - 1
                                ? `1px solid ${tokens.colors.border}`
                                : 'none',
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                              <div style={{ fontSize: 13, color: tokens.colors.text, lineHeight: 1.5, flex: 1 }}>
                                {r.message}
                              </div>
                              <Tag color={s.color} style={{ flexShrink: 0 }}>{s.label}</Tag>
                            </div>
                            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
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
        ]}
      />

      <BlockModal
        clubId={cId}
        userId={uId}
        isBlocked={user.isBlocked}
        currentReason={user.blockReason}
        currentBlockedUntil={user.blockedUntil}
        open={blockModalOpen}
        onClose={() => setBlockModalOpen(false)}
        onDone={load}
      />
    </div>
  )
}
