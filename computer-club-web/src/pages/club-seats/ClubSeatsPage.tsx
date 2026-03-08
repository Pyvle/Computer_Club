import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Switch,
  Tag,
  Popconfirm,
  Select,
  Typography,
  Space,
  App,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import { AdminSeatResponse, CreateSeatRequest, UpdateSeatRequest } from '../../types'

interface SeatForm {
  label: string
  type: 'REGULAR' | 'VIP'
  sortOrder?: number
  isActive?: boolean
}

export default function ClubSeatsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()

  const [seats, setSeats] = useState<AdminSeatResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [showArchived, setShowArchived] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingSeat, setEditingSeat] = useState<AdminSeatResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<SeatForm>()

  async function fetchSeats() {
    setLoading(true)
    try {
      const { data } = await apiClient.get<AdminSeatResponse[]>(
        `/admin/clubs/${clubId}/seats`
      )
      setSeats(data)
    } catch {
      message.error('Не удалось загрузить места')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSeats()
  }, [clubId])

  function openCreate() {
    setEditingSeat(null)
    form.resetFields()
    form.setFieldsValue({ type: 'REGULAR', sortOrder: 0 })
    setModalOpen(true)
  }

  function openEdit(seat: AdminSeatResponse) {
    setEditingSeat(seat)
    form.setFieldsValue({
      label: seat.label,
      type: seat.type,
      sortOrder: seat.sortOrder,
      isActive: seat.isActive,
    })
    setModalOpen(true)
  }

  async function onSubmit(values: SeatForm) {
    setSubmitting(true)
    try {
      if (editingSeat) {
        const req: UpdateSeatRequest = {
          label: values.label,
          type: values.type,
          sortOrder: values.sortOrder,
          isActive: values.isActive,
        }
        await apiClient.put(`/admin/clubs/${clubId}/seats/${editingSeat.id}`, req)
        message.success('Место обновлено')
      } else {
        const req: CreateSeatRequest = {
          label: values.label,
          type: values.type,
          sortOrder: values.sortOrder,
        }
        await apiClient.post(`/admin/clubs/${clubId}/seats`, req)
        message.success('Место создано')
      }
      setModalOpen(false)
      setEditingSeat(null)
      form.resetFields()
      await fetchSeats()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setSubmitting(false)
    }
  }

  async function onArchive(seatId: number) {
    try {
      await apiClient.delete(`/admin/clubs/${clubId}/seats/${seatId}`)
      message.success('Место архивировано')
      await fetchSeats()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    }
  }

  async function onToggleActive(seat: AdminSeatResponse, checked: boolean) {
    try {
      const req: UpdateSeatRequest = {
        label: seat.label,
        type: seat.type,
        sortOrder: seat.sortOrder,
        isActive: checked,
      }
      await apiClient.put(`/admin/clubs/${clubId}/seats/${seat.id}`, req)
      await fetchSeats()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    }
  }

  const displayed = showArchived ? seats : seats.filter((s) => s.isActive)

  const columns: ColumnsType<AdminSeatResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 70,
    },
    {
      title: 'Номер',
      dataIndex: 'label',
    },
    {
      title: 'Тип',
      dataIndex: 'type',
      width: 120,
      render: (type: string) =>
        type === 'VIP' ? (
          <Tag color="gold">VIP</Tag>
        ) : (
          <Tag color="blue">Обычное</Tag>
        ),
    },
    {
      title: 'Порядок',
      dataIndex: 'sortOrder',
      width: 100,
    },
    {
      title: 'Активно',
      dataIndex: 'isActive',
      width: 100,
      render: (val: boolean, row) => (
        <Switch
          checked={val}
          size="small"
          onChange={(checked) => onToggleActive(row, checked)}
        />
      ),
    },
    {
      title: 'Действия',
      width: 140,
      render: (_, row) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(row)}
          >
            Изменить
          </Button>
          <Popconfirm
            title="Архивировать место?"
            description="Место станет неактивным и не будет отображаться."
            onConfirm={() => onArchive(row.id)}
            okText="Архивировать"
            cancelText="Отмена"
            okButtonProps={{ danger: true }}
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Места
          </Typography.Title>
          <Tag color="blue">{seats.filter((s) => s.isActive).length} активных</Tag>
        </Space>
        <Space>
          <Switch
            checked={showArchived}
            onChange={setShowArchived}
            checkedChildren="С архивными"
            unCheckedChildren="Только активные"
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Добавить место
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={displayed}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        rowClassName={(row) => (!row.isActive ? 'ant-table-row-disabled' : '')}
      />

      <Modal
        open={modalOpen}
        title={editingSeat ? 'Редактировать место' : 'Добавить место'}
        footer={null}
        onCancel={() => { setModalOpen(false); setEditingSeat(null); form.resetFields() }}
      >
        <Form layout="vertical" form={form} onFinish={onSubmit}>
          <Form.Item
            name="label"
            label="Номер / название"
            rules={[{ required: true, message: 'Укажите название' }]}
          >
            <Input autoFocus placeholder="Например: 1, VIP-1" />
          </Form.Item>
          <Form.Item
            name="type"
            label="Тип"
            rules={[{ required: true, message: 'Выберите тип' }]}
          >
            <Select
              options={[
                { value: 'REGULAR', label: 'Обычное' },
                { value: 'VIP', label: 'VIP' },
              ]}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="Порядок сортировки">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          {editingSeat && (
            <Form.Item name="isActive" label="Активно" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {editingSeat ? 'Сохранить' : 'Создать'}
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
