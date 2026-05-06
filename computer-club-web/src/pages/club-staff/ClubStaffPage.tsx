import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  Button, Modal, Form, Input, Select, Space,
  Switch, message, Alert, Tooltip, Popconfirm,
} from 'antd'
import {
  UserOutlined,
  TeamOutlined,
  SettingOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import { useAuth } from '../../contexts/AuthContext'
import type { ClubStaffView, ClubStaffPermissionsResponse, UserLookupResult } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import { tokens } from '../../theme/tokens'

// --- Права: группировка и описания ---

interface PermissionMeta {
  label: string
  description: string
}

const PERMISSION_META: Record<string, PermissionMeta> = {
  CLUB_ADMINS_MANAGE:     { label: 'Управление персоналом',   description: 'Добавление и удаление сотрудников, изменение их прав' },
  CLUB_CATALOG_MANAGE:    { label: 'Управление каталогом',    description: 'Добавление товаров, редактирование цен и доступности' },
  CLUB_SEATS_MANAGE:      { label: 'Управление местами',      description: 'Добавление, редактирование и архивирование мест' },
  CLUB_FLOORPLANS_MANAGE: { label: 'Управление схемами зала', description: 'Создание, публикация и архивирование схем' },
  CLUB_REPORTS_VIEW:      { label: 'Отчёты и дашборд',       description: 'Просмотр бронирований, покупок и статистики клуба' },
  CLUB_AUDIT_VIEW:        { label: 'Аудит-журнал',           description: 'Просмотр журнала действий сотрудников' },
  CLUB_USER_BLOCKS_MANAGE:{ label: 'Блокировки пользователей',description: 'Блокировка и разблокировка клиентов клуба' },
}

const PERMISSION_GROUPS: { title: string; keys: string[] }[] = [
  {
    title: 'Операции',
    keys: ['CLUB_REPORTS_VIEW', 'CLUB_AUDIT_VIEW'],
  },
  {
    title: 'Структура клуба',
    keys: ['CLUB_CATALOG_MANAGE', 'CLUB_SEATS_MANAGE', 'CLUB_FLOORPLANS_MANAGE'],
  },
  {
    title: 'Управление',
    keys: ['CLUB_ADMINS_MANAGE', 'CLUB_USER_BLOCKS_MANAGE'],
  },
]

// --- Модалка прав ---

interface PermissionsModalProps {
  clubId: number
  userId: number
  phone: string | null
  currentUserId: number | null
  open: boolean
  onClose: () => void
  onChanged: () => void
}

function PermissionsModal({ clubId, userId, phone, currentUserId, open, onClose, onChanged }: PermissionsModalProps) {
  const [data, setData] = useState<ClubStaffPermissionsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState<string | null>(null)

  const fetchPermissions = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apiClient.get<ClubStaffPermissionsResponse>(
        `/admin/clubs/${clubId}/staff/${userId}/permissions`
      )
      setData(res.data)
    } catch {
      message.error('Не удалось загрузить права')
    } finally {
      setLoading(false)
    }
  }, [clubId, userId])

  useEffect(() => {
    if (open) fetchPermissions()
  }, [open, fetchPermissions])

  const isSelf = currentUserId !== null && userId === currentUserId

  async function handleToggle(permission: string, newGranted: boolean) {
    if (!data) return
    setSaving(permission)
    try {
      const roleGranted = (data.rolePermissions as string[]).includes(permission)
      if (newGranted === roleGranted) {
        // совпадает с дефолтом роли — сбрасываем оверрайд
        await apiClient.delete(`/admin/clubs/${clubId}/staff/${userId}/permissions/${permission}`)
      } else {
        // отличается от дефолта — ставим явный оверрайд
        await apiClient.put(`/admin/clubs/${clubId}/staff/${userId}/permissions/${permission}`, { granted: newGranted })
      }
      await fetchPermissions()
      onChanged()
    } catch {
      message.error('Не удалось изменить право')
    } finally {
      setSaving(null)
    }
  }

  return (
    <Modal
      title={`Права доступа: ${phone ?? `ID ${userId}`}`}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Закрыть</Button>}
      width={560}
    >
      {loading && (
        <div style={{ padding: 24, textAlign: 'center', color: tokens.colors.textMuted }}>
          Загрузка...
        </div>
      )}
      {!loading && data && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {isSelf && (
            <Alert
              type="warning"
              showIcon
              message="Вы редактируете собственные права. Запрет «Управление персоналом» заблокирует вам доступ к этому экрану."
            />
          )}
          {PERMISSION_GROUPS.map((group) => (
            <div key={group.title}>
              {/* Заголовок группы */}
              <div
                style={{
                  fontSize: 11,
                  fontWeight: 700,
                  color: tokens.colors.textMuted,
                  textTransform: 'uppercase',
                  letterSpacing: '0.07em',
                  marginBottom: 8,
                }}
              >
                {group.title}
              </div>

              {/* Права в группе */}
              <div
                style={{
                  border: `1px solid ${tokens.colors.border}`,
                  borderRadius: tokens.radius.md,
                  overflow: 'hidden',
                }}
              >
                {group.keys.map((perm, idx) => {
                  const meta = PERMISSION_META[perm]
                  const isGranted = (data.effectivePermissions as string[]).includes(perm)
                  const hasOverride = data.overrides.some((o) => o.permission === perm)
                  const selfLockout = isSelf && perm === 'CLUB_ADMINS_MANAGE'

                  const row = (
                    <div
                      key={perm}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '10px 14px',
                        borderTop: idx > 0 ? `1px solid ${tokens.colors.border}` : 'none',
                        background: hasOverride ? `${tokens.colors.warning}08` : tokens.colors.surface,
                      }}
                    >
                      {/* Текст */}
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 14, fontWeight: 500, color: tokens.colors.text }}>
                          {meta?.label ?? perm}
                        </div>
                        {meta?.description && (
                          <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 1 }}>
                            {meta.description}
                          </div>
                        )}
                      </div>

                      {/* Бейдж переопределения */}
                      {hasOverride && (
                        <span
                          style={{
                            fontSize: 11,
                            fontWeight: 600,
                            padding: '2px 8px',
                            borderRadius: 10,
                            background: tokens.colors.warningSoft,
                            color: tokens.colors.warning,
                            whiteSpace: 'nowrap',
                            flexShrink: 0,
                          }}
                        >
                          изменено
                        </span>
                      )}

                      {/* Переключатель */}
                      <Switch
                        checked={isGranted}
                        size="small"
                        loading={saving === perm}
                        disabled={saving !== null || selfLockout}
                        onChange={(checked) => handleToggle(perm, checked)}
                        style={{ flexShrink: 0 }}
                      />
                    </div>
                  )

                  return selfLockout ? (
                    <Tooltip title="Нельзя снять у себя право на управление персоналом" key={perm}>
                      {row}
                    </Tooltip>
                  ) : row
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </Modal>
  )
}

// --- Карточка сотрудника ---

interface StaffCardProps {
  member: ClubStaffView
  isSelf: boolean
  canManage: boolean
  onOpenPermissions: () => void
  onDelete: () => void
}

function StaffCard({ member, isSelf, canManage, onOpenPermissions, onDelete }: StaffCardProps) {
  const isOwner = member.role === 'OWNER'

  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        padding: '14px 16px',
        display: 'flex',
        alignItems: 'center',
        gap: 14,
      }}
    >
      {/* Аватар */}
      <div
        style={{
          width: 44,
          height: 44,
          borderRadius: '50%',
          background: isOwner ? `${tokens.colors.warning}20` : tokens.colors.primarySoft,
          color: isOwner ? tokens.colors.warning : tokens.colors.primary,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 20,
          flexShrink: 0,
        }}
      >
        {isOwner ? <TeamOutlined /> : <UserOutlined />}
      </div>

      {/* Контент */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
            {member.phone ?? `ID ${member.userId}`}
          </span>
          {/* Бейдж роли */}
          <span
            style={{
              fontSize: 11,
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: 10,
              background: isOwner ? `${tokens.colors.warning}20` : tokens.colors.primarySoft,
              color: isOwner ? tokens.colors.warning : tokens.colors.primary,
            }}
          >
            {isOwner ? 'Владелец' : 'Администратор'}
          </span>
          {isSelf && (
            <span
              style={{
                fontSize: 11,
                padding: '2px 8px',
                borderRadius: 10,
                background: tokens.colors.surfaceAlt,
                color: tokens.colors.textMuted,
              }}
            >
              вы
            </span>
          )}
        </div>
        {member.addedAt && (
          <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 2 }}>
            Добавлен {dayjs(member.addedAt).format('DD.MM.YYYY')}
            {member.addedByPhone && ` · ${member.addedByPhone}`}
          </div>
        )}
      </div>

      {/* Действия */}
      {!isOwner && canManage && (
        <Space size={6} style={{ flexShrink: 0 }}>
          <Button
            size="small"
            icon={<SettingOutlined />}
            onClick={onOpenPermissions}
          >
            Права
          </Button>
          {isSelf ? (
            <Tooltip title="Нельзя удалить самого себя">
              <Button size="small" danger icon={<DeleteOutlined />} disabled />
            </Tooltip>
          ) : (
            <Popconfirm
              title="Удалить администратора?"
              okText="Удалить"
              cancelText="Отмена"
              okButtonProps={{ danger: true }}
              onConfirm={onDelete}
            >
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          )}
        </Space>
      )}
    </div>
  )
}

// --- Основной компонент ---

export default function ClubStaffPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const id = Number(clubId)
  const { user } = useAuth()

  const [staff, setStaff] = useState<ClubStaffView[]>([])
  const [loadingStaff, setLoadingStaff] = useState(false)
  const [canManage, setCanManage] = useState(false)
  const [phoneFilter, setPhoneFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState<string | null>(null)
  const [addOpen, setAddOpen] = useState(false)
  const [foundUser, setFoundUser] = useState<UserLookupResult | null>(null)
  const [lookupLoading, setLookupLoading] = useState(false)
  const [confirmLoading, setConfirmLoading] = useState(false)
  const [form] = Form.useForm<{ phone: string }>()

  const [permModal, setPermModal] = useState<{ userId: number; phone: string | null } | null>(null)

  useEffect(() => {
    if (!user) return
    const myClub = user.clubs.find((c) => c.clubId === id)
    if (myClub?.role === 'OWNER' || user.globalRole === 'GLOBAL_ADMIN') {
      setCanManage(true)
      return
    }
    apiClient
      .get<ClubStaffPermissionsResponse>(`/admin/clubs/${id}/staff/${user.userId}/permissions`)
      .then((res) => setCanManage(res.data.effectivePermissions.includes('CLUB_ADMINS_MANAGE')))
      .catch(() => setCanManage(false))
  }, [id, user])

  const fetchStaff = useCallback(async () => {
    setLoadingStaff(true)
    try {
      const res = await apiClient.get<ClubStaffView[]>(`/admin/clubs/${id}/admins`)
      setStaff(res.data)
    } catch {
      message.error('Не удалось загрузить персонал')
    } finally {
      setLoadingStaff(false)
    }
  }, [id])

  useEffect(() => { fetchStaff() }, [fetchStaff])

  function closeAddModal() {
    setAddOpen(false)
    setFoundUser(null)
    form.resetFields()
  }

  async function handleLookup() {
    let values: { phone: string }
    try { values = await form.validateFields() } catch { return }
    setLookupLoading(true)
    try {
      const res = await apiClient.get<UserLookupResult>(
        `/admin/clubs/${id}/admins/users/by-phone`,
        { params: { phone: values.phone.trim() } }
      )
      setFoundUser(res.data)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Пользователь с таким номером не найден. Сотрудник должен сначала зарегистрироваться.')
    } finally {
      setLookupLoading(false)
    }
  }

  async function handleConfirmAdd() {
    if (!foundUser) return
    setConfirmLoading(true)
    try {
      await apiClient.put(`/admin/clubs/${id}/admins/${foundUser.userId}`)
      message.success(`Администратор ${foundUser.phone ?? foundUser.userId} добавлен`)
      closeAddModal()
      fetchStaff()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Не удалось добавить администратора')
    } finally {
      setConfirmLoading(false)
    }
  }

  async function handleDelete(userId: number) {
    try {
      await apiClient.delete(`/admin/clubs/${id}/admins/${userId}`)
      message.success('Администратор удалён')
      fetchStaff()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Не удалось удалить администратора')
    }
  }

  const filteredStaff = staff.filter((s) => {
    const matchPhone = !phoneFilter || (s.phone ?? '').includes(phoneFilter.trim())
    const matchRole = !roleFilter || s.role === roleFilter
    return matchPhone && matchRole
  })

  return (
    <div>
      <PageHeader
        title="Персонал клуба"
        subtitle="Администраторы и их права доступа"
        extra={
          canManage && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>
              Добавить администратора
            </Button>
          )
        }
      />

      {/* Фильтры */}
      <SectionCard style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            placeholder="Поиск по телефону"
            allowClear
            style={{ width: 220 }}
            value={phoneFilter}
            onChange={(e) => setPhoneFilter(e.target.value)}
            size="small"
          />
          <Select
            placeholder="Роль"
            allowClear
            style={{ width: 160 }}
            size="small"
            value={roleFilter}
            onChange={(v) => setRoleFilter(v ?? null)}
            options={[
              { value: 'OWNER', label: 'Владелец' },
              { value: 'ADMIN', label: 'Администратор' },
            ]}
          />
        </Space>
      </SectionCard>

      {/* Список сотрудников */}
      <SectionCard>
        {loadingStaff ? (
          <div style={{ textAlign: 'center', padding: 32, color: tokens.colors.textMuted }}>
            Загрузка...
          </div>
        ) : filteredStaff.length === 0 ? (
          <EmptyState
            icon={<TeamOutlined />}
            title="Сотрудников нет"
            description="Добавьте администраторов клуба"
            actionLabel={canManage ? 'Добавить администратора' : undefined}
            onAction={canManage ? () => setAddOpen(true) : undefined}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {filteredStaff.map((member) => (
              <StaffCard
                key={member.userId}
                member={member}
                isSelf={user?.userId === member.userId}
                canManage={canManage}
                onOpenPermissions={() => setPermModal({ userId: member.userId, phone: member.phone })}
                onDelete={() => handleDelete(member.userId)}
              />
            ))}
          </div>
        )}
      </SectionCard>

      {/* Модалка добавления */}
      <Modal
        title="Добавить администратора"
        open={addOpen}
        onCancel={closeAddModal}
        footer={null}
      >
        {!foundUser ? (
          <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
            <Form.Item
              name="phone"
              label="Номер телефона"
              rules={[{ required: true, message: 'Введите номер телефона' }]}
            >
              <Input placeholder="+79991234567" />
            </Form.Item>
            <Button type="primary" block loading={lookupLoading} onClick={handleLookup}>
              Найти
            </Button>
          </Form>
        ) : (
          <div style={{ marginTop: 16 }}>
            <div
              style={{
                background: tokens.colors.surfaceAlt,
                borderRadius: tokens.radius.md,
                padding: '12px 16px',
                marginBottom: 16,
              }}
            >
              <div style={{ fontSize: 14, fontWeight: 600, color: tokens.colors.text, marginBottom: 4 }}>
                {foundUser.phone ?? `ID ${foundUser.userId}`}
              </div>
              <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
                ID: {foundUser.userId} · Роль: {foundUser.role}
              </div>
            </div>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => setFoundUser(null)}>Назад</Button>
              <Button type="primary" loading={confirmLoading} onClick={handleConfirmAdd}>
                Добавить
              </Button>
            </Space>
          </div>
        )}
      </Modal>

      {/* Модалка прав */}
      {permModal && (
        <PermissionsModal
          clubId={id}
          userId={permModal.userId}
          phone={permModal.phone}
          currentUserId={user?.userId ?? null}
          open={true}
          onClose={() => setPermModal(null)}
          onChanged={fetchStaff}
        />
      )}
    </div>
  )
}
