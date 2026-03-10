import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Button,
  DatePicker,
  Input,
  message,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { AuditLogResponse } from '../../types'

// --- Справочник действий ---

const ACTION_LABELS: Record<string, string> = {
  SEAT_CREATE: 'Создание места',
  SEAT_UPDATE: 'Изменение места',
  SEAT_ARCHIVE: 'Архивирование места',
  FLOORPLAN_CREATE: 'Создание схемы',
  FLOORPLAN_UPDATE: 'Изменение схемы',
  FLOORPLAN_CLONE: 'Клонирование схемы',
  FLOORPLAN_PUBLISH: 'Публикация схемы',
  FLOORPLAN_UNPUBLISH: 'Снятие публикации',
  FLOORPLAN_UNPUBLISH_AUTO: 'Авто-снятие публикации',
  FLOORPLAN_ARCHIVE: 'Архивирование схемы',
  CLUB_PRODUCT_UPSERT: 'Изменение товара',
  CLUB_PRODUCT_UNLINK: 'Удаление товара',
  PERMISSION_OVERRIDE_SET: 'Изменение прав',
  PERMISSION_OVERRIDE_DELETE: 'Сброс прав',
  CLUB_USER_BLOCK: 'Блокировка',
  CLUB_USER_UNBLOCK: 'Разблокировка',
  CHECKOUT_PAID: 'Оплата заказа',
  STAFF_ADD: 'Добавление сотрудника',
  STAFF_REMOVE: 'Удаление сотрудника',
  CLUB_SETTINGS_UPDATE: 'Изменение настроек',
}

const ACTION_COLORS: Record<string, string> = {
  SEAT_CREATE: 'blue',
  SEAT_UPDATE: 'cyan',
  SEAT_ARCHIVE: 'default',
  FLOORPLAN_CREATE: 'blue',
  FLOORPLAN_UPDATE: 'cyan',
  FLOORPLAN_CLONE: 'geekblue',
  FLOORPLAN_PUBLISH: 'green',
  FLOORPLAN_UNPUBLISH: 'orange',
  FLOORPLAN_UNPUBLISH_AUTO: 'orange',
  FLOORPLAN_ARCHIVE: 'default',
  CLUB_PRODUCT_UPSERT: 'purple',
  CLUB_PRODUCT_UNLINK: 'default',
  PERMISSION_OVERRIDE_SET: 'gold',
  PERMISSION_OVERRIDE_DELETE: 'default',
  CLUB_USER_BLOCK: 'red',
  CLUB_USER_UNBLOCK: 'green',
  CHECKOUT_PAID: 'green',
  STAFF_ADD: 'blue',
  STAFF_REMOVE: 'red',
  CLUB_SETTINGS_UPDATE: 'volcano',
}

function ActionTag({ action }: { action: string }) {
  return (
    <Tag color={ACTION_COLORS[action] ?? 'default'}>
      {ACTION_LABELS[action] ?? action}
    </Tag>
  )
}

// --- Раскрытие строки ---

function JsonBlock({ label, value }: { label: string; value: unknown }) {
  if (value == null) return null
  return (
    <div style={{ marginBottom: 8 }}>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>{label}</Typography.Text>
      <pre
        style={{
          background: '#f5f5f5',
          borderRadius: 4,
          padding: '6px 10px',
          margin: '4px 0 0',
          fontSize: 12,
          maxHeight: 200,
          overflow: 'auto',
        }}
      >
        {JSON.stringify(value, null, 2)}
      </pre>
    </div>
  )
}

function ExpandedRow({ record }: { record: AuditLogResponse }) {
  const hasDiff = record.before != null || record.after != null
  if (!hasDiff) return <Typography.Text type="secondary">Нет данных изменений</Typography.Text>
  return (
    <div style={{ padding: '4px 8px' }}>
      <JsonBlock label="До" value={record.before} />
      <JsonBlock label="После" value={record.after} />
    </div>
  )
}

// --- Основной компонент ---

export default function ClubAuditPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const [logs, setLogs] = useState<AuditLogResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [actionFilter, setActionFilter] = useState<string | null>(null)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [actorSearch, setActorSearch] = useState('')

  const fetchLogs = useCallback(async () => {
    if (!clubId) return
    setLoading(true)
    try {
      const params: Record<string, string> = {}
      if (actionFilter) params.action = actionFilter
      if (dateRange) {
        params.from = dateRange[0].toISOString()
        params.to = dateRange[1].toISOString()
      }
      const res = await apiClient.get<AuditLogResponse[]>(
        `/admin/clubs/${clubId}/audit`,
        { params }
      )
      setLogs(res.data)
    } catch {
      message.error('Не удалось загрузить журнал аудита')
    } finally {
      setLoading(false)
    }
  }, [clubId, actionFilter, dateRange])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const filteredLogs = useMemo(() => {
    if (!actorSearch.trim()) return logs
    const q = actorSearch.trim().toLowerCase()
    return logs.filter(
      l =>
        (l.actorPhone ?? '').toLowerCase().includes(q) ||
        l.actorUserId.toString().includes(q)
    )
  }, [logs, actorSearch])

  const columns: ColumnsType<AuditLogResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 72,
    },
    {
      title: 'Дата',
      key: 'createdAt',
      width: 170,
      render: (_, r) => new Date(r.createdAt).toLocaleString('ru-RU'),
    },
    {
      title: 'Актор',
      key: 'actor',
      width: 160,
      render: (_, r) => r.actorPhone ?? `#${r.actorUserId}`,
    },
    {
      title: 'Действие',
      key: 'action',
      width: 210,
      render: (_, r) => <ActionTag action={r.action} />,
    },
    {
      title: 'Сущность',
      dataIndex: 'entityType',
      key: 'entityType',
      width: 160,
    },
    {
      title: 'ID сущности',
      dataIndex: 'entityId',
      key: 'entityId',
      width: 110,
      render: v => v ?? <span style={{ color: '#bbb' }}>—</span>,
    },
  ]

  const actionOptions = Object.entries(ACTION_LABELS).map(([value, label]) => ({
    value,
    label,
  }))

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Журнал аудита</h2>
        <Button icon={<ReloadOutlined />} onClick={fetchLogs} loading={loading}>
          Обновить
        </Button>
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="Актор (телефон или ID)"
          allowClear
          style={{ width: 220 }}
          value={actorSearch}
          onChange={e => setActorSearch(e.target.value)}
        />
        <Select
          style={{ width: 220 }}
          placeholder="Все действия"
          allowClear
          value={actionFilter ?? undefined}
          onChange={v => setActionFilter(v ?? null)}
          options={actionOptions}
          showSearch
          filterOption={(input, opt) =>
            (opt?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
          }
        />
        <DatePicker.RangePicker
          showTime
          onChange={val => setDateRange(val as [Dayjs, Dayjs] | null)}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={filteredLogs}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 50, showSizeChanger: false }}
        expandable={{
          expandedRowRender: record => <ExpandedRow record={record} />,
          rowExpandable: record => record.before != null || record.after != null,
        }}
        size="small"
      />
    </div>
  )
}
