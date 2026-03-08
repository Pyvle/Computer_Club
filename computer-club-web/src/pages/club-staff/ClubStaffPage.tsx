import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Tag, Popconfirm, Modal, Form, Input,
  Select, Typography, Space, Divider, message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import type { ClubStaffView, ClubStaffPermissionsResponse, UserLookupResult } from '../../types'

const { Text } = Typography

const PERMISSION_LABELS: Record<string, string> = {
  CLUB_ADMINS_MANAGE: 'Управление персоналом',
  CLUB_CATALOG_MANAGE: 'Управление каталогом',
  CLUB_SEATS_MANAGE: 'Управление местами',
  CLUB_USER_BLOCKS_MANAGE: 'Блокировки пользователей',
  CLUB_FLOORPLANS_MANAGE: 'Управление схемами зала',
  CLUB_REPORTS_VIEW: 'Просмотр отчётов',
}

const ALL_PERMISSIONS = Object.keys(PERMISSION_LABELS)

type PermissionState = 'role' | 'granted' | 'denied'

function getPermissionState(
  permission: string,
  overrides: ClubStaffPermissionsResponse['overrides'],
): PermissionState {
  const override = overrides.find(o => o.permission === permission)
  if (!override) return 'role'
  return override.granted ? 'granted' : 'denied'
}

interface PermissionsModalProps {
  clubId: number
  userId: number
  phone: string | null
  open: boolean
  onClose: () => void
  onChanged: () => void
}

function PermissionsModal({ clubId, userId, phone, open, onClose, onChanged }: PermissionsModalProps) {
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

  async function handleChange(permission: string, value: PermissionState) {
    setSaving(permission)
    try {
      if (value === 'role') {
        await apiClient.delete(`/admin/clubs/${clubId}/staff/${userId}/permissions/${permission}`)
      } else {
        await apiClient.put(`/admin/clubs/${clubId}/staff/${userId}/permissions/${permission}`, {
          granted: value === 'granted',
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

  return (
    <Modal
      title={`Права: ${phone ?? `ID ${userId}`}`}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Закрыть</Button>}
      width={560}
    >
      {loading && <Text type="secondary">Загрузка...</Text>}
      {data && (
        <>
          <Table
            dataSource={ALL_PERMISSIONS.map(p => ({ key: p, permission: p }))}
            pagination={false}
            size="small"
            columns={[
              {
                title: 'Право',
                dataIndex: 'permission',
                render: (p: string) => PERMISSION_LABELS[p] ?? p,
              },
              {
                title: 'Статус',
                dataIndex: 'permission',
                width: 200,
                render: (p: string) => (
                  <Select
                    value={getPermissionState(p, data.overrides)}
                    size="small"
                    style={{ width: 180 }}
                    loading={saving === p}
                    disabled={saving !== null}
                    onChange={(val: PermissionState) => handleChange(p, val)}
                    options={[
                      { value: 'role', label: 'По роли' },
                      { value: 'granted', label: 'Разрешено' },
                      { value: 'denied', label: 'Запрещено' },
                    ]}
                  />
                ),
              },
            ]}
          />
          <Divider style={{ margin: '12px 0' }} />
          <Text type="secondary" style={{ fontSize: 12 }}>
            Итоговые права:{' '}
            {data.effectivePermissions.length === 0
              ? 'нет'
              : data.effectivePermissions.map(p => PERMISSION_LABELS[p] ?? p).join(', ')}
          </Text>
        </>
      )}
    </Modal>
  )
}

export default function ClubStaffPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const id = Number(clubId)

  const [staff, setStaff] = useState<ClubStaffView[]>([])
  const [loadingStaff, setLoadingStaff] = useState(false)
  const [addOpen, setAddOpen] = useState(false)
  const [foundUser, setFoundUser] = useState<UserLookupResult | null>(null)
  const [lookupLoading, setLookupLoading] = useState(false)
  const [confirmLoading, setConfirmLoading] = useState(false)
  const [form] = Form.useForm<{ phone: string }>()

  const [permModal, setPermModal] = useState<{ userId: number; phone: string | null } | null>(null)

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
      title: 'Действия',
      width: 180,
      render: (_: unknown, record: ClubStaffView) => {
        if (record.role === 'OWNER') return null
        return (
          <Space>
            <Button
              size="small"
              onClick={() => setPermModal({ userId: record.userId, phone: record.phone })}
            >
              Права
            </Button>
            <Popconfirm
              title="Удалить администратора?"
              okText="Удалить"
              cancelText="Отмена"
              okButtonProps={{ danger: true }}
              onConfirm={() => handleDelete(record.userId)}
            >
              <Button size="small" danger>Удалить</Button>
            </Popconfirm>
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>Персонал клуба</Typography.Title>
        <Button type="primary" onClick={() => setAddOpen(true)}>Добавить админа</Button>
      </div>

      <Table
        rowKey="userId"
        dataSource={staff}
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
          open={true}
          onClose={() => setPermModal(null)}
          onChanged={fetchStaff}
        />
      )}
    </div>
  )
}
