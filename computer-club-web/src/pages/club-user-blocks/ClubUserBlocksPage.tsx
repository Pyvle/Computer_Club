import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Tag, Popconfirm, Modal, Form, Input,
  DatePicker, Checkbox, Typography, Space, message, Steps,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { ClubUserBlockView, UpsertClubUserBlockRequest, UserLookupResult } from '../../types'

const { Text } = Typography

function fmtDate(iso: string | null): string {
  if (!iso) return '—'
  return dayjs(iso).format('DD.MM.YYYY HH:mm')
}

// --- Модалка блокировки ---

interface BlockModalProps {
  clubId: number
  open: boolean
  // если передан — режим редактирования существующей записи
  existing?: ClubUserBlockView
  onClose: () => void
  onDone: () => void
}

function BlockModal({ clubId, open, existing, onClose, onDone }: BlockModalProps) {
  const [step, setStep] = useState(0)
  const [phoneInput, setPhoneInput] = useState('')
  const [lookupLoading, setLookupLoading] = useState(false)
  const [foundUser, setFoundUser] = useState<UserLookupResult | null>(null)
  const [indefinite, setIndefinite] = useState(true)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ reason: string }>()

  // в режиме edit сразу на шаг формы
  const isEdit = !!existing

  useEffect(() => {
    if (!open) return
    if (isEdit) {
      setStep(2)
      setFoundUser({ userId: existing!.userId, phone: existing!.phone, role: '' })
      setIndefinite(!existing!.blockedUntil)
      form.setFieldsValue({ reason: existing!.reason ?? '' })
    } else {
      setStep(0)
      setPhoneInput('')
      setFoundUser(null)
      setIndefinite(true)
      form.resetFields()
    }
  }, [open, isEdit, existing, form])

  async function handleLookup() {
    const phone = phoneInput.trim()
    if (!phone) return
    setLookupLoading(true)
    try {
      const res = await apiClient.get<UserLookupResult>(
        `/admin/clubs/${clubId}/admins/users/by-phone`,
        { params: { phone } }
      )
      setFoundUser(res.data)
      setStep(1)
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Пользователь не найден')
    } finally {
      setLookupLoading(false)
    }
  }

  async function handleSave() {
    let values: { reason: string }
    try { values = await form.validateFields() } catch { return }

    if (!foundUser) return
    setSaving(true)
    try {
      const blockedUntil = indefinite
        ? null
        : (form.getFieldValue('blockedUntil') as dayjs.Dayjs | null)?.toISOString() ?? null

      const req: UpsertClubUserBlockRequest = {
        isBlocked: true,
        reason: values.reason || null,
        blockedUntil,
      }
      await apiClient.put(`/admin/clubs/${clubId}/user-blocks/${foundUser.userId}`, req)
      message.success(isEdit ? 'Блокировка обновлена' : 'Пользователь заблокирован')
      onDone()
      onClose()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Не удалось сохранить блокировку')
    } finally {
      setSaving(false)
    }
  }

  const title = isEdit
    ? `Изменить блокировку: ${existing!.phone ?? `ID ${existing!.userId}`}`
    : 'Заблокировать пользователя'

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onClose}
      footer={null}
      width={480}
      destroyOnClose
    >
      {!isEdit && (
        <Steps
          size="small"
          current={step}
          style={{ marginBottom: 24 }}
          items={[
            { title: 'Поиск' },
            { title: 'Пользователь' },
            { title: 'Блокировка' },
          ]}
        />
      )}

      {/* Шаг 0 — поиск по телефону */}
      {step === 0 && (
        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder="+79991234567"
            value={phoneInput}
            onChange={e => setPhoneInput(e.target.value)}
            onPressEnter={handleLookup}
          />
          <Button type="primary" loading={lookupLoading} onClick={handleLookup}>
            Найти
          </Button>
        </Space.Compact>
      )}

      {/* Шаг 1 — подтверждение пользователя */}
      {step === 1 && foundUser && (
        <div>
          <p style={{ marginBottom: 4 }}>Телефон: <strong>{foundUser.phone ?? '—'}</strong></p>
          <p style={{ marginBottom: 0 }}>ID: <strong>{foundUser.userId}</strong></p>
          <Space style={{ marginTop: 16, width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={() => setStep(0)}>Назад</Button>
            <Button type="primary" onClick={() => setStep(2)}>Далее</Button>
          </Space>
        </div>
      )}

      {/* Шаг 2 — форма блокировки */}
      {step === 2 && (
        <Form form={form} layout="vertical">
          <Form.Item name="reason" label="Причина">
            <Input.TextArea rows={3} placeholder="Опционально" />
          </Form.Item>
          <Form.Item>
            <Checkbox
              checked={indefinite}
              onChange={e => setIndefinite(e.target.checked)}
            >
              Бессрочно
            </Checkbox>
          </Form.Item>
          {!indefinite && (
            <Form.Item name="blockedUntil" label="Заблокирован до">
              <DatePicker
                showTime
                format="DD.MM.YYYY HH:mm"
                style={{ width: '100%' }}
                disabledDate={d => d.isBefore(dayjs(), 'day')}
              />
            </Form.Item>
          )}
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            {!isEdit && <Button onClick={() => setStep(1)}>Назад</Button>}
            <Button onClick={onClose}>Отмена</Button>
            <Button type="primary" danger loading={saving} onClick={handleSave}>
              {isEdit ? 'Сохранить' : 'Заблокировать'}
            </Button>
          </Space>
        </Form>
      )}
    </Modal>
  )
}

// --- Основная страница ---

export default function ClubUserBlocksPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const id = Number(clubId)

  const [blocks, setBlocks] = useState<ClubUserBlockView[]>([])
  const [loading, setLoading] = useState(false)
  const [blockModalOpen, setBlockModalOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ClubUserBlockView | undefined>()

  const fetchBlocks = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apiClient.get<ClubUserBlockView[]>(`/admin/clubs/${id}/user-blocks`)
      setBlocks(res.data)
    } catch {
      message.error('Не удалось загрузить блокировки')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { fetchBlocks() }, [fetchBlocks])

  async function handleUnblock(userId: number) {
    try {
      await apiClient.put(`/admin/clubs/${id}/user-blocks/${userId}`, {
        isBlocked: false,
        reason: null,
        blockedUntil: null,
      } satisfies UpsertClubUserBlockRequest)
      message.success('Пользователь разблокирован')
      fetchBlocks()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message ?? 'Не удалось разблокировать')
    }
  }

  function openEdit(record: ClubUserBlockView) {
    setEditTarget(record)
    setBlockModalOpen(true)
  }

  function openNew() {
    setEditTarget(undefined)
    setBlockModalOpen(true)
  }

  const columns: ColumnsType<ClubUserBlockView> = [
    {
      title: 'Пользователь',
      render: (_: unknown, r: ClubUserBlockView) => r.phone ?? `ID ${r.userId}`,
    },
    {
      title: 'Статус',
      width: 130,
      render: (_: unknown, r: ClubUserBlockView) => {
        const active = r.isBlocked && (!r.blockedUntil || dayjs(r.blockedUntil).isAfter(dayjs()))
        return active
          ? <Tag color="red">Заблокирован</Tag>
          : <Tag color="green">Активен</Tag>
      },
    },
    {
      title: 'Причина',
      dataIndex: 'reason',
      render: (v: string | null) => v ?? <Text type="secondary">—</Text>,
    },
    {
      title: 'До',
      dataIndex: 'blockedUntil',
      width: 140,
      render: (v: string | null, r: ClubUserBlockView) =>
        r.isBlocked ? (v ? fmtDate(v) : 'Бессрочно') : <Text type="secondary">—</Text>,
    },
    {
      title: 'Кто заблокировал',
      width: 160,
      render: (_: unknown, r: ClubUserBlockView) => {
        if (!r.blockedByUserId) return <Text type="secondary">—</Text>
        return r.blockedByPhone ?? `ID ${r.blockedByUserId}`
      },
    },
    {
      title: 'Создана',
      dataIndex: 'createdAt',
      width: 130,
      render: fmtDate,
    },
    {
      title: 'Обновлена',
      dataIndex: 'updatedAt',
      width: 130,
      render: fmtDate,
    },
    {
      title: 'Действия',
      width: 180,
      render: (_: unknown, r: ClubUserBlockView) => {
        const isActive = r.isBlocked && (!r.blockedUntil || dayjs(r.blockedUntil).isAfter(dayjs()))
        return (
          <Space>
            {isActive && (
              <Button size="small" onClick={() => openEdit(r)}>
                Изменить
              </Button>
            )}
            {isActive && (
              <Popconfirm
                title="Разблокировать пользователя?"
                okText="Разблокировать"
                cancelText="Отмена"
                onConfirm={() => handleUnblock(r.userId)}
              >
                <Button size="small">Разблокировать</Button>
              </Popconfirm>
            )}
            {!isActive && (
              <Button size="small" danger onClick={() => {
                setEditTarget(undefined)
                // открываем модалку с шагом поиска, но пользователь уже известен — сразу на шаг 2
                setEditTarget({ ...r, isBlocked: false })
                setBlockModalOpen(true)
              }}>
                Заблокировать снова
              </Button>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>Блокировки пользователей</Typography.Title>
        <Button type="primary" danger onClick={openNew}>
          Заблокировать
        </Button>
      </div>

      <Table
        rowKey="userId"
        dataSource={blocks}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: false }}
      />

      <BlockModal
        clubId={id}
        open={blockModalOpen}
        existing={editTarget}
        onClose={() => { setBlockModalOpen(false); setEditTarget(undefined) }}
        onDone={fetchBlocks}
      />
    </div>
  )
}
