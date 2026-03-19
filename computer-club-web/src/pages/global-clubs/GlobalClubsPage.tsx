import { useEffect, useState } from 'react'
import {
  Table, Tag, Button, Modal, Form, Input, Popconfirm,
  Space, Typography, App, Descriptions, List, Divider, Image,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  StopOutlined, CheckCircleOutlined, WarningOutlined,
  DeleteOutlined, EyeOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { GlobalClubResponse, ClubWarningResponse } from '../../types'

const { Text } = Typography

export default function GlobalClubsPage() {
  const { message, modal } = App.useApp()
  const [clubs, setClubs] = useState<GlobalClubResponse[]>([])
  const [loading, setLoading] = useState(true)

  const [viewClub, setViewClub] = useState<GlobalClubResponse | null>(null)
  const [warnings, setWarnings] = useState<ClubWarningResponse[]>([])
  const [warningsLoading, setWarningsLoading] = useState(false)
  const [viewOpen, setViewOpen] = useState(false)

  const [blockModalOpen, setBlockModalOpen] = useState(false)
  const [blockTarget, setBlockTarget] = useState<GlobalClubResponse | null>(null)
  const [blockSubmitting, setBlockSubmitting] = useState(false)
  const [blockForm] = Form.useForm<{ reason: string }>()

  const [warnModalOpen, setWarnModalOpen] = useState(false)
  const [warnTarget, setWarnTarget] = useState<GlobalClubResponse | null>(null)
  const [warnSubmitting, setWarnSubmitting] = useState(false)
  const [warnForm] = Form.useForm<{ message: string }>()

  async function fetchClubs() {
    setLoading(true)
    try {
      const { data } = await apiClient.get<GlobalClubResponse[]>('/admin/global/clubs')
      setClubs(data)
    } catch {
      message.error('Не удалось загрузить клубы')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchClubs() }, [])

  async function openView(club: GlobalClubResponse) {
    setViewClub(club)
    setViewOpen(true)
    setWarningsLoading(true)
    try {
      const { data } = await apiClient.get<ClubWarningResponse[]>(`/admin/global/clubs/${club.id}/warnings`)
      setWarnings(data)
    } catch {
      setWarnings([])
    } finally {
      setWarningsLoading(false)
    }
  }

  function openBlock(club: GlobalClubResponse) {
    setBlockTarget(club)
    blockForm.resetFields()
    setBlockModalOpen(true)
  }

  async function onBlock(values: { reason: string }) {
    if (!blockTarget) return
    setBlockSubmitting(true)
    try {
      await apiClient.put(`/admin/global/clubs/${blockTarget.id}/block`, { reason: values.reason || null })
      message.success('Клуб заблокирован')
      setBlockModalOpen(false)
      await fetchClubs()
    } catch {
      message.error('Не удалось заблокировать')
    } finally {
      setBlockSubmitting(false)
    }
  }

  async function onUnblock(club: GlobalClubResponse) {
    try {
      await apiClient.put(`/admin/global/clubs/${club.id}/unblock`, {})
      message.success('Блокировка снята')
      await fetchClubs()
    } catch {
      message.error('Не удалось снять блокировку')
    }
  }

  async function onDelete(club: GlobalClubResponse) {
    try {
      await apiClient.delete(`/admin/global/clubs/${club.id}`)
      message.success('Клуб удалён')
      await fetchClubs()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка при удалении')
    }
  }

  function openWarn(club: GlobalClubResponse) {
    setWarnTarget(club)
    warnForm.resetFields()
    setWarnModalOpen(true)
  }

  async function onWarn(values: { message: string }) {
    if (!warnTarget) return
    setWarnSubmitting(true)
    try {
      await apiClient.post(`/admin/global/clubs/${warnTarget.id}/warnings`, { message: values.message })
      message.success('Предупреждение отправлено')
      setWarnModalOpen(false)
      // обновить warnings если клуб открыт в просмотре
      if (viewClub?.id === warnTarget.id) {
        const { data } = await apiClient.get<ClubWarningResponse[]>(`/admin/global/clubs/${warnTarget.id}/warnings`)
        setWarnings(data)
      }
    } catch {
      message.error('Не удалось отправить предупреждение')
    } finally {
      setWarnSubmitting(false)
    }
  }

  const columns: ColumnsType<GlobalClubResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: 'Название',
      dataIndex: 'name',
      render: (name, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{name}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.addressShort}</Text>
        </Space>
      ),
    },
    {
      title: 'Статус',
      width: 160,
      render: (_, record) => (
        <Space>
          {record.isBlocked
            ? <Tag color="red">Заблокирован</Tag>
            : record.isActive
              ? <Tag color="green">Активен</Tag>
              : <Tag color="default">Неактивен</Tag>
          }
        </Space>
      ),
    },
    {
      title: 'Действия',
      width: 220,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openView(record)}>
            Просмотр
          </Button>
          <Button size="small" icon={<WarningOutlined />} onClick={() => openWarn(record)}>
            Предупреждение
          </Button>
          {record.isBlocked ? (
            <Button size="small" icon={<CheckCircleOutlined />} onClick={() => onUnblock(record)}>
              Разблокировать
            </Button>
          ) : (
            <Button size="small" danger icon={<StopOutlined />} onClick={() => openBlock(record)}>
              Заблокировать
            </Button>
          )}
          <Popconfirm
            title="Удалить клуб?"
            description="Это действие необратимо. Все данные клуба будут удалены."
            okText="Удалить"
            okType="danger"
            cancelText="Отмена"
            onConfirm={() => onDelete(record)}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h2 style={{ marginTop: 0, marginBottom: 16 }}>Клубы</h2>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={clubs}
        loading={loading}
        pagination={{ pageSize: 20 }}
      />

      {/* Просмотр клуба */}
      <Modal
        open={viewOpen}
        title={viewClub?.name ?? 'Клуб'}
        footer={null}
        width={640}
        onCancel={() => setViewOpen(false)}
      >
        {viewClub && (
          <>
            {viewClub.imageUrl && (
              <Image
                src={viewClub.imageUrl.startsWith('/') ? `http://localhost:8080${viewClub.imageUrl}` : viewClub.imageUrl}
                style={{ maxHeight: 200, width: '100%', objectFit: 'cover', borderRadius: 8, marginBottom: 16 }}
              />
            )}
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="ID">{viewClub.id}</Descriptions.Item>
              <Descriptions.Item label="Название">{viewClub.name}</Descriptions.Item>
              <Descriptions.Item label="Адрес (кратко)">{viewClub.addressShort}</Descriptions.Item>
              <Descriptions.Item label="Адрес (полный)">{viewClub.addressFull}</Descriptions.Item>
              <Descriptions.Item label="Описание">{viewClub.description ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Активен">{viewClub.isActive ? 'Да' : 'Нет'}</Descriptions.Item>
              <Descriptions.Item label="Заблокирован">
                {viewClub.isBlocked
                  ? <><Tag color="red">Да</Tag> {viewClub.blockReason && <Text type="secondary">{viewClub.blockReason}</Text>}</>
                  : 'Нет'}
              </Descriptions.Item>
              <Descriptions.Item label="Создан">{dayjs(viewClub.createdAt).format('DD.MM.YYYY HH:mm')}</Descriptions.Item>
            </Descriptions>

            <Divider orientation="left">Предупреждения</Divider>
            <List
              loading={warningsLoading}
              dataSource={warnings}
              locale={{ emptyText: 'Предупреждений нет' }}
              renderItem={(w) => (
                <List.Item>
                  <List.Item.Meta
                    title={w.message}
                    description={dayjs(w.createdAt).format('DD.MM.YYYY HH:mm')}
                  />
                </List.Item>
              )}
            />
          </>
        )}
      </Modal>

      {/* Блокировка */}
      <Modal
        open={blockModalOpen}
        title={`Заблокировать: ${blockTarget?.name}`}
        footer={null}
        onCancel={() => setBlockModalOpen(false)}
      >
        <Form layout="vertical" form={blockForm} onFinish={onBlock}>
          <Form.Item name="reason" label="Причина блокировки (необязательно)">
            <Input.TextArea rows={3} placeholder="Нарушение правил платформы..." />
          </Form.Item>
          <Button type="primary" danger htmlType="submit" block loading={blockSubmitting}>
            Заблокировать
          </Button>
        </Form>
      </Modal>

      {/* Предупреждение */}
      <Modal
        open={warnModalOpen}
        title={`Предупреждение: ${warnTarget?.name}`}
        footer={null}
        onCancel={() => setWarnModalOpen(false)}
      >
        <Form layout="vertical" form={warnForm} onFinish={onWarn}>
          <Form.Item
            name="message"
            label="Текст предупреждения"
            rules={[{ required: true, message: 'Введите текст' }]}
          >
            <Input.TextArea rows={4} placeholder="Ваш клуб нарушает правила..." />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={warnSubmitting}>
            Отправить
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
