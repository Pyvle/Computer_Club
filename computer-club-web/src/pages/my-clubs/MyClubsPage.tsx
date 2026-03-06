import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Tabs,
  Card,
  Button,
  Modal,
  Form,
  Input,
  Alert,
  Tag,
  Empty,
  Spin,
  Typography,
  App,
  Row,
  Col,
  Space,
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useAuth } from '../../contexts/AuthContext'
import apiClient from '../../utils/apiClient'
import { ClubApplicationResponse } from '../../types'

const { Paragraph } = Typography

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Черновик',
  PENDING: 'На рассмотрении',
  REVISION_REQUESTED: 'Требует доработки',
  APPROVED: 'Одобрена',
  REJECTED: 'Отклонена',
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'default',
  PENDING: 'processing',
  REVISION_REQUESTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

interface ApplicationForm {
  clubName: string
  address: string
  locationText?: string
  description?: string
}

function ApplicationFields() {
  return (
    <>
      <Form.Item name="clubName" label="Название клуба" rules={[{ required: true }]}>
        <Input />
      </Form.Item>
      <Form.Item name="address" label="Адрес" rules={[{ required: true }]}>
        <Input />
      </Form.Item>
      <Form.Item name="locationText" label="Ориентир (необязательно)">
        <Input placeholder="Например: возле метро Площадь Ленина" />
      </Form.Item>
      <Form.Item name="description" label="Описание (необязательно)">
        <Input.TextArea rows={3} />
      </Form.Item>
    </>
  )
}

export default function MyClubsPage() {
  const { user, loadContext } = useAuth()
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [applications, setApplications] = useState<ClubApplicationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editingApp, setEditingApp] = useState<ClubApplicationResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<ApplicationForm>()
  const [editForm] = Form.useForm<ApplicationForm>()

  async function fetchApplications() {
    try {
      const { data } = await apiClient.get<ClubApplicationResponse[]>('/club-applications/my')
      setApplications(data)
    } catch {
      message.error('Не удалось загрузить заявки')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    // обновляем контекст при каждом открытии страницы — чтобы user.clubs
    // отражал актуальное состояние (например, после одобрения заявки)
    loadContext().catch(() => {})
    fetchApplications()
  }, [])

  async function onCreateDraft(values: ApplicationForm) {
    setSubmitting(true)
    try {
      await apiClient.post('/club-applications/draft', values)
      message.success('Черновик создан')
      form.resetFields()
      setAddModalOpen(false)
      await fetchApplications()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка создания заявки')
    } finally {
      setSubmitting(false)
    }
  }

  async function onSubmit(appId: number) {
    try {
      await apiClient.post(`/club-applications/${appId}/submit`)
      message.success('Заявка отправлена на рассмотрение')
      await fetchApplications()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка отправки')
    }
  }

  async function onResubmit(values: ApplicationForm) {
    if (!editingApp) return
    setSubmitting(true)
    try {
      await apiClient.post(`/club-applications/${editingApp.id}/resubmit`, values)
      message.success('Заявка отправлена повторно')
      editForm.resetFields()
      setEditModalOpen(false)
      setEditingApp(null)
      await fetchApplications()
      await loadContext()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка отправки')
    } finally {
      setSubmitting(false)
    }
  }

  function openEdit(app: ClubApplicationResponse) {
    setEditingApp(app)
    editForm.setFieldsValue({
      clubName: app.clubName,
      address: app.address,
      locationText: app.locationText ?? undefined,
      description: app.description ?? undefined,
    })
    setEditModalOpen(true)
  }

  async function onUpdateAndSubmit(values: ApplicationForm) {
    if (!editingApp) return
    setSubmitting(true)
    try {
      await apiClient.put(`/club-applications/${editingApp.id}`, values)
      await apiClient.post(`/club-applications/${editingApp.id}/submit`)
      message.success('Заявка отправлена на рассмотрение')
      editForm.resetFields()
      setEditModalOpen(false)
      setEditingApp(null)
      await fetchApplications()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setSubmitting(false)
    }
  }

  const clubs = user?.clubs ?? []
  const pendingApps = applications.filter((a) => a.status === 'PENDING' || a.status === 'REVISION_REQUESTED')
  const draftApps = applications.filter((a) => a.status === 'DRAFT')
  const rejectedApps = applications.filter((a) => a.status === 'REJECTED')

  if (loading) return <Spin style={{ display: 'block', marginTop: 80 }} />

  const isRevision = editingApp?.status === 'REVISION_REQUESTED'
  const hasAnything = clubs.length > 0 || applications.length > 0

  const tabItems = [
    {
      key: 'clubs',
      label: `Опубликованные (${clubs.length})`,
      children: clubs.length === 0 ? (
        <Empty description="У вас нет опубликованных клубов" />
      ) : (
        <Row gutter={[16, 16]}>
          {clubs.map((c) => (
            <Col key={c.clubId} xs={24} sm={12} md={8}>
              <Card
                hoverable
                onClick={() => navigate(`/admin/club/${c.clubId}/catalog`)}
                title={c.clubName}
              >
                <Typography.Text type="secondary">{c.role}</Typography.Text>
              </Card>
            </Col>
          ))}
        </Row>
      ),
    },
    {
      key: 'pending',
      label: `На модерации (${pendingApps.length})`,
      children: pendingApps.length === 0 ? (
        <Empty description="Нет заявок на модерации" />
      ) : (
        <Row gutter={[16, 16]}>
          {pendingApps.map((app) => (
            <Col key={app.id} xs={24} sm={12} md={8}>
              <Card
                title={app.clubName}
                extra={<Tag color={STATUS_COLORS[app.status]}>{STATUS_LABELS[app.status]}</Tag>}
              >
                <Typography.Text type="secondary">{app.address}</Typography.Text>
                {app.status === 'REVISION_REQUESTED' && app.decisionComment && (
                  <Alert
                    type="warning"
                    message="Комментарий модератора"
                    description={app.decisionComment}
                    style={{ marginTop: 12 }}
                    showIcon
                  />
                )}
                {app.status === 'REVISION_REQUESTED' && (
                  <Button
                    type="primary"
                    size="small"
                    style={{ marginTop: 12 }}
                    onClick={() => openEdit(app)}
                  >
                    Редактировать и отправить повторно
                  </Button>
                )}
              </Card>
            </Col>
          ))}
        </Row>
      ),
    },
    {
      key: 'drafts',
      label: `Черновики (${draftApps.length + rejectedApps.length})`,
      children: draftApps.length === 0 && rejectedApps.length === 0 ? (
        <Empty description="Нет черновиков" />
      ) : (
        <Row gutter={[16, 16]}>
          {draftApps.map((app) => (
            <Col key={app.id} xs={24} sm={12} md={8}>
              <Card
                title={app.clubName}
                extra={<Tag color={STATUS_COLORS[app.status]}>{STATUS_LABELS[app.status]}</Tag>}
              >
                <Typography.Text type="secondary">{app.address}</Typography.Text>
                <Space style={{ marginTop: 12 }}>
                  <Button size="small" onClick={() => openEdit(app)}>
                    Редактировать
                  </Button>
                  <Button type="primary" size="small" onClick={() => onSubmit(app.id)}>
                    Отправить на модерацию
                  </Button>
                </Space>
              </Card>
            </Col>
          ))}
          {rejectedApps.map((app) => (
            <Col key={app.id} xs={24} sm={12} md={8}>
              <Card
                title={app.clubName}
                extra={<Tag color={STATUS_COLORS[app.status]}>{STATUS_LABELS[app.status]}</Tag>}
              >
                {app.decisionComment && (
                  <Alert
                    type="error"
                    message="Причина отклонения"
                    description={app.decisionComment}
                    style={{ marginBottom: 12 }}
                    showIcon
                  />
                )}
                <Button size="small" type="primary" onClick={() => setAddModalOpen(true)}>
                  Подать новую заявку
                </Button>
              </Card>
            </Col>
          ))}
        </Row>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>Мои клубы</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModalOpen(true)}>
          Добавить
        </Button>
      </div>

      {!hasAnything ? (
        <Empty
          description={
            <div style={{ textAlign: 'center' }}>
              <Paragraph>Добро пожаловать в панель управления!</Paragraph>
              <Paragraph type="secondary">
                Нажмите «Добавить», чтобы подать заявку на открытие клуба.
                После одобрения вы получите доступ к управлению клубом.
              </Paragraph>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModalOpen(true)}>
                Подать заявку
              </Button>
            </div>
          }
        />
      ) : (
        <Tabs items={tabItems} defaultActiveKey="clubs" />
      )}

      <Modal
        open={addModalOpen}
        title="Новая заявка на клуб"
        footer={null}
        onCancel={() => { setAddModalOpen(false); form.resetFields() }}
      >
        <Form layout="vertical" form={form} onFinish={onCreateDraft} style={{ marginTop: 16 }}>
          <ApplicationFields />
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Создать черновик
          </Button>
        </Form>
      </Modal>

      <Modal
        open={editModalOpen}
        title={isRevision ? 'Редактировать и отправить повторно' : 'Редактировать черновик'}
        footer={null}
        onCancel={() => { setEditModalOpen(false); setEditingApp(null); editForm.resetFields() }}
      >
        {isRevision && editingApp?.decisionComment && (
          <Alert
            type="warning"
            message="Комментарий модератора"
            description={editingApp.decisionComment}
            style={{ marginBottom: 16 }}
            showIcon
          />
        )}
        <Form layout="vertical" form={editForm} onFinish={isRevision ? onResubmit : onUpdateAndSubmit}>
          <ApplicationFields />
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {isRevision ? 'Отправить повторно' : 'Сохранить и отправить'}
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
