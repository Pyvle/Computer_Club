import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Tag,
  Popconfirm,
  Space,
  Typography,
  Empty,
  Switch,
  App,
  Spin,
} from 'antd'
import { PlusOutlined, CopyOutlined, DeleteOutlined, ExpandOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import type {
  FloorplanSummaryResponse,
  FloorplanResponse,
  CreateFloorplanRequest,
  UpdateFloorplanRequest,
  CloneFloorplanRequest,
  AdminSeatResponse,
} from '../../types'
import FloorplanEditor, { type FloorplanData, DEFAULT_COLS, DEFAULT_ROWS } from './FloorplanEditor'

const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'blue',
  PUBLISHED: 'green',
  ARCHIVED: 'default',
}

const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Черновик',
  PUBLISHED: 'Опубликована',
  ARCHIVED: 'Архив',
}

interface CreateForm {
  name: string
  gridSize: number
}

export default function ClubFloorplansPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()

  const [list, setList] = useState<FloorplanSummaryResponse[]>([])
  const [listLoading, setListLoading] = useState(true)
  const [showArchived, setShowArchived] = useState(false)

  const [selected, setSelected] = useState<FloorplanResponse | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const [seats, setSeats] = useState<AdminSeatResponse[]>([])

  // holds latest editor data between renders; only read at save time
  const currentData = useRef<unknown>(null)

  // Create modal
  const [createOpen, setCreateOpen] = useState(false)
  const [createSubmitting, setCreateSubmitting] = useState(false)
  const [createForm] = Form.useForm<CreateForm>()

  // Clone modal
  const [cloneOpen, setCloneOpen] = useState(false)
  const [cloneSubmitting, setCloneSubmitting] = useState(false)
  const [cloneForm] = Form.useForm<{ name: string }>()

  // Resize modal
  const [resizeOpen, setResizeOpen] = useState(false)
  const [resizeSubmitting, setResizeSubmitting] = useState(false)
  const [resizeForm] = Form.useForm<{ width: number; height: number; gridSize: number }>()

  // Action loading states
  const [saving, setSaving] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [unpublishing, setUnpublishing] = useState(false)
  const [archiving, setArchiving] = useState(false)

  async function fetchList() {
    setListLoading(true)
    try {
      const { data } = await apiClient.get<FloorplanSummaryResponse[]>(
        `/admin/clubs/${clubId}/floorplans`,
      )
      setList(data)
    } catch {
      message.error('Не удалось загрузить схемы зала')
    } finally {
      setListLoading(false)
    }
  }

  async function fetchDetail(id: number) {
    setDetailLoading(true)
    try {
      const { data } = await apiClient.get<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans/${id}`,
      )
      setSelected(data)
      currentData.current = data.data
    } catch {
      message.error('Не удалось загрузить схему')
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
    // load seats once for this club — needed by the editor seat picker
    apiClient
      .get<AdminSeatResponse[]>(`/admin/clubs/${clubId}/seats`)
      .then((r) => setSeats(r.data))
      .catch(() => {
        /* seats are optional — editor still works, just won't show labels */
      })
  }, [clubId])

  async function onSelectItem(id: number) {
    await fetchDetail(id)
  }

  // --- Create ---

  function openCreate() {
    createForm.resetFields()
    createForm.setFieldsValue({ gridSize: 40, name: '' })
    setCreateOpen(true)
  }

  async function onCreateSubmit(values: CreateForm) {
    setCreateSubmitting(true)
    try {
      const gridSize = values.gridSize ?? 40
      const req: CreateFloorplanRequest = {
        name: values.name,
        width: DEFAULT_COLS * gridSize,
        height: DEFAULT_ROWS * gridSize,
        gridSize,
      }
      const { data } = await apiClient.post<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans`,
        req,
      )
      message.success('Схема создана')
      setCreateOpen(false)
      await fetchList()
      await fetchDetail(data.id)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err?.response?.data?.message ?? 'Ошибка')
    } finally {
      setCreateSubmitting(false)
    }
  }

  // --- Save (PUT) ---

  async function onSave() {
    if (!selected) return
    setSaving(true)
    try {
      const req: UpdateFloorplanRequest = {
        name: selected.name,
        width: selected.width,
        height: selected.height,
        gridSize: selected.gridSize,
        version: selected.version,
        data: currentData.current ?? selected.data,
      }
      const { data } = await apiClient.put<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans/${selected.id}`,
        req,
      )
      setSelected(data)
      currentData.current = data.data
      message.success('Схема сохранена')
      await fetchList()
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { message?: string } } }
      if (err?.response?.status === 409) {
        message.error('Схема была изменена другим пользователем. Обновите страницу.')
        await fetchDetail(selected.id)
      } else {
        message.error(err?.response?.data?.message ?? 'Ошибка')
      }
    } finally {
      setSaving(false)
    }
  }

  // --- Resize ---

  function openResize() {
    if (!selected) return
    resizeForm.setFieldsValue({
      width: selected.width,
      height: selected.height,
      gridSize: selected.gridSize,
    })
    setResizeOpen(true)
  }

  async function onResizeSubmit(values: { width: number; height: number; gridSize: number }) {
    if (!selected) return
    setResizeSubmitting(true)
    try {
      const existingData = currentData.current ?? selected.data
      // убираем места, которые выходят за пределы новой сетки
      const newCols = Math.floor(values.width / values.gridSize)
      const newRows = Math.floor(values.height / values.gridSize)
      let filteredData = existingData
      if (existingData && typeof existingData === 'object') {
        const d = existingData as { items?: unknown[] }
        if (Array.isArray(d.items)) {
          const items = d.items.filter((it: unknown) => {
            const item = it as { col?: number; row?: number; x?: number; y?: number; w?: number }
            // новый формат: col/row напрямую
            if (typeof item.col === 'number' && typeof item.row === 'number') {
              return item.col < newCols && item.row < newRows
            }
            // старый формат: x/y/w/h
            if (typeof item.x !== 'number' || typeof item.y !== 'number') return false
            const isAbsolute = typeof item.w === 'number' && item.w >= 1
            const col = isAbsolute
              ? Math.round(item.x / selected.gridSize)
              : Math.round((item.x * selected.width) / selected.gridSize)
            const row = isAbsolute
              ? Math.round(item.y / selected.gridSize)
              : Math.round((item.y * selected.height) / selected.gridSize)
            return col < newCols && row < newRows
          })
          filteredData = { ...d, items }
        }
      }
      const req: UpdateFloorplanRequest = {
        name: selected.name,
        width: values.width,
        height: values.height,
        gridSize: values.gridSize,
        version: selected.version,
        data: filteredData,
      }
      const { data } = await apiClient.put<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans/${selected.id}`,
        req,
      )
      setSelected(data)
      currentData.current = data.data
      message.success('Размер схемы изменён')
      setResizeOpen(false)
      await fetchList()
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { message?: string } } }
      if (err?.response?.status === 409) {
        message.error('Конфликт версий — обновите страницу')
        await fetchDetail(selected.id)
      } else {
        message.error(err?.response?.data?.message ?? 'Ошибка')
      }
    } finally {
      setResizeSubmitting(false)
    }
  }

  // --- Publish ---

  async function onPublish() {
    if (!selected) return
    setPublishing(true)
    try {
      await apiClient.post(`/admin/clubs/${clubId}/floorplans/${selected.id}/publish`)
      message.success('Схема опубликована')
      await fetchList()
      await fetchDetail(selected.id)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err?.response?.data?.message ?? 'Ошибка')
    } finally {
      setPublishing(false)
    }
  }

  // --- Unpublish ---

  async function onUnpublish() {
    if (!selected) return
    setUnpublishing(true)
    try {
      await apiClient.post(`/admin/clubs/${clubId}/floorplans/${selected.id}/unpublish`)
      message.success('Публикация снята')
      await fetchList()
      await fetchDetail(selected.id)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err?.response?.data?.message ?? 'Ошибка')
    } finally {
      setUnpublishing(false)
    }
  }

  // --- Clone ---

  function openClone() {
    cloneForm.resetFields()
    cloneForm.setFieldsValue({ name: selected ? `${selected.name} (копия)` : '' })
    setCloneOpen(true)
  }

  async function onCloneSubmit(values: { name: string }) {
    if (!selected) return
    setCloneSubmitting(true)
    try {
      const req: CloneFloorplanRequest = { name: values.name }
      const { data } = await apiClient.post<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans/${selected.id}/clone`,
        req,
      )
      message.success('Схема клонирована')
      setCloneOpen(false)
      await fetchList()
      await fetchDetail(data.id)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err?.response?.data?.message ?? 'Ошибка')
    } finally {
      setCloneSubmitting(false)
    }
  }

  // --- Archive ---

  async function onArchive() {
    if (!selected) return
    setArchiving(true)
    try {
      await apiClient.delete(`/admin/clubs/${clubId}/floorplans/${selected.id}`)
      message.success('Схема архивирована')
      const updatedList = await apiClient
        .get<FloorplanSummaryResponse[]>(`/admin/clubs/${clubId}/floorplans`)
        .then((r) => r.data)
      setList(updatedList)
      const next = updatedList.find((f) => f.id !== selected.id && f.status !== 'ARCHIVED')
      if (next) {
        await fetchDetail(next.id)
      } else {
        setSelected(null)
        currentData.current = null
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err?.response?.data?.message ?? 'Ошибка')
    } finally {
      setArchiving(false)
    }
  }

  const displayed = showArchived ? list : list.filter((f) => f.status !== 'ARCHIVED')
  const isEditable = selected?.status === 'DRAFT'

  return (
    <div style={{ display: 'flex', gap: 16, height: 'calc(100vh - 120px)' }}>
      {/* --- Left panel --- */}
      <div
        style={{
          width: '30%',
          minWidth: 240,
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            padding: '12px 16px',
            borderBottom: '1px solid #f0f0f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Typography.Title level={5} style={{ margin: 0 }}>
            Схемы зала
          </Typography.Title>
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={openCreate}>
            Создать
          </Button>
        </div>

        <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0' }}>
          <Switch
            size="small"
            checked={showArchived}
            onChange={setShowArchived}
            checkedChildren="С архивными"
            unCheckedChildren="Без архивных"
          />
        </div>

        <div style={{ flex: 1, overflowY: 'auto' }}>
          {listLoading ? (
            <div style={{ padding: 24, textAlign: 'center' }}>
              <Spin />
            </div>
          ) : displayed.length === 0 ? (
            <div style={{ padding: 24, color: '#999', textAlign: 'center', fontSize: 13 }}>
              Схем нет
            </div>
          ) : (
            displayed.map((f) => (
              <div
                key={f.id}
                onClick={() => onSelectItem(f.id)}
                style={{
                  padding: '10px 16px',
                  cursor: 'pointer',
                  borderBottom: '1px solid #f5f5f5',
                  background: selected?.id === f.id ? '#e6f4ff' : 'transparent',
                  transition: 'background 0.15s',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                  <span style={{ fontWeight: 500, flex: 1, fontSize: 13 }}>{f.name}</span>
                  <Tag color={STATUS_COLOR[f.status]} style={{ margin: 0 }}>
                    {STATUS_LABEL[f.status]}
                  </Tag>
                </div>
                <div style={{ fontSize: 11, color: '#999' }}>
                  {new Date(f.updatedAt).toLocaleString('ru-RU')}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* --- Right panel --- */}
      <div
        style={{
          flex: 1,
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {!selected && !detailLoading ? (
          <div
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Empty description="Выберите схему слева или создайте новую" />
          </div>
        ) : detailLoading ? (
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Spin />
          </div>
        ) : selected ? (
          <>
            {/* Header */}
            <div
              style={{
                padding: '12px 20px',
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                alignItems: 'center',
                gap: 12,
              }}
            >
              <Typography.Title level={5} style={{ margin: 0, flex: 1 }}>
                {selected.name}
              </Typography.Title>
              <Tag color={STATUS_COLOR[selected.status]}>{STATUS_LABEL[selected.status]}</Tag>
            </div>

            {/* Meta */}
            <div
              style={{
                padding: '8px 20px',
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                gap: 24,
                fontSize: 12,
                color: '#666',
              }}
            >
              <span>
                Размер: {selected.width}×{selected.height}px
              </span>
              <span>Шаг сетки: {selected.gridSize}px</span>
              <span>Версия: {selected.version}</span>
              <span>Обновлено: {new Date(selected.updatedAt).toLocaleString('ru-RU')}</span>
            </div>

            {/* Actions */}
            <div
              style={{
                padding: '10px 20px',
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                gap: 8,
                flexWrap: 'wrap',
              }}
            >
              {selected.status === 'DRAFT' && (
                <Button type="primary" loading={saving} onClick={onSave}>
                  Сохранить
                </Button>
              )}

              {selected.status === 'DRAFT' && (
                <Button icon={<ExpandOutlined />} onClick={openResize}>
                  Изменить размер
                </Button>
              )}

              {selected.status === 'DRAFT' && (
                <Popconfirm
                  title="Опубликовать схему?"
                  description="Текущая опубликованная схема будет заменена."
                  onConfirm={onPublish}
                  okText="Опубликовать"
                  cancelText="Отмена"
                >
                  <Button loading={publishing}>Опубликовать</Button>
                </Popconfirm>
              )}

              {selected.status === 'PUBLISHED' && (
                <Popconfirm
                  title="Снять публикацию?"
                  description="Схема вернётся в статус «Черновик»."
                  onConfirm={onUnpublish}
                  okText="Снять"
                  cancelText="Отмена"
                >
                  <Button loading={unpublishing}>Снять публикацию</Button>
                </Popconfirm>
              )}

              <Button icon={<CopyOutlined />} onClick={openClone}>
                Клонировать
              </Button>

              {(selected.status === 'DRAFT' || selected.status === 'PUBLISHED') && (
                <Popconfirm
                  title={
                    selected.status === 'PUBLISHED'
                      ? 'Архивировать опубликованную схему?'
                      : 'Архивировать схему?'
                  }
                  description={
                    selected.status === 'PUBLISHED'
                      ? 'Схема перестанет быть активной.'
                      : 'Схема будет недоступна для редактирования.'
                  }
                  onConfirm={onArchive}
                  okText="Архивировать"
                  cancelText="Отмена"
                  okButtonProps={{ danger: true }}
                >
                  <Button danger icon={<DeleteOutlined />} loading={archiving}>
                    Архивировать
                  </Button>
                </Popconfirm>
              )}
            </div>

            {/* Visual editor */}
            <div style={{ flex: 1, padding: '16px 20px', overflowY: 'auto' }}>
              <FloorplanEditor
                key={selected.id}
                floorplan={selected}
                seats={seats}
                readOnly={!isEditable}
                onChange={(data: FloorplanData) => {
                  currentData.current = data
                }}
              />
            </div>
          </>
        ) : null}
      </div>

      {/* Create modal */}
      <Modal
        open={createOpen}
        title="Создать схему зала"
        footer={null}
        onCancel={() => setCreateOpen(false)}
      >
        <Form layout="vertical" form={createForm} onFinish={onCreateSubmit}>
          <Form.Item
            name="name"
            label="Название"
            rules={[{ required: true, message: 'Укажите название' }]}
          >
            <Input autoFocus placeholder="Главный зал" />
          </Form.Item>
          <Form.Item name="gridSize" label="Размер ячейки">
            <Select
              options={[
                { value: 30, label: 'Мелкая (30px) — больше ячеек' },
                { value: 40, label: 'Средняя (40px)' },
                { value: 60, label: 'Крупная (60px) — меньше ячеек' },
              ]}
            />
          </Form.Item>
          <div style={{ fontSize: 12, color: '#888', marginTop: -8, marginBottom: 16 }}>
            Сетка {DEFAULT_COLS}×{DEFAULT_ROWS} ячеек. Нарисуйте комнаты инструментом «Комната».
          </div>
          <Button
            type="primary"
            htmlType="submit"
            block
            loading={createSubmitting}
            style={{ marginTop: 16 }}
          >
            Создать
          </Button>
        </Form>
      </Modal>

      {/* Resize modal */}
      <Modal
        open={resizeOpen}
        title="Изменить размер схемы"
        footer={null}
        onCancel={() => setResizeOpen(false)}
      >
        <Form layout="vertical" form={resizeForm} onFinish={onResizeSubmit}>
          <Space style={{ width: '100%' }}>
            <Form.Item
              name="width"
              label="Ширина (px)"
              rules={[{ required: true, message: 'Укажите ширину' }]}
              style={{ marginBottom: 0 }}
            >
              <InputNumber min={1} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item
              name="height"
              label="Высота (px)"
              rules={[{ required: true, message: 'Укажите высоту' }]}
              style={{ marginBottom: 0 }}
            >
              <InputNumber min={1} style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="gridSize" label="Шаг сетки" style={{ marginBottom: 0 }}>
              <InputNumber min={1} style={{ width: 100 }} />
            </Form.Item>
          </Space>
          <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
            Места, выходящие за новые границы, будут убраны с карты.
          </div>
          <Button
            type="primary"
            htmlType="submit"
            block
            loading={resizeSubmitting}
            style={{ marginTop: 16 }}
          >
            Применить
          </Button>
        </Form>
      </Modal>

      {/* Clone modal */}
      <Modal
        open={cloneOpen}
        title="Клонировать схему"
        footer={null}
        onCancel={() => setCloneOpen(false)}
      >
        <Form layout="vertical" form={cloneForm} onFinish={onCloneSubmit}>
          <Form.Item
            name="name"
            label="Название копии"
            rules={[{ required: true, message: 'Укажите название' }]}
          >
            <Input autoFocus />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={cloneSubmitting}>
            Клонировать
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
