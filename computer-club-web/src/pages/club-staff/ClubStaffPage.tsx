import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Tag, Popconfirm, Modal, Form, Input,
  Select, Typography, Space, Switch, message, Alert, Tooltip,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import { useAuth } from '../../contexts/AuthContext'
import type { ClubStaffView, ClubStaffPermissionsResponse, UserLookupResult } from '../../types'

const { Text } = Typography

const PERMISSION_LABELS: Record<string, string> = {
  CLUB_ADMINS_MANAGE: 'Управление персоналом',
  CLUB_CATALOG_MANAGE: 'Управление каталогом',
  CLUB_SEATS_MANAGE: 'Управление местами',
  CLUB_USER_BLOCKS_MANAGE: 'Блокировки пользователей',
  CLUB_FLOORPLANS_MANAGE: 'Управление схемами зала',
  CLUB_REPORTS_VIEW: 'Просмотр отчётов (брони, покупки)',
  CLUB_AUDIT_VIEW: 'Просмотр аудита',
}

const ALL_PERMISSIONS = Object.keys(PERMISSION_LABELS)

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
        await apiClient.put(`/admin/clubs/${clubId}/staff/${userId}/permissions/${permission}`, {
          granted: newGranted,
        })
      }
      await fetchPermissions()
      onChanged()
    } catch {
      message.error('Не удалось изменить право')
    } finally {
      setSaving(null)
    }
  }

  const columns: ColumnsType<{ key: string }> = [
    {
      title: 'Право',
      dataIndex: 'key',
      render: (p: string) => PERMISSION_LABELS[p] ?? p,
    },
    {
      title: 'По умолчанию',
      dataIndex: 'key',
      width: 120,
      render: (p: string) => {
        if (!data) return null
        const roleGranted = (data.rolePermissions as string[]).includes(p)
        return roleGranted
          ? <Tag color="green">Разрешено</Tag>
          : <Tag color="default">Запрещено</Tag>
      },
    },
    {
      title: 'Текущее',
      dataIndex: 'key',
      width: 180,
      render: (p: string) => {
        if (!data) return null
        const isGranted = (data.effectivePermissions as string[]).includes(p)
        const hasOverride = data.overrides.some(o => o.permission === p)
        // запрещаем самому себе снять CLUB_ADMINS_MANAGE — это заблокирует доступ
        const selfLockout = isSelf && p === 'CLUB_ADMINS_MANAGE'
        const toggle = (
          <Space size={8}>
            <Switch
              checked={isGranted}
              size="small"
              loading={saving === p}
              disabled={saving !== null || selfLockout}
              onChange={(checked) => handleToggle(p, checked)}
            />
            <Text style={{ fontSize: 13 }} type={isGranted ? undefined : 'secondary'}>
              {isGranted ? 'Разрешено' : 'Запрещено'}
            </Text>
            {hasOverride && (
              <Tag color="orange" style={{ fontSize: 11, marginLeft: 0 }}>изменено</Tag>
            )}
          </Space>
        )
        return selfLockout
          ? <Tooltip title="Нельзя снять у себя право на управление персоналом">{toggle}</Tooltip>
          : toggle
      },
    },
  ]

  return (
    <Modal
      title={`Права: ${phone ?? `ID ${userId}`}`}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Закрыть</Button>}
      width={620}
    >
      {loading && <Text type="secondary">Загрузка...</Text>}
      {data && (
        <>
          {isSelf && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message="Вы редактируете собственные права. Запрет «Управление персоналом» заблокирует вам доступ к этому экрану."
            />
          )}
          <Table
            dataSource={ALL_PERMISSIONS.map(p => ({ key: p }))}
            pagination={false}
            size="small"
            columns={columns}
          />
        </>
      )}
    </Modal>
  )
}

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
    const myClub = user.clubs.find(c => c.clubId === id)
    if (myClub?.role === 'OWNER' || user.globalRole === 'GLOBAL_ADMIN') {
      setCanManage(true)
      return
    }
    // для ADMIN — проверяем эффективные права
    apiClient
      .get<ClubStaffPermissionsResponse>(`/admin/clubs/${id}/staff/${user.userId}/permissions`)
      .then(res => setCanManage(res.data.effectivePermissions.includes('CLUB_ADMINS_MANAGE')))
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

  const filteredStaff = staff.filter(s => {
    const matchPhone = !phoneFilter || (s.phone ?? '').includes(phoneFilter.trim())
    const matchRole = !roleFilter || s.role === roleFilter
    return matchPhone && matchRole
  })

  const columns: ColumnsType<ClubStaffView> = [
    { title: 'ID', dataIndex: 'userId', width: 70 },
    {
      title: 'Телефон',
      dataIndex: 'phone',
      render: (v: string | null) => v ?? '—',
    },
    {
      title: 'Роль',
      dataIndex: 'role',
      width: 100,
      render: (role: string) => (
        <Tag color={role === 'OWNER' ? 'gold' : 'blue'}>{role}</Tag>
      ),
    },
    {
      title: 'Добавлен',
      dataIndex: 'addedAt',
      width: 160,
      render: (v: string | null) => {
        if (!v) return '—'
        const d = new Date(v)
        return d.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      },
    },
    {
      title: 'Кем добавлен',
      width: 160,
      render: (_: unknown, record: ClubStaffView) => {
        if (!record.addedByUserId) return '—'
        return record.addedByPhone ?? `ID ${record.addedByUserId}`
      },
    },
    {
      title: 'Действия',
      width: 220,
      render: (_: unknown, record: ClubStaffView) => {
        if (record.role === 'OWNER') return null
        const isSelf = user?.userId === record.userId
        return (
          <Space>
            {canManage && (
              <Button
                size="small"
                onClick={() => setPermModal({ userId: record.userId, phone: record.phone })}
              >
                Права
              </Button>
            )}
            {canManage && (
              isSelf ? (
                <Tooltip title="Нельзя удалить самого себя">
                  <Button size="small" danger disabled>Удалить</Button>
                </Tooltip>
              ) : (
                <Popconfirm
                  title="Удалить администратора?"
                  okText="Удалить"
                  cancelText="Отмена"
                  okButtonProps={{ danger: true }}
                  onConfirm={() => handleDelete(record.userId)}
                >
                  <Button size="small" danger>Удалить</Button>
                </Popconfirm>
              )
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>Персонал клуба</Typography.Title>
        {canManage && (
          <Button type="primary" onClick={() => setAddOpen(true)}>Добавить админа</Button>
        )}
      </div>

      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="Поиск по телефону"
          allowClear
          style={{ width: 220 }}
          value={phoneFilter}
          onChange={e => setPhoneFilter(e.target.value)}
        />
        <Select
          placeholder="Роль"
          allowClear
          style={{ width: 140 }}
          value={roleFilter}
          onChange={v => setRoleFilter(v ?? null)}
          options={[
            { value: 'OWNER', label: 'OWNER' },
            { value: 'ADMIN', label: 'ADMIN' },
          ]}
        />
      </Space>

      <Table
        rowKey="userId"
        dataSource={filteredStaff}
        columns={columns}
        loading={loadingStaff}
        pagination={false}
      />

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
            <p style={{ marginBottom: 4 }}>ID: <strong>{foundUser.userId}</strong></p>
            <p style={{ marginBottom: 4 }}>Телефон: <strong>{foundUser.phone ?? '—'}</strong></p>
            <p style={{ marginBottom: 0 }}>Роль: <strong>{foundUser.role}</strong></p>
            <Space style={{ width: '100%', justifyContent: 'flex-end', marginTop: 16 }}>
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
