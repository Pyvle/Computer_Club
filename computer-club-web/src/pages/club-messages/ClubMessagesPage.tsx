import { useEffect, useState } from 'react'
import { Tabs, Select, DatePicker, Button, Space, Dropdown, App, Tag } from 'antd'
import {
  WarningOutlined,
  UserOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  MoreOutlined,
} from '@ant-design/icons'
import { useParams } from 'react-router-dom'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { ClubUserReportResponse, ClubReportStatus, PlatformMessageResponse } from '../../types'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import { tokens } from '../../theme/tokens'

// --- Маппинг статусов жалоб ---

const REPORT_STATUS: Record<ClubReportStatus, { label: string; color: string; bg: string; icon: React.ReactNode }> = {
  NEW:         { label: 'Новая',          color: tokens.colors.error,   bg: tokens.colors.errorSoft,   icon: <ExclamationCircleOutlined /> },
  IN_PROGRESS: { label: 'В работе',       color: tokens.colors.warning, bg: tokens.colors.warningSoft, icon: <ClockCircleOutlined /> },
  RESOLVED:    { label: 'Обработана',     color: tokens.colors.success, bg: tokens.colors.successSoft, icon: <CheckCircleOutlined /> },
}

const STATUS_OPTIONS: { label: string; value: ClubReportStatus | '' }[] = [
  { label: 'Все статусы',   value: '' },
  { label: 'Новые',         value: 'NEW' },
  { label: 'В работе',      value: 'IN_PROGRESS' },
  { label: 'Обработанные',  value: 'RESOLVED' },
]

// --- Бейдж статуса жалобы ---

function ReportStatusBadge({ status }: { status: ClubReportStatus }) {
  const s = REPORT_STATUS[status]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        padding: '3px 10px',
        borderRadius: 20,
        fontSize: 12,
        fontWeight: 600,
        background: s.bg,
        color: s.color,
        whiteSpace: 'nowrap',
      }}
    >
      {s.icon}
      {s.label}
    </span>
  )
}

// --- Карточка жалобы пользователя ---

interface ReportCardProps {
  report: ClubUserReportResponse
  onStatusChange: (id: number, status: ClubReportStatus) => void
  updating: boolean
}

function ReportCard({ report, onStatusChange, updating }: ReportCardProps) {
  const nextStatuses: { label: string; value: ClubReportStatus }[] = (
    ['NEW', 'IN_PROGRESS', 'RESOLVED'] as ClubReportStatus[]
  )
    .filter((s) => s !== report.status)
    .map((s) => ({ label: REPORT_STATUS[s].label, value: s }))

  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderLeft: `4px solid ${REPORT_STATUS[report.status].color}`,
        borderRadius: tokens.radius.md,
        padding: '14px 16px',
        display: 'flex',
        gap: 12,
        alignItems: 'flex-start',
      }}
    >
      {/* Аватар-заглушка */}
      <div
        style={{
          width: 36,
          height: 36,
          borderRadius: '50%',
          background: tokens.colors.primarySoft,
          color: tokens.colors.primary,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 16,
          flexShrink: 0,
        }}
      >
        <UserOutlined />
      </div>

      {/* Контент */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 6 }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
            {report.userPhone ?? `Пользователь #${report.userId}`}
          </span>
          <ReportStatusBadge status={report.status} />
          <span style={{ fontSize: 12, color: tokens.colors.textMuted, marginLeft: 'auto' }}>
            {dayjs(report.createdAt).format('DD.MM.YYYY HH:mm')}
          </span>
        </div>
        <p style={{ margin: 0, fontSize: 14, color: tokens.colors.textSecondary, lineHeight: 1.5 }}>
          {report.message}
        </p>
      </div>

      {/* Быстрое изменение статуса */}
      <Dropdown
        disabled={updating}
        menu={{
          items: nextStatuses.map((s) => ({
            key: s.value,
            label: s.label,
            onClick: () => onStatusChange(report.id, s.value),
          })),
        }}
        trigger={['click']}
      >
        <Button
          type="text"
          size="small"
          icon={<MoreOutlined />}
          loading={updating}
          style={{ color: tokens.colors.textMuted, flexShrink: 0 }}
        />
      </Dropdown>
    </div>
  )
}

// --- Карточка сообщения от платформы ---

function PlatformMessageCard({ msg }: { msg: PlatformMessageResponse }) {
  return (
    <div
      style={{
        background: tokens.colors.warningSoft,
        border: `1px solid ${tokens.colors.warning}30`,
        borderLeft: `4px solid ${tokens.colors.warning}`,
        borderRadius: tokens.radius.md,
        padding: '14px 16px',
        display: 'flex',
        gap: 12,
        alignItems: 'flex-start',
      }}
    >
      <div
        style={{
          width: 36,
          height: 36,
          borderRadius: '50%',
          background: `${tokens.colors.warning}20`,
          color: tokens.colors.warning,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 18,
          flexShrink: 0,
        }}
      >
        <WarningOutlined />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.warning }}>
            Администрация платформы
          </span>
          <span style={{ fontSize: 12, color: tokens.colors.textMuted, marginLeft: 'auto' }}>
            {dayjs(msg.createdAt).format('DD.MM.YYYY HH:mm')}
          </span>
        </div>
        <p style={{ margin: 0, fontSize: 14, color: tokens.colors.text, lineHeight: 1.5 }}>
          {msg.message}
        </p>
      </div>
    </div>
  )
}

// --- Основной компонент ---

export default function ClubMessagesPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()

  const [userReports, setUserReports] = useState<ClubUserReportResponse[]>([])
  const [platformMessages, setPlatformMessages] = useState<PlatformMessageResponse[]>([])
  const [reportsLoading, setReportsLoading] = useState(true)
  const [platformLoading, setPlatformLoading] = useState(true)
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  // фильтры жалоб
  const [statusFilter, setStatusFilter] = useState<ClubReportStatus | ''>('')
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null)

  function fetchReports() {
    if (!clubId) return
    setReportsLoading(true)
    const params: Record<string, string> = {}
    if (statusFilter) params.status = statusFilter
    apiClient
      .get<ClubUserReportResponse[]>(`/admin/clubs/${clubId}/user-reports`, { params })
      .then(({ data }) => setUserReports(data))
      .catch(() => message.error('Не удалось загрузить жалобы'))
      .finally(() => setReportsLoading(false))
  }

  function fetchPlatformMessages() {
    if (!clubId) return
    setPlatformLoading(true)
    apiClient
      .get<PlatformMessageResponse[]>(`/admin/clubs/${clubId}/platform-messages`)
      .then(({ data }) => setPlatformMessages(data))
      .catch(() => message.error('Не удалось загрузить сообщения платформы'))
      .finally(() => setPlatformLoading(false))
  }

  useEffect(() => { fetchReports() }, [clubId, statusFilter])
  useEffect(() => { fetchPlatformMessages() }, [clubId])

  async function handleStatusChange(reportId: number, status: ClubReportStatus) {
    if (!clubId) return
    setUpdatingId(reportId)
    try {
      const { data } = await apiClient.patch<ClubUserReportResponse>(
        `/admin/clubs/${clubId}/user-reports/${reportId}/status`,
        { status }
      )
      setUserReports((prev) => prev.map((r) => (r.id === reportId ? data : r)))
      message.success('Статус обновлён')
    } catch {
      message.error('Не удалось обновить статус')
    } finally {
      setUpdatingId(null)
    }
  }

  // клиентская фильтрация по дате поверх серверной
  const filteredReports = dateRange
    ? userReports.filter((r) => {
        const d = dayjs(r.createdAt)
        return d.isAfter(dateRange[0].startOf('day')) && d.isBefore(dateRange[1].endOf('day'))
      })
    : userReports

  const newCount = userReports.filter((r) => r.status === 'NEW').length

  const reportsTab = (
    <div>
      {/* Панель фильтров */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          marginBottom: 16,
          flexWrap: 'wrap',
          alignItems: 'center',
        }}
      >
        <Select
          value={statusFilter}
          onChange={setStatusFilter}
          options={STATUS_OPTIONS}
          style={{ width: 160 }}
          size="small"
        />
        <DatePicker.RangePicker
          size="small"
          value={dateRange}
          onChange={(v) => setDateRange(v as [Dayjs, Dayjs] | null)}
          format="DD.MM.YYYY"
          allowClear
          placeholder={['От', 'До']}
        />
        <Button
          size="small"
          icon={<ReloadOutlined />}
          onClick={fetchReports}
          loading={reportsLoading}
        >
          Обновить
        </Button>
        {(statusFilter || dateRange) && (
          <Button
            size="small"
            type="link"
            onClick={() => { setStatusFilter(''); setDateRange(null) }}
          >
            Сбросить
          </Button>
        )}
      </div>

      {/* Список */}
      {reportsLoading ? (
        <div style={{ textAlign: 'center', padding: 32, color: tokens.colors.textMuted }}>
          Загрузка...
        </div>
      ) : filteredReports.length === 0 ? (
        <EmptyState
          icon={<UserOutlined />}
          title="Жалоб нет"
          description="Когда пользователи отправят жалобы, они появятся здесь"
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {filteredReports.map((r) => (
            <ReportCard
              key={r.id}
              report={r}
              onStatusChange={handleStatusChange}
              updating={updatingId === r.id}
            />
          ))}
        </div>
      )}
    </div>
  )

  const platformTab = (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
        <Button
          size="small"
          icon={<ReloadOutlined />}
          onClick={fetchPlatformMessages}
          loading={platformLoading}
        >
          Обновить
        </Button>
      </div>
      {platformLoading ? (
        <div style={{ textAlign: 'center', padding: 32, color: tokens.colors.textMuted }}>
          Загрузка...
        </div>
      ) : platformMessages.length === 0 ? (
        <EmptyState
          icon={<WarningOutlined />}
          title="Нет сообщений от платформы"
          description="Уведомления и предупреждения от администрации платформы будут отображаться здесь"
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {platformMessages.map((m) => (
            <PlatformMessageCard key={m.id} msg={m} />
          ))}
        </div>
      )}
    </div>
  )

  return (
    <div>
      <PageHeader
        title="Сообщения"
        subtitle="Жалобы пользователей и уведомления от администрации платформы"
      />
      <SectionCard>
        <Tabs
          items={[
            {
              key: 'reports',
              label: (
                <Space size={6}>
                  <UserOutlined />
                  От пользователей
                  {newCount > 0 && (
                    <Tag color="red" style={{ margin: 0, fontSize: 11 }}>
                      {newCount}
                    </Tag>
                  )}
                </Space>
              ),
              children: reportsTab,
            },
            {
              key: 'platform',
              label: (
                <Space size={6}>
                  <WarningOutlined />
                  От платформы
                  {platformMessages.length > 0 && (
                    <Tag color="orange" style={{ margin: 0, fontSize: 11 }}>
                      {platformMessages.length}
                    </Tag>
                  )}
                </Space>
              ),
              children: platformTab,
            },
          ]}
        />
      </SectionCard>
    </div>
  )
}
