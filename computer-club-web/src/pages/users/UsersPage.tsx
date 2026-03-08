import { useEffect, useState } from 'react'
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import type { AdminUserResponse, CreateUserRequest, GlobalRole } from '../../types'

const ROLE_LABELS: Record<GlobalRole, string> = {
  USER: 'Пользователь',
  GLOBAL_ADMIN: 'Глобальный админ',
}

export default function UsersPage() {
  const { message } = App.useApp()

  const [users, setUsers] = useState<AdminUserResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)

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
        prev.map((u) => (u.id === roleTarget.id ? { ...u, globalRole: values.globalRole } : u)),
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

  const activeSwitch = (user: AdminUserResponse) => (
    <Switch
      checked={user.isActive}
      loading={togglingId === user.id}
      onChange={(checked) => handleToggleActive(user, checked)}
    />
  )

  const adminColumns: ColumnsType<AdminUserResponse> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: 'Телефон', dataIndex: 'phone', render: (v: string | null) => v ?? '—' },
    {
      title: 'Роль',
      dataIndex: 'globalRole',
      width: 180,
      render: (role: GlobalRole) => (
        <Tag color={role === 'GLOBAL_ADMIN' ? 'red' : 'default'}>{ROLE_LABELS[role]}</Tag>
      ),
    },
    {
      title: 'Активен',
      dataIndex: 'isActive',
      width: 100,
      render: (_, record) => activeSwitch(record),
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => new Date(v).toLocaleDateString('ru-RU'),
    },
    {
      title: '',
      key: 'actions',
      width: 180,
      render: (_, record) => (
        <Space>
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

  const appUserColumns: ColumnsType<AdminUserResponse> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: 'Телефон', dataIndex: 'phone', render: (v: string | null) => v ?? '—' },
    {
      title: 'Активен',
      dataIndex: 'isActive',
      width: 100,
      render: (_, record) => activeSwitch(record),
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => new Date(v).toLocaleDateString('ru-RU'),
    },
  ]

  const admins = users.filter((u) => u.hasPassword)
  const appUsers = users.filter((u) => !u.hasPassword)

  const adminTab = (
    <>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
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
      />
    </>
  )

  const appUserTab = (
    <Table
      rowKey="id"
      columns={appUserColumns}
      dataSource={appUsers}
      loading={loading}
      pagination={{ pageSize: 20 }}
      size="middle"
    />
  )

  return (
    <>
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        Пользователи
      </Typography.Title>

      <Tabs
        defaultActiveKey="admins"
        items={[
          { key: 'admins', label: 'Администраторы', children: adminTab },
          { key: 'app-users', label: 'Пользователи приложения', children: appUserTab },
        ]}
      />

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
            rules={[
              { required: true, message: 'Введите номер телефона' },
            ]}
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
                { label: ROLE_LABELS.USER, value: 'USER' },
                { label: ROLE_LABELS.GLOBAL_ADMIN, value: 'GLOBAL_ADMIN' },
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
                { label: ROLE_LABELS.USER, value: 'USER' },
                { label: ROLE_LABELS.GLOBAL_ADMIN, value: 'GLOBAL_ADMIN' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
