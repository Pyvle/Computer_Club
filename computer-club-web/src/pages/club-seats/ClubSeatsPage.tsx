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
  Card,
  Collapse,
  Divider,
  Spin,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined, EditOutlined, DeleteOutlined, MinusCircleOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import { AdminSeatResponse, CreateSeatRequest, UpdateSeatRequest, SeatSpecResponse, SpecLine } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'

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

  // --- Характеристики мест ---
  const [specs, setSpecs] = useState<SeatSpecResponse[]>([])
  const [specsLoading, setSpecsLoading] = useState(false)
  const [editingSpec, setEditingSpec] = useState<SeatSpecResponse | null>(null)
  const [specModalOpen, setSpecModalOpen] = useState(false)
  const [specSubmitting, setSpecSubmitting] = useState(false)
  const [specForm] = Form.useForm<{ title: string; specs: SpecLine[] }>()

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

  async function fetchSpecs() {
    setSpecsLoading(true)
    try {
      const { data } = await apiClient.get<SeatSpecResponse[]>(`/admin/clubs/${clubId}/seat-specs`)
      setSpecs(data)
    } catch {
      // если характеристик ещё нет — просто пустой список
    } finally {
      setSpecsLoading(false)
    }
  }

  useEffect(() => {
    fetchSeats()
    fetchSpecs()
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

  function openSpecEdit(spec: SeatSpecResponse) {
    setEditingSpec(spec)
    specForm.setFieldsValue({ title: spec.title, specs: spec.specs })
    setSpecModalOpen(true)
  }

  function openSpecCreate(seatType: 'REGULAR' | 'VIP') {
    const defaultTitle = seatType === 'VIP' ? 'VIP' : 'СТАНДАРТ'
    setEditingSpec({ seatType, title: defaultTitle, specs: [] })
    specForm.setFieldsValue({ title: defaultTitle, specs: [] })
    setSpecModalOpen(true)
  }

  async function onSpecSubmit(values: { title: string; specs: SpecLine[] }) {
    if (!editingSpec) return
    setSpecSubmitting(true)
    try {
      await apiClient.put(`/admin/clubs/${clubId}/seat-specs/${editingSpec.seatType}`, {
        title: values.title,
        specs: values.specs ?? [],
      })
      message.success('Характеристики сохранены')
      setSpecModalOpen(false)
      setEditingSpec(null)
      specForm.resetFields()
      await fetchSpecs()
    } catch {
      message.error('Не удалось сохранить характеристики')
    } finally {
      setSpecSubmitting(false)
    }
  }

  async function onDelete(seatId: number) {
    try {
      await apiClient.delete(`/admin/clubs/${clubId}/seats/${seatId}`)
      message.success('Место удалено')
      await fetchSeats()
    } catch (e: any) {
      const msg: string = e?.response?.data?.message ?? ''
      if (msg.toLowerCase().includes('active') || msg.toLowerCase().includes('upcoming') || e?.response?.status === 409) {
        message.error('Нельзя удалить: есть активные или предстоящие бронирования на это место')
      } else {
        message.error(msg || 'Ошибка при удалении')
      }
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
      sorter: (a, b) => Number(a.id) - Number(b.id),
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
            title="Удалить место?"
            description="Место будет удалено навсегда. Нельзя удалить, если есть активные бронирования."
            onConfirm={() => onDelete(row.id)}
            okText="Удалить"
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
      <PageHeader
        title="Места"
        subtitle={`${seats.filter((s) => s.isActive).length} активных мест`}
        extra={
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
        }
      />

      <SectionCard noPadding style={{ marginBottom: 24 }}>
        <Table
          rowKey="id"
          dataSource={displayed}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          rowClassName={(row) => (!row.isActive ? 'ant-table-row-disabled' : '')}
        />
      </SectionCard>

      <Divider orientation="left">Характеристики мест</Divider>

      {specsLoading ? <Spin style={{ display: 'block', margin: '24px auto' }} /> : <Collapse
        items={(['REGULAR', 'VIP'] as const).map((seatType) => {
          const spec = specs.find((s) => s.seatType === seatType)
          return {
            key: seatType,
            label: seatType === 'VIP' ? 'VIP' : 'СТАНДАРТ',
            extra: (
              <Button
                size="small"
                icon={spec ? <EditOutlined /> : <PlusOutlined />}
                onClick={(e) => {
                  e.stopPropagation()
                  spec ? openSpecEdit(spec) : openSpecCreate(seatType)
                }}
              >
                {spec ? 'Редактировать' : 'Добавить'}
              </Button>
            ),
            children: spec && spec.specs.length > 0 ? (
              <Table
                size="small"
                pagination={false}
                dataSource={spec.specs.map((s, i) => ({ ...s, key: i }))}
                columns={[
                  { title: 'Параметр', dataIndex: 'name', key: 'name' },
                  { title: 'Значение', dataIndex: 'value', key: 'value' },
                ]}
              />
            ) : (
              <Typography.Text type="secondary">Характеристики не заданы</Typography.Text>
            ),
          }
        })}
      />}

      <Modal
        open={specModalOpen}
        title={`Характеристики: ${editingSpec?.seatType === 'VIP' ? 'VIP' : 'СТАНДАРТ'}`}
        footer={null}
        onCancel={() => { setSpecModalOpen(false); setEditingSpec(null); specForm.resetFields() }}
        width={560}
      >
        <Form layout="vertical" form={specForm} onFinish={onSpecSubmit}>
          <Form.Item name="title" label="Заголовок" rules={[{ required: true }]}>
            <Input placeholder="Например: СТАНДАРТ" />
          </Form.Item>

          <Form.List name="specs">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name }) => (
                  <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 4 }}>
                    <Form.Item name={[name, 'name']} rules={[{ required: true, message: 'Параметр' }]}>
                      <Input placeholder="Параметр (напр. Процессор)" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item name={[name, 'value']} rules={[{ required: true, message: 'Значение' }]}>
                      <Input placeholder="Значение (напр. Intel i7)" style={{ width: 200 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} style={{ color: 'red' }} />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />} block>
                  Добавить строку
                </Button>
              </>
            )}
          </Form.List>

          <Button type="primary" htmlType="submit" block loading={specSubmitting} style={{ marginTop: 16 }}>
            Сохранить
          </Button>
        </Form>
      </Modal>

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
