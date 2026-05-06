import { useEffect, useState } from 'react'
import {
  Table,
  Select,
  Tag,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Space,
  Typography,
  App,
  Tooltip,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import type {
  ClubApplicationResponse,
  ClubApplicationStatus,
  ClubApplicationDecisionRequest,
  ApproveClubApplicationResponse,
} from '../../types'

const STATUS_COLORS: Record<ClubApplicationStatus, string> = {
  DRAFT: 'default',
  PENDING: 'orange',
  REVISION_REQUESTED: 'gold',
  APPROVED: 'green',
  REJECTED: 'red',
}

const STATUS_LABELS: Record<ClubApplicationStatus, string> = {
  DRAFT: 'Черновик',
  PENDING: 'Ожидает',
  REVISION_REQUESTED: 'На доработке',
  APPROVED: 'Одобрена',
  REJECTED: 'Отклонена',
}

type ModalMode = 'approve' | 'reject' | 'revision'

interface DecisionForm {
  comment?: string
  ownerUserId?: number
}

export default function ClubApplicationsPage() {
  const { message } = App.useApp()

  const [items, setItems] = useState<ClubApplicationResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [statusFilter, setStatusFilter] = useState<ClubApplicationStatus | 'ALL'>('ALL')

  const [modalMode, setModalMode] = useState<ModalMode | null>(null)
  const [selectedItem, setSelectedItem] = useState<ClubApplicationResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<DecisionForm>()

  async function fetchItems(status: ClubApplicationStatus | 'ALL') {
    setLoading(true)
    try {
      const params = status !== 'ALL' ? { status } : {}
      const { data } = await apiClient.get<ClubApplicationResponse[]>(
        '/admin/club-applications',
        { params },
      )
      setItems(data)
    } catch {
      message.error('Не удалось загрузить заявки')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchItems(statusFilter)
  }, [statusFilter])

  function openModal(mode: ModalMode, item: ClubApplicationResponse) {
    setModalMode(mode)
    setSelectedItem(item)
    form.resetFields()
  }

  function closeModal() {
    setModalMode(null)
    setSelectedItem(null)
  }

  async function handleSubmit(values: DecisionForm) {
    if (!selectedItem) return
    setSubmitting(true)
    const body: ClubApplicationDecisionRequest = {
      comment: values.comment || undefined,
      ownerUserId: values.ownerUserId || undefined,
    }
    try {
      if (modalMode === 'approve') {
        const { data } = await apiClient.post<ApproveClubApplicationResponse>(
          `/admin/club-applications/${selectedItem.id}/approve`,
          body,
        )
        message.success(`Заявка одобрена. Создан клуб #${data.createdClubId}`)
      } else if (modalMode === 'reject') {
        await apiClient.post(`/admin/club-applications/${selectedItem.id}/reject`, body)
        message.success('Заявка отклонена')
      } else {
        await apiClient.post(`/admin/club-applications/${selectedItem.id}/request-revision`, { comment: values.comment })
        message.success('Запрошена доработка')
      }
      closeModal()
      fetchItems(statusFilter)
    } catch {
      message.error('Ошибка при обработке заявки')
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<ClubApplicationResponse> = [
    { title: 'ID', dataIndex: 'id', width: 70, sorter: (a, b) => Number(a.id) - Number(b.id) },
    { title: 'Название клуба', dataIndex: 'clubName', ellipsis: true },
    { title: 'Адрес', dataIndex: 'address', ellipsis: true },
    {
      title: 'Заявитель (ID)',
      dataIndex: 'applicantUserId',
      width: 130,
      sorter: (a, b) => Number(a.applicantUserId) - Number(b.applicantUserId),
    },
    {
      title: 'Дата подачи',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => dayjs(v).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      width: 120,
      render: (s: ClubApplicationStatus) => (
        <Tag color={STATUS_COLORS[s]}>{STATUS_LABELS[s]}</Tag>
      ),
    },
    {
      title: 'Действия',
      width: 116,
      fixed: 'right',
      render: (_, record) =>
        record.status === 'PENDING' ? (
          <Space size={4} wrap={false}>
            <Tooltip title="Одобрить">
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => openModal('approve', record)}
              />
            </Tooltip>
            <Tooltip title="Запросить доработку">
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => openModal('revision', record)}
              />
            </Tooltip>
            <Tooltip title="Отклонить">
              <Button
                danger
                size="small"
                icon={<CloseOutlined />}
                onClick={() => openModal('reject', record)}
              />
            </Tooltip>
          </Space>
        ) : null,
    },
  ]

  return (
    <>
      <PageHeader
        title="Заявки на клубы"
        subtitle="Рассмотрение заявок на открытие новых клубов на платформе"
        extra={
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 180 }}
            options={[
              { value: 'ALL', label: 'Все' },
              { value: 'DRAFT', label: 'Черновики' },
              { value: 'PENDING', label: 'Ожидают' },
              { value: 'REVISION_REQUESTED', label: 'На доработке' },
              { value: 'APPROVED', label: 'Одобренные' },
              { value: 'REJECTED', label: 'Отклонённые' },
            ]}
          />
        }
      />

      <SectionCard noPadding>
        <Table
        rowKey="id"
        columns={columns}
        dataSource={items}
        loading={loading}
        pagination={{ pageSize: 20 }}
        scroll={{ x: 980 }}
        expandable={{
          expandedRowRender: (record) => (
            <div style={{ paddingLeft: 8, display: 'flex', flexDirection: 'column', gap: 4 }}>
              {record.description && (
                <Typography.Text>
                  <b>Описание:</b> {record.description}
                </Typography.Text>
              )}
              {record.locationText && (
                <Typography.Text>
                  <b>Локация:</b> {record.locationText}
                </Typography.Text>
              )}
              {record.decisionComment && (
                <Typography.Text>
                  <b>Комментарий решения:</b> {record.decisionComment}
                </Typography.Text>
              )}
              {record.createdClubId && (
                <Typography.Text>
                  <b>Создан клуб ID:</b> {record.createdClubId}
                </Typography.Text>
              )}
            </div>
          ),
          rowExpandable: (record) =>
            !!(record.description || record.locationText || record.decisionComment || record.createdClubId),
        }}
        />
      </SectionCard>

      <Modal
        title={
          modalMode === 'approve' ? 'Одобрить заявку'
          : modalMode === 'reject' ? 'Отклонить заявку'
          : 'Запросить доработку'
        }
        open={modalMode !== null}
        onCancel={closeModal}
        onOk={form.submit}
        okText={
          modalMode === 'approve' ? 'Одобрить'
          : modalMode === 'reject' ? 'Отклонить'
          : 'Запросить доработку'
        }
        okButtonProps={{
          danger: modalMode === 'reject',
          loading: submitting,
        }}
        destroyOnClose
      >
        {selectedItem && (
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            {selectedItem.clubName} — {selectedItem.address}
          </Typography.Text>
        )}
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="comment" label="Комментарий">
            <Input.TextArea rows={3} placeholder="Необязательно" />
          </Form.Item>
          {modalMode === 'approve' && (
            <Form.Item
              name="ownerUserId"
              label="ID владельца"
              extra="Оставьте пустым — владельцем станет заявитель"
            >
              <InputNumber style={{ width: '100%' }} min={1} placeholder="ID пользователя" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </>
  )
}
