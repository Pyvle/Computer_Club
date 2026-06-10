import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Spin, App } from 'antd'
import {
  LogoutOutlined,
  HistoryOutlined,
  EnvironmentOutlined,
  HeartOutlined,
  HeartFilled,
  CalendarOutlined,
  ShoppingOutlined,
  RightOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import apiClient from '../../../utils/apiClient'
import { useAuth } from '../../../contexts/AuthContext'
import SectionCard from '../../../components/ui/SectionCard'
import StatusBadge from '../../../components/ui/StatusBadge'
import EmptyState from '../../../components/ui/EmptyState'
import { tokens } from '../../../theme/tokens'
import type { ClubListItemResponse, ClientPurchaseListItem, PaymentStatus } from '../../../types'

// --- Маппинг статусов ---

const STATUS_MAP: Record<PaymentStatus, { variant: 'success' | 'warning' | 'error' | 'info' | 'default'; label: string; icon: React.ReactNode }> = {
  CREATED:  { variant: 'info',    label: 'Ожидает оплаты', icon: <ClockCircleOutlined /> },
  PAID:     { variant: 'success', label: 'Оплачен',        icon: <CheckCircleOutlined /> },
  FAILED:   { variant: 'error',   label: 'Ошибка оплаты',  icon: <ExclamationCircleOutlined /> },
  CANCELED: { variant: 'default', label: 'Отменён',        icon: null },
  REFUND:   { variant: 'warning', label: 'Возврат',        icon: null },
}

// --- Строка заказа ---

function PurchaseRow({ purchase, onClick }: { purchase: ClientPurchaseListItem; onClick: () => void }) {
  const status = STATUS_MAP[purchase.paymentStatus] ?? { variant: 'default' as const, label: purchase.paymentStatus, icon: null }
  const isPending = purchase.paymentStatus === 'CREATED'

  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        gap: 12, padding: '12px 0',
        borderBottom: `1px solid ${tokens.colors.border}`,
        cursor: 'pointer',
      }}
    >
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text, marginBottom: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {purchase.clubName}
        </div>
        <div style={{ fontSize: 12, color: tokens.colors.textSecondary }}>
          {dayjs(purchase.createdAt).format('D MMM YYYY')}
          {purchase.bookingTotalRub > 0 && (
            <span style={{ marginLeft: 6 }}>
              <CalendarOutlined style={{ marginRight: 3 }} />Бронь
            </span>
          )}
          {purchase.productsTotalRub > 0 && (
            <span style={{ marginLeft: 6 }}>
              <ShoppingOutlined style={{ marginRight: 3 }} />Товары
            </span>
          )}
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
        <span style={{ fontSize: 15, fontWeight: 700, color: isPending ? tokens.colors.info : tokens.colors.text }}>
          {purchase.totalRub.toLocaleString('ru-RU')} ₽
        </span>
        <StatusBadge label={status.label} variant={status.variant} />
      </div>
      <RightOutlined style={{ fontSize: 11, color: tokens.colors.textMuted, flexShrink: 0 }} />
    </div>
  )
}

// --- Карточка клуба в избранном ---

function FavoriteClubCard({ club, onOpen, onBook, onRemove }: {
  club: ClubListItemResponse
  onOpen: () => void
  onBook: () => void
  onRemove: () => void
}) {
  const [removing, setRemoving] = useState(false)

  async function handleRemove(e: React.MouseEvent) {
    e.stopPropagation()
    setRemoving(true)
    onRemove()
  }

  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'box-shadow 0.15s, transform 0.12s',
      }}
      onClick={onOpen}
      onMouseEnter={(e) => {
        e.currentTarget.style.boxShadow = tokens.shadow.hover
        e.currentTarget.style.transform = 'translateY(-2px)'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = 'none'
        e.currentTarget.style.transform = ''
      }}
    >
      {/* Фото */}
      <div style={{ height: 100, overflow: 'hidden', flexShrink: 0, position: 'relative' }}>
        {club.imageUrl ? (
          <img src={club.imageUrl} alt={club.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
        ) : (
          <div style={{
            width: '100%', height: '100%',
            background: `linear-gradient(135deg, ${tokens.colors.primarySoft}, ${tokens.colors.surfaceAlt})`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <EnvironmentOutlined style={{ fontSize: 28, color: tokens.colors.primary, opacity: 0.3 }} />
          </div>
        )}

        {/* Кнопка удалить из избранного */}
        <button
          onClick={handleRemove}
          disabled={removing}
          style={{
            position: 'absolute', top: 8, right: 8,
            width: 28, height: 28, borderRadius: '50%',
            background: 'rgba(255,255,255,0.9)',
            border: 'none', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            backdropFilter: 'blur(4px)',
          }}
          title="Убрать из избранного"
        >
          <HeartFilled style={{ fontSize: 13, color: tokens.colors.error }} />
        </button>
      </div>

      {/* Информация */}
      <div style={{ padding: '10px 12px' }}>
        <div style={{ fontWeight: 700, fontSize: 13, color: tokens.colors.text, marginBottom: 3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {club.name}
        </div>
        <div style={{ fontSize: 11, color: tokens.colors.textSecondary, marginBottom: 8, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          <EnvironmentOutlined style={{ marginRight: 3 }} />
          {club.address}
        </div>
        <Button
          type="primary"
          size="small"
          icon={<CalendarOutlined />}
          block
          onClick={(e) => { e.stopPropagation(); onBook() }}
        >
          Забронировать
        </Button>
      </div>
    </div>
  )
}

// --- Основной компонент ---

export default function ProfilePage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const { message } = App.useApp()

  const [purchases, setPurchases] = useState<ClientPurchaseListItem[]>([])
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set())
  const [allClubs, setAllClubs] = useState<ClubListItemResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [purchasesRes, favRes, clubsRes] = await Promise.all([
          apiClient.get<ClientPurchaseListItem[]>('/purchases').catch(() => ({ data: [] as ClientPurchaseListItem[] })),
          apiClient.get<number[]>('/me/favorites').catch(() => ({ data: [] as number[] })),
          apiClient.get<ClubListItemResponse[]>('/clubs').catch(() => ({ data: [] as ClubListItemResponse[] })),
        ])
        setPurchases(purchasesRes.data)
        setFavoriteIds(new Set(favRes.data))
        setAllClubs(clubsRes.data)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function removeFavorite(clubId: number) {
    try {
      await apiClient.delete(`/me/favorites/${clubId}`)
      setFavoriteIds((prev) => { const s = new Set(prev); s.delete(clubId); return s })
    } catch {
      message.error('Не удалось обновить избранное')
    }
  }

  function handleLogout() {
    logout()
    navigate('/')
  }

  if (!user) return null

  // производные данные
  const pendingPurchases = purchases.filter((p) => p.paymentStatus === 'CREATED')
  const recentPurchases = purchases.filter((p) => p.paymentStatus !== 'CREATED').slice(0, 4)
  const favoriteClubs = allClubs.filter((c) => favoriteIds.has(c.id))

  const totalSpent = useMemo(
    () => purchases.filter((p) => p.paymentStatus === 'PAID').reduce((s, p) => s + p.totalRub, 0),
    [purchases]
  )

  return (
    <div style={{ maxWidth: 860 }}>
      {/* Hero-блок профиля */}
      <div style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        overflow: 'hidden',
        marginBottom: 20,
      }}>
        {/* Цветная шапка с мини-статой */}
        <div style={{
          height: 90,
          background: `linear-gradient(135deg, ${tokens.colors.primarySoft} 0%, ${tokens.colors.surfaceAlt} 100%)`,
          borderBottom: `1px solid ${tokens.colors.border}`,
          padding: '16px 24px',
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'flex-start',
          gap: 24,
        }}>
          {purchases.length > 0 && [
            { label: 'Заказов', value: purchases.length },
            { label: 'Потрачено', value: `${totalSpent.toLocaleString('ru-RU')} ₽` },
            { label: 'Избранных', value: favoriteIds.size },
          ].map(({ label, value }) => (
            <div key={label} style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 18, fontWeight: 800, color: tokens.colors.text, lineHeight: 1 }}>{value}</div>
              <div style={{ fontSize: 11, color: tokens.colors.textSecondary, marginTop: 2 }}>{label}</div>
            </div>
          ))}
        </div>

        <div style={{ padding: '0 24px 20px' }}>
          {/* Телефон и роль */}
          <div style={{ marginTop: 18, marginBottom: 16 }}>
            <div style={{ fontSize: 20, fontWeight: 700, color: tokens.colors.text, marginBottom: 4 }}>
              {user.phone ?? 'Без телефона'}
            </div>
            {user.globalRole === 'GLOBAL_ADMIN' && (
              <span style={{
                fontSize: 11, fontWeight: 600,
                color: tokens.colors.error, background: tokens.colors.errorSoft,
                padding: '2px 10px', borderRadius: 12,
              }}>
                Администратор платформы
              </span>
            )}
          </div>

          {/* Действия */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <Button icon={<ShoppingOutlined />} onClick={() => navigate('/clubs')}>
              Выбрать клуб
            </Button>
            <Button icon={<LogoutOutlined />} danger onClick={handleLogout} style={{ marginLeft: 'auto' }}>
              Выйти
            </Button>
          </div>
        </div>
      </div>

      {loading ? (
        <Spin style={{ display: 'block', margin: '48px auto' }} />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, alignItems: 'start' }}
          className="profile-layout"
        >
          {/* Левая колонка */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Требуют оплаты */}
            {pendingPurchases.length > 0 && (
              <SectionCard
                title={
                  <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <ClockCircleOutlined style={{ color: tokens.colors.info }} />
                    Ожидают оплаты
                    <span style={{
                      background: tokens.colors.infoSoft, color: tokens.colors.info,
                      fontSize: 11, fontWeight: 700, padding: '1px 7px', borderRadius: 10,
                    }}>
                      {pendingPurchases.length}
                    </span>
                  </span>
                }
              >
                {pendingPurchases.map((p) => (
                  <PurchaseRow
                    key={p.purchaseId}
                    purchase={p}
                    onClick={() => navigate(`/history/${p.purchaseId}`)}
                  />
                ))}
              </SectionCard>
            )}

            {/* Последние заказы */}
            <SectionCard
              title={
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <HistoryOutlined style={{ color: tokens.colors.textSecondary }} />
                  Последние заказы
                </span>
              }
              extra={
                purchases.length > 4 ? (
                  <Button type="link" size="small" onClick={() => navigate('/history')} style={{ padding: 0 }}>
                    Все →
                  </Button>
                ) : undefined
              }
            >
              {recentPurchases.length === 0 ? (
                <EmptyState
                  icon={<HistoryOutlined />}
                  title="Заказов пока нет"
                  description="После оформления первого заказа он появится здесь"
                  actionLabel="Выбрать клуб"
                  onAction={() => navigate('/clubs')}
                />
              ) : (
                <>
                  {recentPurchases.map((p) => (
                    <PurchaseRow
                      key={p.purchaseId}
                      purchase={p}
                      onClick={() => navigate(`/history/${p.purchaseId}`)}
                    />
                  ))}
                  <button
                    onClick={() => navigate('/history')}
                    style={{
                      display: 'block', width: '100%',
                      marginTop: 8, padding: '8px 0',
                      background: 'none', border: 'none',
                      color: tokens.colors.primary, fontSize: 13,
                      fontWeight: 500, cursor: 'pointer',
                      textAlign: 'center',
                    }}
                  >
                    Смотреть всю историю →
                  </button>
                </>
              )}
            </SectionCard>
          </div>

          {/* Правая колонка — избранное */}
          <SectionCard
            title={
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <HeartFilled style={{ color: tokens.colors.error, fontSize: 13 }} />
                Избранное
                {favoriteClubs.length > 0 && (
                  <span style={{
                    background: tokens.colors.errorSoft, color: tokens.colors.error,
                    fontSize: 11, fontWeight: 700, padding: '1px 7px', borderRadius: 10,
                  }}>
                    {favoriteClubs.length}
                  </span>
                )}
              </span>
            }
            extra={
              <Button type="link" size="small" onClick={() => navigate('/clubs')} style={{ padding: 0 }}>
                Все клубы →
              </Button>
            }
          >
            {favoriteClubs.length === 0 ? (
              <EmptyState
                icon={<HeartOutlined />}
                title="Нет избранных"
                description="Добавляйте клубы в избранное, чтобы быстро их находить"
                actionLabel="Перейти к клубам"
                onAction={() => navigate('/clubs')}
              />
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                {favoriteClubs.map((club) => (
                  <FavoriteClubCard
                    key={club.id}
                    club={club}
                    onOpen={() => navigate(`/clubs/${club.id}`)}
                    onBook={() => navigate(`/clubs/${club.id}/booking`)}
                    onRemove={() => removeFavorite(club.id)}
                  />
                ))}
              </div>
            )}
          </SectionCard>
        </div>
      )}

      <style>{`
        @media (max-width: 680px) {
          .profile-layout {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  )
}
