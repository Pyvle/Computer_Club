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
  List,
} from 'antd'
import type { FormInstance } from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import { useAuth } from '../../contexts/AuthContext'
import apiClient from '../../utils/apiClient'
import type { AddressSearchResult, ClubApplicationResponse } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import { tokens } from '../../theme/tokens'

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

function ApplicationFields({
  form,
  resetToken,
}: {
  form: FormInstance<ApplicationForm>
  resetToken: string | boolean
}) {
  const [searching, setSearching] = useState(false)
  const [searchResults, setSearchResults] = useState<AddressSearchResult[]>([])
  const [searchError, setSearchError] = useState<string | null>(null)
  const [confirmedAddress, setConfirmedAddress] = useState<AddressSearchResult | null>(null)

  useEffect(() => {
    setSearching(false)
    setSearchResults([])
    setSearchError(null)
    setConfirmedAddress(null)
  }, [resetToken])

  async function handleFindAddress() {
    const query = form.getFieldValue('address')
    if (!query?.trim()) return
    setSearching(true)
    setSearchError(null)
    setSearchResults([])
    try {
      const { data } = await apiClient.get<AddressSearchResult[]>('/admin/geo/search', {
        params: { query: query.trim() },
      })
      if (data.length === 0) {
        setSearchError('Ничего не найдено — уточните адрес')
      } else {
        setSearchResults(data)
      }
    } catch {
      setSearchError('Не удалось выполнить поиск адреса')
    } finally {
      setSearching(false)
    }
  }

  function handleSelectAddress(item: AddressSearchResult) {
    form.setFieldValue('address', item.addressShort)
    setConfirmedAddress(item)
    setSearchResults([])
    setSearchError(null)
  }

  return (
    <>
      <Form.Item name="clubName" label="Название клуба" rules={[{ required: true }]}>
        <Input />
      </Form.Item>

      <Form.Item
        name="address"
        label="Адрес"
        rules={[{ required: true }]}
        extra="Введите адрес, нажмите «Найти» и выберите подходящий вариант"
      >
        <Input
          placeholder="Москва, ул. Тверская, 1"
          suffix={
            <Button
              type="link"
              size="small"
              icon={<SearchOutlined />}
              loading={searching}
              onClick={handleFindAddress}
              style={{ padding: 0, height: 'auto' }}
            >
              Найти
            </Button>
          }
          onPressEnter={handleFindAddress}
          onChange={(e) => {
            if (confirmedAddress && e.target.value !== confirmedAddress.addressShort) {
              setConfirmedAddress(null)
            }
          }}
        />
      </Form.Item>

      {searchError && (
        <Alert type="warning" message={searchError} showIcon style={{ marginBottom: 16 }} />
      )}

      {searchResults.length > 0 && (
        <div
          style={{
            border: `1px solid ${tokens.colors.border}`,
            borderRadius: tokens.radius.md,
            overflow: 'hidden',
            marginBottom: 16,
          }}
        >
          <div
            style={{
              padding: '8px 12px',
              background: tokens.colors.surfaceAlt,
              borderBottom: `1px solid ${tokens.colors.border}`,
              fontSize: 12,
              fontWeight: 600,
              color: tokens.colors.textMuted,
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
            }}
          >
            Найдено вариантов: {searchResults.length}
          </div>

          <List
            dataSource={searchResults}
            renderItem={(item, idx) => (
              <List.Item
                style={{
                  padding: '10px 16px',
                  cursor: 'pointer',
                  borderBottom:
                    idx < searchResults.length - 1 ? `1px solid ${tokens.colors.border}` : 'none',
                }}
                onClick={() => handleSelectAddress(item)}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
                    {item.addressShort}
                  </div>
                  <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 2 }}>
                    {item.addressFull}
                  </div>
                </div>
              </List.Item>
            )}
          />
        </div>
      )}

      {confirmedAddress && (
        <div
          style={{
            background: tokens.colors.successSoft,
            border: `1px solid ${tokens.colors.success}40`,
            borderRadius: tokens.radius.md,
            padding: '12px 14px',
            marginBottom: 16,
            display: 'flex',
            gap: 10,
            alignItems: 'flex-start',
          }}
        >
          <CheckCircleOutlined
            style={{ color: tokens.colors.success, fontSize: 16, marginTop: 2, flexShrink: 0 }}
          />
          <div style={{ flex: 1 }}>
            <div
              style={{
                fontWeight: 600,
                fontSize: 14,
                color: tokens.colors.text,
                marginBottom: 4,
              }}
            >
              {confirmedAddress.addressShort}
            </div>
            <div
              style={{
                fontSize: 12,
                color: tokens.colors.textSecondary,
                marginBottom: 2,
              }}
            >
              {confirmedAddress.addressFull}
            </div>
            <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
              {confirmedAddress.latitude.toFixed(6)}, {confirmedAddress.longitude.toFixed(6)}
            </div>
          </div>
        </div>
      )}

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
      children:
        clubs.length === 0 ? (
          <Empty description="У вас нет опубликованных клубов" />
        ) : (
          <Row gutter={[16, 16]}>
            {clubs.map((c) => (
              <Col key={c.clubId} xs={24} sm={12} md={8}>
                <Card hoverable onClick={() => navigate(`/admin/club/${c.clubId}/catalog`)} title={c.clubName}>
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
      children:
        pendingApps.length === 0 ? (
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
      children:
        draftApps.length === 0 && rejectedApps.length === 0 ? (
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
      <PageHeader
        title="Мои клубы"
        subtitle="Клубы, которыми вы управляете, и заявки на открытие"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddModalOpen(true)}>
            Добавить
          </Button>
        }
      />

      {!hasAnything ? (
        <Empty
          description={
            <div style={{ textAlign: 'center' }}>
              <Paragraph>Добро пожаловать в панель управления!</Paragraph>
              <Paragraph type="secondary">
                Нажмите «Добавить», чтобы подать заявку на открытие клуба. После одобрения вы получите
                доступ к управлению клубом.
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
        destroyOnClose
        onCancel={() => {
          setAddModalOpen(false)
          form.resetFields()
        }}
      >
        <Form layout="vertical" form={form} onFinish={onCreateDraft} style={{ marginTop: 16 }}>
          <ApplicationFields form={form} resetToken={addModalOpen} />
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Создать черновик
          </Button>
        </Form>
      </Modal>

      <Modal
        open={editModalOpen}
        title={isRevision ? 'Редактировать и отправить повторно' : 'Редактировать черновик'}
        footer={null}
        destroyOnClose
        onCancel={() => {
          setEditModalOpen(false)
          setEditingApp(null)
          editForm.resetFields()
        }}
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
          <ApplicationFields form={editForm} resetToken={`${editingApp?.id ?? 'none'}-${editModalOpen}`} />
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {isRevision ? 'Отправить повторно' : 'Сохранить и отправить'}
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
