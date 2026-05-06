import { useEffect, useMemo, useRef, useState } from 'react'
import {
  App,
  Button,
  Col,
  Drawer,
  Empty,
  Form,
  Image,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  CheckCircleOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  EyeOutlined,
  GlobalOutlined,
  HistoryOutlined,
  LayoutOutlined,
  MessageOutlined,
  ReloadOutlined,
  ShoppingOutlined,
  StopOutlined,
  TeamOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import { BOOKING_STATUS, PAYMENT_STATUS } from '../../utils/statusMaps'
import { parseItems, type FloorItem, type WallItem } from '../../utils/floorplanUtils'
import { tokens } from '../../theme/tokens'
import type {
  AdminBookingResponse,
  AdminClubCatalogProductResponse,
  AdminPurchaseResponse,
  ClubReportStatus,
  ClubWarningResponse,
  GlobalClubDetailsResponse,
  GlobalClubFloorplanResponse,
  GlobalClubResponse,
  GlobalClubStaffDetailsResponse,
  SeatClientResponse,
} from '../../types'

type StatusFilter = 'all' | 'active' | 'inactive' | 'blocked'

const STATUS_FILTERS: { label: string; value: StatusFilter }[] = [
  { label: 'Все', value: 'all' },
  { label: 'Активные', value: 'active' },
  { label: 'Неактивные', value: 'inactive' },
  { label: 'Заблокированные', value: 'blocked' },
]

const REPORT_STATUS_META: Record<ClubReportStatus, { label: string; variant: 'success' | 'warning' | 'default' }> = {
  NEW: { label: 'Новая', variant: 'default' },
  IN_PROGRESS: { label: 'В работе', variant: 'warning' },
  RESOLVED: { label: 'Решена', variant: 'success' },
}

function QuickFilters({ active, onChange }: { active: StatusFilter; onChange: (v: StatusFilter) => void }) {
  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {STATUS_FILTERS.map((filter) => {
        const selected = filter.value === active
        return (
          <button
            key={filter.value}
            onClick={() => onChange(filter.value)}
            style={{
              padding: '4px 12px',
              borderRadius: 20,
              fontSize: 13,
              fontWeight: selected ? 600 : 400,
              cursor: 'pointer',
              border: `1px solid ${selected ? tokens.colors.primary : tokens.colors.border}`,
              background: selected ? tokens.colors.primarySoft : tokens.colors.surface,
              color: selected ? tokens.colors.primary : tokens.colors.textSecondary,
              transition: 'all 0.15s',
              outline: 'none',
            }}
          >
            {filter.label}
          </button>
        )
      })}
    </div>
  )
}

function MetricTile({ label, value }: { label: string; value: string | number }) {
  return (
    <div
      style={{
        background: tokens.colors.surfaceAlt,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        padding: '12px 14px',
      }}
    >
      <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700, color: tokens.colors.text }}>{value}</div>
    </div>
  )
}

function FieldBlock({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginBottom: 4 }}>{label}</div>
      <div style={{ fontSize: 14, color: tokens.colors.text, lineHeight: 1.5 }}>{value}</div>
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        fontSize: 12,
        fontWeight: 700,
        color: tokens.colors.textMuted,
        textTransform: 'uppercase',
        letterSpacing: '0.06em',
        marginBottom: 10,
      }}
    >
      {children}
    </div>
  )
}

function WarningCard({ warning }: { warning: ClubWarningResponse }) {
  return (
    <div
      style={{
        background: tokens.colors.warningSoft,
        border: `1px solid ${tokens.colors.warning}30`,
        borderLeft: `4px solid ${tokens.colors.warning}`,
        borderRadius: tokens.radius.md,
        padding: '10px 14px',
        display: 'flex',
        gap: 10,
        alignItems: 'flex-start',
      }}
    >
      <WarningOutlined style={{ color: tokens.colors.warning, marginTop: 2 }} />
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 13, color: tokens.colors.text, lineHeight: 1.5 }}>{warning.message}</div>
        <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 4 }}>
          {formatDateTime(warning.createdAt)}
        </div>
      </div>
    </div>
  )
}

function JsonPreview({ value }: { value: unknown }) {
  const text = value == null ? '—' : safeJson(value)
  return (
    <pre
      style={{
        margin: 0,
        padding: 12,
        background: tokens.colors.surfaceAlt,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        fontSize: 12,
        lineHeight: 1.5,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        maxHeight: 280,
        overflow: 'auto',
      }}
    >
      {text}
    </pre>
  )
}

function cellKey(col: number, row: number) {
  return `${col},${row}`
}

function wallKey(orientation: 'H' | 'V', col: number, row: number) {
  return `${orientation}:${col},${row}`
}

function FloorplanPreview({ floorplan, seats }: { floorplan: GlobalClubFloorplanResponse; seats: SeatClientResponse[] }) {
  const parsed = useMemo(() => parseItems(floorplan as any), [floorplan])
  const cols = Math.max(1, Math.floor(floorplan.width / floorplan.gridSize))
  const rows = Math.max(1, Math.floor(floorplan.height / floorplan.gridSize))
  const previewRef = useRef<HTMLDivElement | null>(null)
  const [cellPx, setCellPx] = useState(18)
  const gap = 2
  const padding = 8
  const wallThickness = 3

  const seatById = useMemo(() => new Map(seats.map((seat) => [seat.id, seat])), [seats])
  const floorMap = useMemo(() => {
    const map = new Map<string, FloorItem>()
    for (const item of parsed) {
      if (item.type === 'FLOOR') map.set(cellKey(item.col, item.row), item)
    }
    return map
  }, [parsed])
  const seatMap = useMemo(() => {
    const map = new Map<string, number>()
    for (const item of parsed) {
      if (item.type === 'SEAT') map.set(cellKey(item.col, item.row), item.seatId)
    }
    return map
  }, [parsed])

  const hasFloor = floorMap.size > 0
  const cellLeft = (col: number) => padding + col * (cellPx + gap)
  const cellTop = (row: number) => padding + row * (cellPx + gap)

  useEffect(() => {
    const container = previewRef.current
    if (!container) return

    const updateScale = () => {
      const availableWidth = Math.max(180, container.clientWidth - 20 - padding * 2)
      const availableHeight = 360
      const fitByWidth = (availableWidth - Math.max(0, (cols - 1) * gap)) / Math.max(1, cols)
      const fitByHeight = (availableHeight - Math.max(0, (rows - 1) * gap)) / Math.max(1, rows)
      const nextCellPx = Math.max(8, Math.min(18, fitByWidth, fitByHeight))
      setCellPx(nextCellPx)
    }

    updateScale()
    const observer = new ResizeObserver(updateScale)
    observer.observe(container)
    return () => observer.disconnect()
  }, [cols, rows])

  return (
    <div
      ref={previewRef}
      style={{
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        background: '#e7eaef',
        padding: 10,
        overflow: 'auto',
        maxHeight: 380,
      }}
    >
      <div style={{ position: 'relative', display: 'inline-block', padding }}>
        <div style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, ${cellPx}px)`, gap }}>
          {Array.from({ length: rows * cols }, (_, index) => {
            const row = Math.floor(index / cols)
            const col = index % cols
            const key = cellKey(col, row)
            const floorItem = floorMap.get(key)
            const seatId = seatMap.get(key) ?? null
            const seat = seatId != null ? seatById.get(seatId) : null
            const isVoid = hasFloor && !floorItem
            const isVipSeat = seat?.type === 'VIP'

            return (
              <div
                key={key}
                title={seat ? `${seat.label}${isVipSeat ? ' (VIP)' : ''}` : undefined}
                style={{
                  width: cellPx,
                  height: cellPx,
                  borderRadius: 4,
                  border: `1px solid ${
                    isVoid ? '#d2d6dd' : seat ? (isVipSeat ? '#d48806' : tokens.colors.success) : '#cbd5e1'
                  }`,
                  background: isVoid
                    ? '#e7eaef'
                    : seat
                      ? (isVipSeat ? '#fff3cd' : tokens.colors.successSoft)
                      : floorItem?.roomType === 'VIP'
                        ? '#fff8e6'
                        : '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxSizing: 'border-box',
                  color: '#374151',
                  fontSize: 9,
                  fontWeight: 700,
                  overflow: 'hidden',
                }}
              >
                {seat ? seat.label.replace(/^(.{0,3}).*$/, '$1') : ''}
              </div>
            )
          })}
        </div>

        <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
          {parsed.filter((item): item is WallItem => item.type === 'WALL').map((wall) =>
            wall.orientation === 'H' ? (
              <div
                key={wallKey(wall.orientation, wall.col, wall.row)}
                style={{
                  position: 'absolute',
                  left: cellLeft(wall.col),
                  top: cellTop(wall.row) - gap / 2 - wallThickness / 2,
                  width: cellPx,
                  height: wallThickness,
                  background: '#1f2937',
                  borderRadius: 2,
                }}
              />
            ) : (
              <div
                key={wallKey(wall.orientation, wall.col, wall.row)}
                style={{
                  position: 'absolute',
                  left: cellLeft(wall.col) - gap / 2 - wallThickness / 2,
                  top: cellTop(wall.row),
                  width: wallThickness,
                  height: cellPx,
                  background: '#1f2937',
                  borderRadius: 2,
                }}
              />
            )
          )}
        </div>
      </div>
    </div>
  )
}

function renderPermissionTags(values: string[]) {
  if (values.length === 0) {
    return <span style={{ color: tokens.colors.textMuted }}>—</span>
  }

  return (
    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
      {values.map((value) => (
        <Tag key={value} style={{ marginInlineEnd: 0 }}>
          {value}
        </Tag>
      ))}
    </div>
  )
}

function renderStatusBadge(club: GlobalClubResponse | GlobalClubDetailsResponse) {
  if (club.isBlocked) return <StatusBadge label="Заблокирован" variant="error" />
  if (club.isActive) return <StatusBadge label="Активен" variant="success" />
  return <StatusBadge label="Неактивен" variant="default" />
}

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format('DD.MM.YYYY HH:mm') : '—'
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('DD.MM.YYYY') : '—'
}

function formatMoney(value: number) {
  return `${value.toLocaleString('ru-RU')} ₽`
}

function resolveImage(url?: string | null) {
  if (!url) return null
  return url.startsWith('/') ? url : url
}

function safeJson(value: unknown) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function buildStaffColumns(): ColumnsType<GlobalClubStaffDetailsResponse> {
  return [
    {
      title: 'Сотрудник',
      key: 'user',
      width: 220,
      render: (_, row) => (
        <div>
          <div style={{ fontWeight: 600, color: tokens.colors.text }}>{row.phone ?? `Пользователь #${row.userId}`}</div>
          <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>ID #{row.userId}</div>
        </div>
      ),
    },
    {
      title: 'Роль',
      dataIndex: 'role',
      width: 120,
      render: (role) => <StatusBadge label={role === 'OWNER' ? 'Владелец' : 'Администратор'} variant={role === 'OWNER' ? 'warning' : 'info'} />,
    },
    {
      title: 'Добавлен',
      width: 220,
      render: (_, row) => (
        <div>
          <div>{formatDateTime(row.addedAt)}</div>
          <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
            {row.addedByPhone ? `Добавил ${row.addedByPhone}` : 'Без автора'}
          </div>
        </div>
      ),
    },
    {
      title: 'Эффективные права',
      key: 'permissions',
      render: (_, row) => renderPermissionTags(row.effectivePermissions),
    },
    {
      title: 'Переопределения',
      key: 'overrides',
      render: (_, row) =>
        row.overrides.length === 0 ? (
          <span style={{ color: tokens.colors.textMuted }}>—</span>
        ) : (
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {row.overrides.map((override) => (
              <Tag key={`${row.userId}-${override.permission}`} color={override.granted ? 'green' : 'red'} style={{ marginInlineEnd: 0 }}>
                {override.permission}: {override.granted ? 'allow' : 'deny'}
              </Tag>
            ))}
          </div>
        ),
    },
  ]
}

function buildProductColumns(categoryTitleById: Map<number, string>): ColumnsType<AdminClubCatalogProductResponse> {
  return [
    {
      title: 'Товар',
      key: 'product',
      render: (_, row) => (
        <div>
          <div style={{ fontWeight: 600, color: tokens.colors.text }}>{row.productTitle}</div>
          <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
            {categoryTitleById.get(row.categoryId) ?? `Категория #${row.categoryId}`}
          </div>
        </div>
      ),
    },
    {
      title: 'Глобально',
      width: 120,
      render: (_, row) => <StatusBadge label={row.productIsActive ? 'Активен' : 'Выключен'} variant={row.productIsActive ? 'success' : 'default'} />,
    },
    {
      title: 'В клубе',
      width: 120,
      render: (_, row) => <StatusBadge label={row.isLinkedToClub ? 'Подключен' : 'Не подключен'} variant={row.isLinkedToClub ? 'info' : 'default'} />,
    },
    {
      title: 'Доступность',
      width: 120,
      render: (_, row) => {
        if (!row.isLinkedToClub) return <span style={{ color: tokens.colors.textMuted }}>—</span>
        return <StatusBadge label={row.clubIsAvailable ? 'Доступен' : 'Скрыт'} variant={row.clubIsAvailable ? 'success' : 'default'} />
      },
    },
    {
      title: 'Цена',
      width: 120,
      render: (_, row) => (row.clubPriceRub != null ? formatMoney(row.clubPriceRub) : <span style={{ color: tokens.colors.textMuted }}>—</span>),
    },
  ]
}

function buildFloorplanColumns(): ColumnsType<GlobalClubFloorplanResponse> {
  return [
    {
      title: 'Схема',
      dataIndex: 'name',
      render: (name: string, row) => (
        <div>
          <div style={{ fontWeight: 600, color: tokens.colors.text }}>{name}</div>
          <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>ID #{row.id}</div>
        </div>
      ),
    },
    {
      title: 'Статус',
      width: 130,
      render: (_, row) => <StatusBadge label={row.status} variant={row.status === 'PUBLISHED' ? 'success' : row.status === 'DRAFT' ? 'warning' : 'default'} />,
    },
    {
      title: 'Размер',
      width: 130,
      render: (_, row) => `${row.width} × ${row.height}`,
    },
    {
      title: 'Сетка',
      width: 90,
      dataIndex: 'gridSize',
    },
    {
      title: 'Элементов',
      width: 110,
      dataIndex: 'itemCount',
    },
    {
      title: 'Версия',
      width: 90,
      dataIndex: 'version',
    },
    {
      title: 'Обновлена',
      width: 150,
      render: (_, row) => formatDateTime(row.updatedAt),
    },
  ]
}

function buildBookingColumns(): ColumnsType<AdminBookingResponse> {
  return [
    {
      title: 'Бронь',
      width: 90,
      render: (_, row) => `#${row.id}`,
    },
    {
      title: 'Пользователь',
      dataIndex: 'userPhone',
      render: (phone: string | null, row) => phone ?? `Пользователь #${row.userId}`,
    },
    {
      title: 'Статус',
      width: 130,
      render: (_, row) => {
        const meta = BOOKING_STATUS[row.status]
        return <StatusBadge label={meta?.label ?? row.status} variant={meta?.variant ?? 'default'} />
      },
    },
    {
      title: 'Время',
      width: 220,
      render: (_, row) => `${formatDateTime(row.startAt)} - ${dayjs(row.endAt).format('HH:mm')}`,
    },
    {
      title: 'Места',
      render: (_, row) => row.seatLabels.join(', '),
    },
    {
      title: 'Сумма',
      width: 120,
      render: (_, row) => formatMoney(row.totalRub),
    },
  ]
}

function buildPurchaseColumns(): ColumnsType<AdminPurchaseResponse> {
  return [
    {
      title: 'Покупка',
      width: 90,
      render: (_, row) => `#${row.id}`,
    },
    {
      title: 'Пользователь',
      dataIndex: 'userPhone',
      render: (phone: string | null, row) => phone ?? `Пользователь #${row.userId}`,
    },
    {
      title: 'Статус',
      width: 150,
      render: (_, row) => {
        const meta = PAYMENT_STATUS[row.paymentStatus]
        return <StatusBadge label={meta?.label ?? row.paymentStatus} variant={meta?.variant ?? 'default'} />
      },
    },
    {
      title: 'Создана',
      width: 150,
      render: (_, row) => formatDateTime(row.createdAt),
    },
    {
      title: 'Места',
      render: (_, row) => row.seatLabels.length ? row.seatLabels.join(', ') : <span style={{ color: tokens.colors.textMuted }}>—</span>,
    },
    {
      title: 'Товаров',
      width: 90,
      dataIndex: 'productCount',
    },
    {
      title: 'Сумма',
      width: 120,
      render: (_, row) => formatMoney(row.totalAmountRub),
    },
  ]
}

function ClubDetailsDrawer({
  club,
  loading,
  open,
  onClose,
  onBlock,
  onUnblock,
  onWarn,
}: {
  club: GlobalClubDetailsResponse | null
  loading: boolean
  open: boolean
  onClose: () => void
  onBlock: (club: GlobalClubDetailsResponse) => void
  onUnblock: (club: GlobalClubDetailsResponse) => void
  onWarn: (club: GlobalClubDetailsResponse) => void
}) {
  const categoryTitleById = useMemo(
    () => new Map((club?.catalog.categories ?? []).map((category) => [category.id, category.title])),
    [club]
  )
  const activeFloorplan = useMemo(
    () => club?.floorplans.find((floorplan) => floorplan.status === 'PUBLISHED') ?? null,
    [club]
  )
  const recentReports = useMemo(() => (club?.reports ?? []).slice(0, 5), [club])
  const linkedProducts = useMemo(
    () => (club?.catalog.products ?? []).filter((product) => product.isLinkedToClub),
    [club]
  )
  const previewSeats = useMemo<SeatClientResponse[]>(
    () => (club?.seats ?? []).map((seat) => ({ id: seat.id, label: seat.label, type: seat.type })),
    [club]
  )

  const overviewWarnings = club?.warnings ?? []
  const staffColumns = useMemo(() => buildStaffColumns(), [])
  const productColumns = useMemo(() => buildProductColumns(categoryTitleById), [categoryTitleById])
  const floorplanColumns = useMemo(() => buildFloorplanColumns(), [])
  const bookingColumns = useMemo(() => buildBookingColumns(), [])
  const purchaseColumns = useMemo(() => buildPurchaseColumns(), [])

  return (
    <Drawer
      open={open}
      onClose={onClose}
      width={1180}
      destroyOnClose={false}
      title={club ? club.name : 'Клуб'}
      extra={
        club ? (
          <Space wrap>
            <Button icon={<WarningOutlined />} onClick={() => onWarn(club)}>
              Предупреждение
            </Button>
            {club.isBlocked ? (
              <Button icon={<CheckCircleOutlined />} onClick={() => onUnblock(club)}>
                Разблокировать
              </Button>
            ) : (
              <Button danger icon={<StopOutlined />} onClick={() => onBlock(club)}>
                Заблокировать
              </Button>
            )}
          </Space>
        ) : null
      }
    >
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '64px 0' }}>
          <Spin size="large" />
        </div>
      ) : !club ? (
        <Empty description="Не удалось загрузить данные клуба" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'minmax(240px, 320px) minmax(0, 1fr)',
              gap: 20,
              alignItems: 'start',
            }}
          >
            <div>
              {club.imageUrl ? (
                <Image
                  src={resolveImage(club.imageUrl) ?? undefined}
                  preview={false}
                  style={{
                    width: '100%',
                    height: 220,
                    objectFit: 'cover',
                    borderRadius: tokens.radius.lg,
                    border: `1px solid ${tokens.colors.border}`,
                  }}
                />
              ) : (
                <div
                  style={{
                    height: 220,
                    borderRadius: tokens.radius.lg,
                    border: `1px solid ${tokens.colors.border}`,
                    background: tokens.colors.surfaceAlt,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: tokens.colors.textMuted,
                    fontSize: 34,
                  }}
                >
                  <GlobalOutlined />
                </div>
              )}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
                <div>
                  <div style={{ fontSize: 24, fontWeight: 800, color: tokens.colors.text }}>{club.name}</div>
                  <div style={{ fontSize: 14, color: tokens.colors.textSecondary, marginTop: 4 }}>{club.addressShort}</div>
                </div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  {renderStatusBadge(club)}
                  {club.blockReason && club.isBlocked ? <StatusBadge label="Требует внимания" variant="error" /> : null}
                </div>
              </div>

              {club.isBlocked && club.blockReason ? (
                <div
                  style={{
                    background: tokens.colors.errorSoft,
                    border: `1px solid ${tokens.colors.error}30`,
                    borderLeft: `4px solid ${tokens.colors.error}`,
                    borderRadius: tokens.radius.md,
                    padding: '10px 14px',
                    color: tokens.colors.error,
                    fontSize: 13,
                  }}
                >
                  <span style={{ fontWeight: 700 }}>Причина блокировки: </span>
                  {club.blockReason}
                </div>
              ) : null}

              <Row gutter={[12, 12]}>
                <Col xs={12} md={8}><MetricTile label="Места" value={`${club.stats.activeSeats} / ${club.stats.totalSeats}`} /></Col>
                <Col xs={12} md={8}><MetricTile label="Персонал" value={`${club.stats.ownersCount} владелец, ${club.stats.adminsCount} админ`} /></Col>
                <Col xs={12} md={8}><MetricTile label="Выручка" value={formatMoney(club.stats.paidRevenueRub)} /></Col>
                <Col xs={12} md={8}><MetricTile label="Схемы" value={`${club.stats.publishedFloorplans} / ${club.stats.floorplansTotal}`} /></Col>
                <Col xs={12} md={8}><MetricTile label="Каталог" value={`${club.stats.availableCatalogItems} / ${club.stats.linkedCatalogItems}`} /></Col>
                <Col xs={12} md={8}><MetricTile label="Жалобы" value={`${club.stats.reportsNewCount + club.stats.reportsInProgressCount} активных`} /></Col>
              </Row>
            </div>
          </div>

          <Tabs
            items={[
              {
                key: 'overview',
                label: 'Обзор',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                        gap: 14,
                      }}
                    >
                      <FieldBlock label="Полный адрес" value={club.addressFull} />
                      <FieldBlock label="Как найти вход" value={club.locationText ?? '—'} />
                      <FieldBlock label="Координаты" value={club.latitude != null && club.longitude != null ? `${club.latitude}, ${club.longitude}` : '—'} />
                      <FieldBlock label="Дата создания" value={formatDate(club.createdAt)} />
                      <FieldBlock label="Последнее обновление" value={formatDateTime(club.updatedAt)} />
                      <FieldBlock label="ID клуба" value={`#${club.id}`} />
                    </div>

                    <div>
                      <SectionTitle>Описание</SectionTitle>
                      <div
                        style={{
                          padding: '12px 14px',
                          background: tokens.colors.surfaceAlt,
                          border: `1px solid ${tokens.colors.border}`,
                          borderRadius: tokens.radius.md,
                          color: tokens.colors.text,
                          lineHeight: 1.6,
                        }}
                      >
                        {club.description ?? 'Описание не заполнено'}
                      </div>
                    </div>

                    <div>
                      <SectionTitle>Активная схема клуба</SectionTitle>
                      {activeFloorplan ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                          <div
                            style={{
                              display: 'grid',
                              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                              gap: 12,
                            }}
                          >
                            <MetricTile label="Название" value={activeFloorplan.name} />
                            <MetricTile label="Размер" value={`${activeFloorplan.width} × ${activeFloorplan.height}`} />
                            <MetricTile label="Сетка" value={activeFloorplan.gridSize} />
                            <MetricTile label="Элементов" value={activeFloorplan.itemCount} />
                          </div>
                          <div>
                            <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginBottom: 6 }}>
                              Обновлена {formatDateTime(activeFloorplan.updatedAt)}
                            </div>
                            <FloorplanPreview floorplan={activeFloorplan} seats={previewSeats} />
                          </div>
                        </div>
                      ) : (
                        <Empty description="Опубликованной схемы нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                      )}
                    </div>

                    <div>
                      <SectionTitle>Последние жалобы пользователей ({club.reports.length})</SectionTitle>
                      {recentReports.length === 0 ? (
                        <Empty description="Жалоб нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                          {recentReports.map((report) => {
                            const meta = REPORT_STATUS_META[report.status]
                            return (
                              <div
                                key={report.id}
                                style={{
                                  border: `1px solid ${tokens.colors.border}`,
                                  borderRadius: tokens.radius.md,
                                  padding: '12px 14px',
                                  background: tokens.colors.surfaceAlt,
                                }}
                              >
                                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap', marginBottom: 8 }}>
                                  <div style={{ fontWeight: 600, color: tokens.colors.text }}>
                                    {report.userPhone ?? `Пользователь #${report.userId}`}
                                  </div>
                                  <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                                    <StatusBadge label={meta.label} variant={meta.variant} />
                                    <span style={{ fontSize: 12, color: tokens.colors.textMuted }}>{formatDateTime(report.createdAt)}</span>
                                  </div>
                                </div>
                                <div style={{ fontSize: 14, lineHeight: 1.6, color: tokens.colors.text }}>{report.message}</div>
                              </div>
                            )
                          })}
                        </div>
                      )}
                    </div>

                    <div>
                      <SectionTitle>Дашборд клуба</SectionTitle>
                      <Row gutter={[12, 12]}>
                        <Col xs={12} md={8}><MetricTile label="Активные брони" value={club.dashboard.activeBookingsCount} /></Col>
                        <Col xs={12} md={8}><MetricTile label="Брони сегодня" value={club.dashboard.upcomingTodayCount} /></Col>
                        <Col xs={12} md={8}><MetricTile label="Занятые места" value={`${club.dashboard.occupiedSeats} / ${club.dashboard.totalSeats}`} /></Col>
                        <Col xs={12} md={8}><MetricTile label="Выручка сегодня" value={formatMoney(club.dashboard.todayRevenueRub)} /></Col>
                        <Col xs={12} md={8}><MetricTile label="Выручка за неделю" value={club.dashboard.weekRevenueRub != null ? formatMoney(club.dashboard.weekRevenueRub) : '—'} /></Col>
                        <Col xs={12} md={8}><MetricTile label="Выручка за месяц" value={club.dashboard.monthRevenueRub != null ? formatMoney(club.dashboard.monthRevenueRub) : '—'} /></Col>
                      </Row>
                    </div>

                    <div>
                      <SectionTitle>Предупреждения платформы ({overviewWarnings.length})</SectionTitle>
                      {overviewWarnings.length === 0 ? (
                        <Empty description="Предупреждений нет" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                          {overviewWarnings.map((warning) => <WarningCard key={warning.id} warning={warning} />)}
                        </div>
                      )}
                    </div>
                  </div>
                ),
              },
              {
                key: 'staff',
                label: 'Команда и доступ',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div>
                      <SectionTitle>Персонал ({club.staff.length})</SectionTitle>
                      <Table
                        rowKey="userId"
                        size="small"
                        pagination={{ pageSize: 5 }}
                        scroll={{ x: 980 }}
                        columns={staffColumns}
                        dataSource={club.staff}
                      />
                    </div>

                    <div>
                      <SectionTitle>Блокировки пользователей ({club.blocks.length})</SectionTitle>
                      <Table
                        rowKey="userId"
                        size="small"
                        pagination={{ pageSize: 5 }}
                        scroll={{ x: 900 }}
                        columns={[
                          {
                            title: 'Пользователь',
                            render: (_, row) => row.phone ?? `Пользователь #${row.userId}`,
                          },
                          {
                            title: 'Статус',
                            width: 120,
                            render: (_, row) => <StatusBadge label={row.isBlocked ? 'Заблокирован' : 'Снят'} variant={row.isBlocked ? 'error' : 'default'} />,
                          },
                          {
                            title: 'Причина',
                            dataIndex: 'reason',
                            render: (value) => value ?? <span style={{ color: tokens.colors.textMuted }}>—</span>,
                          },
                          {
                            title: 'До',
                            width: 160,
                            render: (_, row) => formatDateTime(row.blockedUntil),
                          },
                          {
                            title: 'Кем',
                            width: 170,
                            render: (_, row) => row.blockedByPhone ?? <span style={{ color: tokens.colors.textMuted }}>—</span>,
                          },
                        ]}
                        dataSource={club.blocks}
                      />
                    </div>
                  </div>
                ),
              },
              {
                key: 'setup',
                label: 'Клуб и зал',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div>
                      <SectionTitle>Цены по типам мест</SectionTitle>
                      <Table
                        rowKey="seatType"
                        size="small"
                        pagination={false}
                        columns={[
                          { title: 'Тип', dataIndex: 'seatType', width: 140 },
                          { title: 'Цена в час', render: (_, row) => formatMoney(row.pricePerHourRub) },
                        ]}
                        dataSource={club.seatPrices}
                      />
                    </div>

                    <div>
                      <SectionTitle>Характеристики мест</SectionTitle>
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 12 }}>
                        {club.seatSpecs.length === 0 ? (
                          <div style={{ color: tokens.colors.textMuted }}>Характеристики не заполнены</div>
                        ) : (
                          club.seatSpecs.map((spec) => (
                            <div
                              key={spec.seatType}
                              style={{
                                border: `1px solid ${tokens.colors.border}`,
                                borderRadius: tokens.radius.md,
                                padding: '12px 14px',
                                background: tokens.colors.surfaceAlt,
                              }}
                            >
                              <div style={{ fontWeight: 700, color: tokens.colors.text, marginBottom: 8 }}>
                                {spec.seatType} • {spec.title}
                              </div>
                              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                                {spec.specs.map((line, idx) => (
                                  <div key={`${spec.seatType}-${idx}`} style={{ display: 'flex', gap: 8 }}>
                                    <span style={{ color: tokens.colors.textMuted }}>{line.name}:</span>
                                    <span style={{ color: tokens.colors.text }}>{line.value}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>

                    <div>
                      <SectionTitle>Места ({club.seats.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 8 }}
                        columns={[
                          { title: 'ID', dataIndex: 'id', width: 80 },
                          { title: 'Метка', dataIndex: 'label', width: 140 },
                          { title: 'Тип', dataIndex: 'type', width: 120 },
                          { title: 'Статус', width: 120, render: (_, row) => <StatusBadge label={row.isActive ? 'Активно' : 'Выключено'} variant={row.isActive ? 'success' : 'default'} /> },
                          { title: 'Порядок', dataIndex: 'sortOrder', width: 110 },
                        ]}
                        dataSource={club.seats}
                      />
                    </div>

                    <div>
                      <SectionTitle>Тарифы ({club.timePackages.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 6 }}
                        scroll={{ x: 900 }}
                        columns={[
                          { title: 'Название', dataIndex: 'name' },
                          { title: 'Часы', dataIndex: 'hours', width: 90 },
                          { title: 'Цена/час', width: 120, render: (_, row) => formatMoney(row.pricePerHourRub) },
                          { title: 'Итог', width: 120, render: (_, row) => formatMoney(row.totalPriceRub) },
                          { title: 'Окно', width: 180, render: (_, row) => row.availableFrom || row.availableTo ? `${row.availableFrom ?? '00:00'} - ${row.availableTo ?? '23:59'}` : 'Без ограничений' },
                          { title: 'Статус', width: 120, render: (_, row) => <StatusBadge label={row.isActive ? 'Активен' : 'Выключен'} variant={row.isActive ? 'success' : 'default'} /> },
                        ]}
                        dataSource={club.timePackages}
                      />
                    </div>

                    <div>
                      <SectionTitle>Схемы зала ({club.floorplans.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 6 }}
                        scroll={{ x: 900 }}
                        columns={floorplanColumns}
                        expandable={{
                          expandedRowRender: (row) => <FloorplanPreview floorplan={row} seats={previewSeats} />,
                        }}
                        dataSource={club.floorplans}
                      />
                    </div>
                  </div>
                ),
              },
              {
                key: 'catalog',
                label: 'Каталог',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
                      <MetricTile label="Категорий" value={club.catalog.categories.length} />
                      <MetricTile label="Подключено к клубу" value={linkedProducts.length} />
                      <MetricTile label="Доступно в клубе" value={club.stats.availableCatalogItems} />
                      <MetricTile label="Скрыто в клубе" value={linkedProducts.filter((product) => !product.clubIsAvailable).length} />
                    </div>

                    <div>
                      <SectionTitle>Подключенные товары</SectionTitle>
                      <Table
                        rowKey="productId"
                        size="small"
                        pagination={{ pageSize: 8 }}
                        scroll={{ x: 980 }}
                        columns={productColumns}
                        dataSource={linkedProducts}
                      />
                    </div>
                  </div>
                ),
              },
              {
                key: 'activity',
                label: 'Активность',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                    <div>
                      <SectionTitle>Бронирования ({club.bookings.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 5 }}
                        scroll={{ x: 980 }}
                        columns={bookingColumns}
                        dataSource={club.bookings}
                      />
                    </div>

                    <div>
                      <SectionTitle>Покупки ({club.purchases.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 5 }}
                        scroll={{ x: 980 }}
                        columns={purchaseColumns}
                        dataSource={club.purchases}
                      />
                    </div>

                    <div>
                      <SectionTitle>Аудит ({club.audit.length})</SectionTitle>
                      <Table
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 5 }}
                        scroll={{ x: 980 }}
                        columns={[
                          { title: 'Когда', width: 160, render: (_, row) => formatDateTime(row.createdAt) },
                          { title: 'Кто', width: 180, render: (_, row) => row.actorPhone ?? `Пользователь #${row.actorUserId}` },
                          { title: 'Действие', width: 210, dataIndex: 'action' },
                          { title: 'Сущность', width: 180, render: (_, row) => `${row.entityType}${row.entityId ? ` • ${row.entityId}` : ''}` },
                        ]}
                        expandable={{
                          expandedRowRender: (row) => (
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                              <div>
                                <SectionTitle>До</SectionTitle>
                                <JsonPreview value={row.before} />
                              </div>
                              <div>
                                <SectionTitle>После</SectionTitle>
                                <JsonPreview value={row.after} />
                              </div>
                            </div>
                          ),
                        }}
                        dataSource={club.audit}
                      />
                    </div>
                  </div>
                ),
              },
            ]}
          />
        </div>
      )}
    </Drawer>
  )
}

export default function GlobalClubsPage() {
  const { message } = App.useApp()
  const [clubs, setClubs] = useState<GlobalClubResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')

  const [viewOpen, setViewOpen] = useState(false)
  const [viewClubId, setViewClubId] = useState<number | null>(null)
  const [clubDetails, setClubDetails] = useState<GlobalClubDetailsResponse | null>(null)
  const [detailsLoading, setDetailsLoading] = useState(false)

  const [blockModalOpen, setBlockModalOpen] = useState(false)
  const [blockTarget, setBlockTarget] = useState<GlobalClubDetailsResponse | null>(null)
  const [blockSubmitting, setBlockSubmitting] = useState(false)
  const [blockForm] = Form.useForm<{ reason: string }>()

  const [warnModalOpen, setWarnModalOpen] = useState(false)
  const [warnTarget, setWarnTarget] = useState<GlobalClubDetailsResponse | null>(null)
  const [warnSubmitting, setWarnSubmitting] = useState(false)
  const [warnForm] = Form.useForm<{ message: string }>()

  async function fetchClubs() {
    setLoading(true)
    try {
      const { data } = await apiClient.get<GlobalClubResponse[]>('/admin/global/clubs')
      setClubs(data)
    } catch {
      message.error('Не удалось загрузить клубы')
    } finally {
      setLoading(false)
    }
  }

  async function fetchClubDetails(clubId: number) {
    setDetailsLoading(true)
    try {
      const { data } = await apiClient.get<GlobalClubDetailsResponse>(`/admin/global/clubs/${clubId}`)
      setClubDetails(data)
      setViewClubId(clubId)
      return data
    } catch {
      setClubDetails(null)
      message.error('Не удалось загрузить полные данные клуба')
      return null
    } finally {
      setDetailsLoading(false)
    }
  }

  useEffect(() => {
    fetchClubs()
  }, [])

  async function openView(club: GlobalClubResponse) {
    setViewOpen(true)
    setViewClubId(club.id)
    await fetchClubDetails(club.id)
  }

  function openBlock(club: GlobalClubDetailsResponse) {
    setBlockTarget(club)
    blockForm.setFieldsValue({ reason: club.blockReason ?? '' })
    setBlockModalOpen(true)
  }

  async function submitBlock(values: { reason: string }) {
    if (!blockTarget) return
    setBlockSubmitting(true)
    try {
      await apiClient.put(`/admin/global/clubs/${blockTarget.id}/block`, { reason: values.reason || null })
      message.success('Клуб заблокирован')
      setBlockModalOpen(false)
      await fetchClubs()
      if (viewClubId === blockTarget.id) {
        await fetchClubDetails(blockTarget.id)
      }
    } catch {
      message.error('Не удалось заблокировать клуб')
    } finally {
      setBlockSubmitting(false)
    }
  }

  async function submitUnblock(club: GlobalClubDetailsResponse) {
    try {
      await apiClient.put(`/admin/global/clubs/${club.id}/unblock`, {})
      message.success('Блокировка снята')
      await fetchClubs()
      if (viewClubId === club.id) {
        await fetchClubDetails(club.id)
      }
    } catch {
      message.error('Не удалось снять блокировку')
    }
  }

  function openWarn(club: GlobalClubDetailsResponse) {
    setWarnTarget(club)
    warnForm.resetFields()
    setWarnModalOpen(true)
  }

  async function submitWarn(values: { message: string }) {
    if (!warnTarget) return
    setWarnSubmitting(true)
    try {
      await apiClient.post(`/admin/global/clubs/${warnTarget.id}/warnings`, { message: values.message })
      message.success('Предупреждение отправлено')
      setWarnModalOpen(false)
      if (viewClubId === warnTarget.id) {
        await fetchClubDetails(warnTarget.id)
      }
    } catch {
      message.error('Не удалось отправить предупреждение')
    } finally {
      setWarnSubmitting(false)
    }
  }

  const filteredClubs = useMemo(() => {
    let result = clubs
    if (statusFilter === 'active') result = result.filter((club) => !club.isBlocked && club.isActive)
    if (statusFilter === 'inactive') result = result.filter((club) => !club.isBlocked && !club.isActive)
    if (statusFilter === 'blocked') result = result.filter((club) => club.isBlocked)
    if (search.trim()) {
      const query = search.trim().toLowerCase()
      result = result.filter(
        (club) =>
          club.name.toLowerCase().includes(query) ||
          club.addressShort.toLowerCase().includes(query) ||
          club.addressFull.toLowerCase().includes(query)
      )
    }
    return result
  }, [clubs, search, statusFilter])

  const stats = useMemo(() => ({
    total: clubs.length,
    active: clubs.filter((club) => !club.isBlocked && club.isActive).length,
    inactive: clubs.filter((club) => !club.isBlocked && !club.isActive).length,
    blocked: clubs.filter((club) => club.isBlocked).length,
  }), [clubs])

  const columns: ColumnsType<GlobalClubResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 70,
      sorter: (a, b) => a.id - b.id,
    },
    {
      title: 'Клуб',
      key: 'club',
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>{record.name}</div>
          <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>{record.addressShort}</div>
        </div>
      ),
    },
    {
      title: 'Статус',
      width: 150,
      render: (_, record) => renderStatusBadge(record),
    },
    {
      title: 'Жалобы',
      width: 110,
      sorter: (a, b) => a.reportsCount - b.reportsCount,
      render: (_, record) => (
        <Tag color={record.reportsCount > 0 ? 'orange' : 'default'} style={{ marginInlineEnd: 0 }}>
          {record.reportsCount}
        </Tag>
      ),
    },
    {
      title: 'Создан',
      width: 120,
      render: (_, record) => formatDate(record.createdAt),
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Button size="small" icon={<EyeOutlined />} onClick={(event) => { event.stopPropagation(); openView(record) }}>
          Открыть
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Клубы платформы"
        subtitle="Полный read-only обзор всех клубов платформы"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchClubs} loading={loading}>
            Обновить
          </Button>
        }
      />

      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col xs={12} sm={6}>
          <StatCard label="Всего клубов" value={stats.total} icon={<GlobalOutlined />} />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard label="Активные" value={stats.active} icon={<CheckOutlined />} accentColor={tokens.colors.success} />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard label="Неактивные" value={stats.inactive} icon={<CloseOutlined />} accentColor={tokens.colors.textMuted} />
        </Col>
        <Col xs={12} sm={6}>
          <StatCard label="Заблокированные" value={stats.blocked} icon={<StopOutlined />} accentColor={tokens.colors.error} />
        </Col>
      </Row>

      <SectionCard style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <QuickFilters active={statusFilter} onChange={setStatusFilter} />
          <Input.Search
            placeholder="Поиск по названию или адресу"
            allowClear
            style={{ maxWidth: 360 }}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            size="small"
          />
        </div>
      </SectionCard>

      <SectionCard noPadding>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredClubs}
          loading={loading}
          pagination={{ pageSize: 20 }}
          onRow={(record) => ({
            style: { cursor: 'pointer' },
            onClick: () => openView(record),
          })}
          locale={{
            emptyText: (
              <EmptyState
                icon={<GlobalOutlined />}
                title="Клубов нет"
                description={statusFilter !== 'all' || search ? 'Нет клубов по выбранным фильтрам' : 'Пока нет зарегистрированных клубов'}
              />
            ),
          }}
        />
      </SectionCard>

      <ClubDetailsDrawer
        club={clubDetails}
        loading={detailsLoading}
        open={viewOpen}
        onClose={() => setViewOpen(false)}
        onBlock={openBlock}
        onUnblock={submitUnblock}
        onWarn={openWarn}
      />

      <Modal
        open={blockModalOpen}
        title={`Заблокировать: ${blockTarget?.name ?? ''}`}
        footer={null}
        onCancel={() => setBlockModalOpen(false)}
      >
        <Form layout="vertical" form={blockForm} onFinish={submitBlock} style={{ marginTop: 12 }}>
          <Form.Item name="reason" label="Причина блокировки">
            <Input.TextArea rows={3} placeholder="Причина блокировки клуба на платформе" />
          </Form.Item>
          <Button type="primary" danger htmlType="submit" block loading={blockSubmitting}>
            Заблокировать
          </Button>
        </Form>
      </Modal>

      <Modal
        open={warnModalOpen}
        title={`Предупреждение: ${warnTarget?.name ?? ''}`}
        footer={null}
        onCancel={() => setWarnModalOpen(false)}
      >
        <Form layout="vertical" form={warnForm} onFinish={submitWarn} style={{ marginTop: 12 }}>
          <Form.Item
            name="message"
            label="Текст предупреждения"
            rules={[{ required: true, message: 'Введите текст предупреждения' }]}
          >
            <Input.TextArea rows={4} placeholder="Что платформа просит исправить в клубе" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={warnSubmitting}>
            Отправить предупреждение
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
