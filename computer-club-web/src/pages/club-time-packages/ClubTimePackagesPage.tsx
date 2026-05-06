import React, { useEffect, useState } from 'react'
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
  Typography,
  Space,
  App,
  TimePicker,
  Divider,
  Flex,
  Tabs,
} from 'antd'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import type { ColumnsType } from 'antd/es/table'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
  DesktopOutlined,
  CrownOutlined,
  StarFilled,
} from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import apiClient from '../../utils/apiClient'
import {
  AdminTimePackageResponse,
  CreateTimePackageRequest,
  UpdateTimePackageRequest,
  AdminSeatPriceResponse,
  UpsertSeatPriceRequest,
} from '../../types'

type TimePackageRow = AdminTimePackageResponse & { _standard?: true }

// ─── Seat prices tab ────────────────────────────────────────────────────────

const SEAT_TYPES = [
  { key: 'REGULAR', label: 'Обычное место', icon: <DesktopOutlined /> },
  { key: 'VIP',     label: 'VIP-место',     icon: <CrownOutlined /> },
]

interface SeatRow {
  seatType: string
  label: string
  icon: React.ReactNode
  pricePerHourRub: number | null
}

function SeatPricesTab({ clubId }: { clubId: string }) {
  const { message } = App.useApp()
  const [prices, setPrices] = useState<AdminSeatPriceResponse[]>([])
  const [editing, setEditing] = useState<string | null>(null)
  const [draftValue, setDraftValue] = useState<number>(0)
  const [saving, setSaving] = useState(false)

  async function fetchPrices() {
    try {
      const { data } = await apiClient.get<AdminSeatPriceResponse[]>(`/admin/clubs/${clubId}/seat-prices`)
      setPrices(data)
    } catch {
      // не критично — пустой список
    }
  }

  useEffect(() => { fetchPrices() }, [clubId])

  function priceFor(seatType: string): number | null {
    return prices.find((p) => p.seatType === seatType)?.pricePerHourRub ?? null
  }

  function startEdit(seatType: string) {
    setEditing(seatType)
    setDraftValue(priceFor(seatType) ?? 0)
  }

  async function saveEdit() {
    if (!editing) return
    setSaving(true)
    try {
      const req: UpsertSeatPriceRequest = { seatType: editing, pricePerHourRub: draftValue }
      await apiClient.put(`/admin/clubs/${clubId}/seat-prices`, req)
      message.success('Цена сохранена')
      setEditing(null)
      await fetchPrices()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setSaving(false)
    }
  }

  const definedPrices = prices.map((p) => p.pricePerHourRub)
  const standardPrice = definedPrices.length > 0 ? Math.min(...definedPrices) : null

  // стандартный пакет как первая строка (виртуальная)
  const standardRow: SeatRow = {
    seatType: '__standard__',
    label: 'Стандартный пакет',
    icon: <StarFilled style={{ color: '#52c41a' }} />,
    pricePerHourRub: standardPrice,
  }

  const rows: SeatRow[] = [
    standardRow,
    ...SEAT_TYPES.map(({ key, label, icon }) => ({
      seatType: key,
      label,
      icon,
      pricePerHourRub: priceFor(key),
    })),
  ]

  const columns: ColumnsType<SeatRow> = [
    {
      title: 'Тип',
      dataIndex: 'label',
      render: (label, row) => (
        <Space>
          {row.icon}
          <Typography.Text strong={row.seatType === '__standard__'} style={row.seatType === '__standard__' ? { color: '#52c41a' } : undefined}>
            {label}
          </Typography.Text>
          {row.seatType === '__standard__' && <Tag color="green">По умолчанию</Tag>}
        </Space>
      ),
    },
    {
      title: 'Цена за час',
      dataIndex: 'pricePerHourRub',
      width: 220,
      render: (price, row) => {
        if (row.seatType === '__standard__') {
          return price != null
            ? <Typography.Text strong style={{ color: '#52c41a' }}>{price.toLocaleString('ru-RU')} ₽/ч</Typography.Text>
            : <Typography.Text type="secondary">Задайте цены ниже</Typography.Text>
        }
        if (editing === row.seatType) {
          return (
            <Space>
              <InputNumber
                min={0}
                value={draftValue}
                onChange={(v) => setDraftValue(v ?? 0)}
                addonAfter="₽/ч"
                style={{ width: 140 }}
                autoFocus
                onPressEnter={saveEdit}
              />
              <Button type="primary" size="small" loading={saving} onClick={saveEdit}>Сохранить</Button>
              <Button size="small" onClick={() => setEditing(null)}>Отмена</Button>
            </Space>
          )
        }
        return price != null
          ? `${price.toLocaleString('ru-RU')} ₽/ч`
          : <Typography.Text type="secondary">Не задана</Typography.Text>
      },
    },
    {
      title: 'Действия',
      width: 120,
      render: (_, row) => {
        if (row.seatType === '__standard__' || editing === row.seatType) return null
        return (
          <Button size="small" icon={<EditOutlined />} onClick={() => startEdit(row.seatType)}>
            {row.pricePerHourRub != null ? 'Изменить' : 'Задать'}
          </Button>
        )
      },
    },
  ]

  return (
    <Table
      rowKey="seatType"
      dataSource={rows}
      columns={columns}
      pagination={false}
      rowClassName={(row) => row.seatType === '__standard__' ? 'ant-table-row-standard' : ''}
    />
  )
}

// ─── Time packages tab ───────────────────────────────────────────────────────

interface PackageForm {
  name: string
  hours: number
  pricePerHourRub: number
  sortOrder: number
  isActive?: boolean
  availableFrom?: Dayjs | null
  availableTo?: Dayjs | null
}

function parseTime(s: string | null | undefined): Dayjs | null {
  if (!s) return null
  return dayjs(s, 'HH:mm')
}

function formatTime(d: Dayjs | null | undefined): string | null {
  if (!d) return null
  return d.format('HH:mm')
}

function scheduleLabel(from: string | null, to: string | null): string {
  if (!from || !to) return 'Круглосуточно'
  return `${from} – ${to}`
}

function TimePackagesTab({ clubId }: { clubId: string }) {
  const { message } = App.useApp()

  const [packages, setPackages] = useState<AdminTimePackageResponse[]>([])
  const [seatPrices, setSeatPrices] = useState<AdminSeatPriceResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [showInactive, setShowInactive] = useState(false)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<AdminTimePackageResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<PackageForm>()

  const pricePerHour = Form.useWatch('pricePerHourRub', form) ?? 0
  const hours = Form.useWatch('hours', form) ?? 0
  const totalPreview = pricePerHour * hours

  async function fetchAll() {
    setLoading(true)
    try {
      const [pkgsRes, pricesRes] = await Promise.all([
        apiClient.get<AdminTimePackageResponse[]>(`/admin/clubs/${clubId}/time-packages`),
        apiClient.get<AdminSeatPriceResponse[]>(`/admin/clubs/${clubId}/seat-prices`),
      ])
      setPackages(pkgsRes.data)
      setSeatPrices(pricesRes.data)
    } catch {
      message.error('Не удалось загрузить данные')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchAll() }, [clubId])

  // стандартный пакет: мин. цена среди типов мест
  const standardPrice = seatPrices.length > 0
    ? Math.min(...seatPrices.map((p) => p.pricePerHourRub))
    : null

  // виртуальная строка стандартного пакета
  const standardRow: AdminTimePackageResponse & { _standard?: true } = {
    id: -1,
    name: 'Стандартный',
    hours: 0,
    pricePerHourRub: standardPrice ?? 0,
    totalPriceRub: 0,
    isActive: true,
    sortOrder: -1,
    availableFrom: null,
    availableTo: null,
    _standard: true,
  }

  const displayed: TimePackageRow[] = [
    ...(standardPrice != null ? [standardRow] : []),
    ...(showInactive ? packages : packages.filter((p) => p.isActive)),
  ]

  function openCreate() {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ sortOrder: 0 })
    setModalOpen(true)
  }

  function openEdit(pkg: AdminTimePackageResponse) {
    setEditing(pkg)
    form.setFieldsValue({
      name: pkg.name,
      hours: pkg.hours,
      pricePerHourRub: pkg.pricePerHourRub,
      sortOrder: pkg.sortOrder,
      isActive: pkg.isActive,
      availableFrom: parseTime(pkg.availableFrom),
      availableTo: parseTime(pkg.availableTo),
    })
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditing(null)
    form.resetFields()
  }

  async function onSubmit(values: PackageForm) {
    setSubmitting(true)
    try {
      const from = formatTime(values.availableFrom)
      const to = formatTime(values.availableTo)
      const scheduleFrom = from && to ? from : null
      const scheduleTo = from && to ? to : null

      if (editing) {
        const req: UpdateTimePackageRequest = {
          name: values.name, hours: values.hours, pricePerHourRub: values.pricePerHourRub,
          isActive: values.isActive ?? true, sortOrder: values.sortOrder,
          availableFrom: scheduleFrom, availableTo: scheduleTo,
        }
        await apiClient.put(`/admin/clubs/${clubId}/time-packages/${editing.id}`, req)
        message.success('Пакет обновлён')
      } else {
        const req: CreateTimePackageRequest = {
          name: values.name, hours: values.hours, pricePerHourRub: values.pricePerHourRub,
          sortOrder: values.sortOrder, availableFrom: scheduleFrom, availableTo: scheduleTo,
        }
        await apiClient.post(`/admin/clubs/${clubId}/time-packages`, req)
        message.success('Пакет создан')
      }
      closeModal()
      await fetchAll()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setSubmitting(false)
    }
  }

  async function onDelete(id: number) {
    try {
      await apiClient.delete(`/admin/clubs/${clubId}/time-packages/${id}`)
      message.success('Пакет удалён')
      await fetchAll()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка при удалении')
    }
  }

  async function onToggleActive(pkg: AdminTimePackageResponse, checked: boolean) {
    try {
      const req: UpdateTimePackageRequest = {
        name: pkg.name, hours: pkg.hours, pricePerHourRub: pkg.pricePerHourRub,
        isActive: checked, sortOrder: pkg.sortOrder,
        availableFrom: pkg.availableFrom, availableTo: pkg.availableTo,
      }
      await apiClient.put(`/admin/clubs/${clubId}/time-packages/${pkg.id}`, req)
      await fetchAll()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    }
  }

  const columns: ColumnsType<TimePackageRow> = [
    {
      title: 'Название',
      dataIndex: 'name',
      render: (name, row) =>
        row._standard ? (
          <Space>
            <StarFilled style={{ color: '#52c41a' }} />
            <Typography.Text strong style={{ color: '#52c41a' }}>{name}</Typography.Text>
            <Tag color="green">По умолчанию</Tag>
          </Space>
        ) : name,
    },
    {
      title: 'Часы',
      dataIndex: 'hours',
      width: 80,
      render: (h, row) => row._standard ? '—' : `${h} ч`,
    },
    {
      title: 'Цена/час',
      dataIndex: 'pricePerHourRub',
      width: 130,
      render: (p) => `${p.toLocaleString('ru-RU')} ₽/ч`,
    },
    {
      title: 'Итого',
      dataIndex: 'totalPriceRub',
      width: 120,
      render: (p, row) => row._standard ? '—' : `${p.toLocaleString('ru-RU')} ₽`,
    },
    {
      title: 'Доступность',
      width: 150,
      render: (_, row) => {
        if (row._standard) return <Typography.Text type="secondary">Круглосуточно</Typography.Text>
        const label = scheduleLabel(row.availableFrom, row.availableTo)
        return (
          <Space size={4}>
            <ClockCircleOutlined style={{ color: row.availableFrom ? '#1677ff' : '#8c8c8c' }} />
            <Typography.Text type={row.availableFrom ? undefined : 'secondary'} style={{ fontSize: 13 }}>
              {label}
            </Typography.Text>
          </Space>
        )
      },
    },
    {
      title: 'Активно',
      dataIndex: 'isActive',
      width: 100,
      render: (val, row) =>
        row._standard ? null : (
          <Switch checked={val} size="small" onChange={(checked) => onToggleActive(row, checked)} />
        ),
    },
    {
      title: 'Действия',
      width: 140,
      render: (_, row) =>
        row._standard ? null : (
          <Space>
            <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>Изменить</Button>
            <Popconfirm
              title="Удалить пакет?"
              description="Пакет будет удалён навсегда."
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
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Typography.Text strong>Пакеты</Typography.Text>
          <Tag color="blue">{packages.filter((p) => p.isActive).length} активных</Tag>
        </Space>
        <Space>
          <Switch
            checked={showInactive}
            onChange={setShowInactive}
            checkedChildren="С неактивными"
            unCheckedChildren="Только активные"
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Добавить пакет
          </Button>
        </Space>
      </div>

      <Table
        rowKey={(row) => row._standard ? '__standard__' : String(row.id)}
        dataSource={displayed}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        rowClassName={(row) => {
          if (row._standard) return 'ant-table-row-standard'
          if (!row.isActive) return 'ant-table-row-disabled'
          return ''
        }}
      />

      <Modal
        open={modalOpen}
        title={editing ? 'Редактировать пакет' : 'Добавить пакет времени'}
        footer={null}
        onCancel={closeModal}
        width={440}
      >
        <Form layout="vertical" form={form} onFinish={onSubmit}>
          <Form.Item name="name" label="Название" rules={[{ required: true, message: 'Укажите название' }]}>
            <Input autoFocus placeholder="Например: 3 часа, Ночной пакет" />
          </Form.Item>
          <Form.Item name="hours" label="Количество часов" rules={[{ required: true, message: 'Укажите количество часов' }]}>
            <InputNumber min={1} max={24} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="pricePerHourRub" label="Цена за час (₽/ч)" rules={[{ required: true, message: 'Укажите цену за час' }]}>
            <InputNumber min={0} style={{ width: '100%' }} addonAfter="₽/ч" />
          </Form.Item>
          {hours > 0 && pricePerHour > 0 && (
            <div style={{ marginTop: -12, marginBottom: 16, color: '#8c8c8c', fontSize: 13 }}>
              Итоговая стоимость:{' '}
              <strong style={{ color: '#262626' }}>{totalPreview.toLocaleString('ru-RU')} ₽</strong>
              {' '}({hours} ч × {pricePerHour.toLocaleString('ru-RU')} ₽/ч)
            </div>
          )}
          <Form.Item name="sortOrder" label="Порядок сортировки">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Divider style={{ margin: '8px 0 12px' }} />
          <Typography.Text strong>Окно доступности</Typography.Text>
          <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginTop: 4, marginBottom: 10 }}>
            Если оставить пустым — пакет доступен круглосуточно. Для ночных пакетов «От» может быть больше «До» (например, 22:00 – 08:00).
          </Typography.Paragraph>
          <Flex gap={12}>
            <Form.Item name="availableFrom" label="От" style={{ flex: 1 }}>
              <TimePicker format="HH:mm" minuteStep={30} style={{ width: '100%' }} placeholder="Не задано" />
            </Form.Item>
            <Form.Item name="availableTo" label="До" style={{ flex: 1 }}>
              <TimePicker format="HH:mm" minuteStep={30} style={{ width: '100%' }} placeholder="Не задано" />
            </Form.Item>
          </Flex>

          {editing && (
            <Form.Item name="isActive" label="Активно" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {editing ? 'Сохранить' : 'Создать'}
          </Button>
        </Form>
      </Modal>
    </>
  )
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function ClubTimePackagesPage() {
  const { clubId } = useParams<{ clubId: string }>()

  return (
    <div>
      <PageHeader title="Тарифы" subtitle="Пакеты времени и почасовые цены на места" />
      <SectionCard>
        <Tabs
          defaultActiveKey="packages"
          items={[
            {
              key: 'packages',
              label: 'Пакеты',
              children: <TimePackagesTab clubId={clubId!} />,
            },
            {
              key: 'seat-prices',
              label: 'Цены за места',
              children: <SeatPricesTab clubId={clubId!} />,
            },
          ]}
        />
      </SectionCard>
    </div>
  )
}
