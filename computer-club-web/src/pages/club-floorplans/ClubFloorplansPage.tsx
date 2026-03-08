import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Tag,
  Popconfirm,
  Space,
  Typography,
  Empty,
  Switch,
  App,
  Spin,
} from 'antd'
import { PlusOutlined, CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import type {
  FloorplanSummaryResponse,
  FloorplanResponse,
  CreateFloorplanRequest,
  UpdateFloorplanRequest,
  CloneFloorplanRequest,
} from '../../types'

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
  width: number
  height: number
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

  // JSON editor state
  const [jsonText, setJsonText] = useState('')

  // Create modal
  const [createOpen, setCreateOpen] = useState(false)
  const [createSubmitting, setCreateSubmitting] = useState(false)
  const [createForm] = Form.useForm<CreateForm>()

  // Clone modal
  const [cloneOpen, setCloneOpen] = useState(false)
  const [cloneSubmitting, setCloneSubmitting] = useState(false)
  const [cloneForm] = Form.useForm<{ name: string }>()

  // Action loading states
  const [saving, setSaving] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [unpublishing, setUnpublishing] = useState(false)
  const [archiving, setArchiving] = useState(false)

  async function fetchList() {
    setListLoading(true)
    try {
      const { data } = await apiClient.get<FloorplanSummaryResponse[]>(
        `/admin/clubs/${clubId}/floorplans`
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
        `/admin/clubs/${clubId}/floorplans/${id}`
      )
      setSelected(data)
      setJsonText(JSON.stringify(data.data, null, 2))
    } catch {
      message.error('Не удалось загрузить схему')
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    fetchList()
  }, [clubId])

  async function onSelectItem(id: number) {
    await fetchDetail(id)
  }

  // --- Create ---

  function openCreate() {
    createForm.resetFields()
    createForm.setFieldsValue({ gridSize: 10 })
    setCreateOpen(true)
  }

  async function onCreateSubmit(values: CreateForm) {
    setCreateSubmitting(true)
    try {
      const req: CreateFloorplanRequest = {
        name: values.name,
        width: values.width,
        height: values.height,
        gridSize: values.gridSize,
      }
      const { data } = await apiClient.post<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans`,
        req
      )
      message.success('Схема создана')
      setCreateOpen(false)
      await fetchList()
      await fetchDetail(data.id)
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setCreateSubmitting(false)
    }
  }

  // --- Save (PUT) ---

  async function onSave() {
    if (!selected) return
    let parsed: unknown
    try {
      parsed = JSON.parse(jsonText)
    } catch {
      message.error('Некорректный JSON')
      return
    }
    setSaving(true)
    try {
      const req: UpdateFloorplanRequest = {
        name: selected.name,
        width: selected.width,
        height: selected.height,
        gridSize: selected.gridSize,
        version: selected.version,
        data: parsed,
      }
      const { data } = await apiClient.put<FloorplanResponse>(
        `/admin/clubs/${clubId}/floorplans/${selected.id}`,
        req
      )
      setSelected(data)
      setJsonText(JSON.stringify(data.data, null, 2))
      message.success('Схема сохранена')
      await fetchList()
    } catch (e: any) {
      if (e?.response?.status === 409) {
        message.error('Схема была изменена. Обновите страницу.')
        await fetchDetail(selected.id)
      } else {
        message.error(e?.response?.data?.message ?? 'Ошибка')
      }
    } finally {
      setSaving(false)
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
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
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
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
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
        req
      )
      message.success('Схема клонирована')
      setCloneOpen(false)
      await fetchList()
      await fetchDetail(data.id)
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
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
      // switch to first non-archived or clear
      const next = updatedList.find((f) => f.id !== selected.id && f.status !== 'ARCHIVED')
      if (next) {
        await fetchDetail(next.id)
      } else {
        setSelected(null)
        setJsonText('')
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
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
          width: '35%',
          minWidth: 260,
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
                Размер: {selected.width}×{selected.height}
              </span>
              <span>Сетка: {selected.gridSize}</span>
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
                <Button
                  type="primary"
                  loading={saving}
                  onClick={onSave}
                >
                  Сохранить
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

              {selected.status === 'DRAFT' && (
                <Popconfirm
                  title="Архивировать схему?"
                  description="Схема будет недоступна для редактирования."
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

              {selected.status === 'PUBLISHED' && (
                <Popconfirm
                  title="Архивировать опубликованную схему?"
                  description="Опубликованная схема будет архивирована и перестанет быть активной."
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

            {/* JSON editor */}
            <div style={{ flex: 1, padding: 20, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Данные схемы (JSON)
              </Typography.Text>
              <Input.TextArea
                value={jsonText}
                onChange={(e) => setJsonText(e.target.value)}
                readOnly={!isEditable}
                disabled={selected.status === 'ARCHIVED'}
                style={{
                  flex: 1,
                  fontFamily: 'monospace',
                  fontSize: 12,
                  resize: 'none',
                  minHeight: 300,
                  background: isEditable ? undefined : '#fafafa',
                }}
                autoSize={false}
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
            <Form.Item name="gridSize" label="Сетка" style={{ marginBottom: 0 }}>
              <InputNumber min={1} style={{ width: 100 }} />
            </Form.Item>
          </Space>
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
