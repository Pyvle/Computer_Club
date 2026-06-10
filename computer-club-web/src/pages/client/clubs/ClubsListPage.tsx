import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Col, Input, Row, Segmented, Spin, App } from 'antd'
import {
  HeartOutlined,
  HeartFilled,
  UnorderedListOutlined,
  EnvironmentOutlined,
  CalendarOutlined,
} from '@ant-design/icons'
import apiClient from '../../../utils/apiClient'
import { filterClubs } from '../../../utils/clubsFilters'
import { useAuth } from '../../../contexts/AuthContext'
import ClubsMap from '../../../components/ClubsMap'
import PageHeader from '../../../components/ui/PageHeader'
import EmptyState from '../../../components/ui/EmptyState'
import { tokens } from '../../../theme/tokens'
import type { ClubListItemResponse } from '../../../types'

type ViewMode = 'list' | 'map'

// Обрезает описание до нужной длины
function excerpt(text: string | null | undefined, max = 90): string | null {
  if (!text) return null
  return text.length > max ? text.slice(0, max).trimEnd() + '…' : text
}

// Отдельный компонент карточки клуба
function ClubCard({
  club,
  isFav,
  onFav,
  onDetails,
  onBook,
}: {
  club: ClubListItemResponse
  isFav: boolean
  onFav: () => void
  onDetails: () => void
  onBook: () => void
}) {
  const blocked = club.isBlocked ?? false
  const desc = excerpt(club.description)

  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${blocked ? tokens.colors.error : tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        overflow: 'hidden',
        opacity: blocked ? 0.65 : 1,
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        transition: 'box-shadow 0.2s, transform 0.15s',
      }}
      onMouseEnter={(e) => {
        if (!blocked) {
          e.currentTarget.style.boxShadow = tokens.shadow.hover
          e.currentTarget.style.transform = 'translateY(-2px)'
        }
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = tokens.shadow.card
        e.currentTarget.style.transform = ''
      }}
    >
      {/* Фото */}
      <div
        style={{ position: 'relative', height: 190, flexShrink: 0, cursor: blocked ? 'default' : 'pointer' }}
        onClick={blocked ? undefined : onDetails}
      >
        {club.imageUrl ? (
          <img
            src={club.imageUrl}
            alt={club.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
          />
        ) : (
          <div style={{
            width: '100%', height: '100%',
            background: `linear-gradient(135deg, ${tokens.colors.primarySoft} 0%, ${tokens.colors.surfaceAlt} 100%)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <EnvironmentOutlined style={{ fontSize: 36, color: tokens.colors.primary, opacity: 0.4 }} />
          </div>
        )}

        {/* Бейдж "заблокирован" */}
        {blocked && (
          <div style={{
            position: 'absolute', top: 10, left: 10,
            background: tokens.colors.error, color: '#fff',
            fontSize: 11, fontWeight: 700, padding: '3px 10px', borderRadius: 20,
            letterSpacing: '0.04em', textTransform: 'uppercase',
          }}>
            Заблокирован
          </div>
        )}

        {/* Кнопка избранного */}
        <button
          onClick={(e) => { e.stopPropagation(); onFav() }}
          style={{
            position: 'absolute', top: 10, right: 10,
            width: 34, height: 34, borderRadius: '50%',
            background: 'rgba(255,255,255,0.92)',
            border: 'none', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            backdropFilter: 'blur(4px)',
            transition: 'background 0.15s',
          }}
          title={isFav ? 'Убрать из избранного' : 'Добавить в избранное'}
        >
          {isFav
            ? <HeartFilled style={{ fontSize: 16, color: tokens.colors.error }} />
            : <HeartOutlined style={{ fontSize: 16, color: tokens.colors.textSecondary }} />}
        </button>
      </div>

      {/* Контент */}
      <div style={{ padding: '14px 16px 0', flex: 1, cursor: blocked ? 'default' : 'pointer' }} onClick={blocked ? undefined : onDetails}>
        {/* Название и цена в одной строке */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8, marginBottom: 4 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: tokens.colors.text, lineHeight: 1.3 }}>
            {club.name}
          </div>
          {club.minPricePerHourRub != null && !blocked && (
            <div style={{ flexShrink: 0, textAlign: 'right' }}>
              <span style={{ fontSize: 11, color: tokens.colors.textMuted }}>от </span>
              <span style={{ fontSize: 15, fontWeight: 800, color: tokens.colors.primary }}>
                {club.minPricePerHourRub}
              </span>
              <span style={{ fontSize: 10, color: tokens.colors.textMuted }}> ₽/ч</span>
            </div>
          )}
        </div>

        <div style={{ fontSize: 13, color: tokens.colors.textSecondary, marginBottom: 4, display: 'flex', alignItems: 'flex-start', gap: 4 }}>
          <EnvironmentOutlined style={{ marginTop: 2, flexShrink: 0 }} />
          <span>{club.address}</span>
        </div>

        {club.locationText && (
          <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginBottom: 4 }}>
            {club.locationText}
          </div>
        )}

        {desc && (
          <div style={{ fontSize: 13, color: tokens.colors.textSecondary, marginTop: 8, lineHeight: 1.5 }}>
            {desc}
          </div>
        )}
      </div>

      {/* Действия */}
      <div style={{
        padding: '12px 16px',
        display: 'flex',
        gap: 8,
        borderTop: `1px solid ${tokens.colors.border}`,
        marginTop: 14,
        background: tokens.colors.surfaceAlt,
      }}>
        <Button
          size="small"
          block
          disabled={blocked}
          onClick={onDetails}
          style={{ flex: 1 }}
        >
          Подробнее
        </Button>
        <Button
          type="primary"
          size="small"
          block
          disabled={blocked}
          icon={<CalendarOutlined />}
          onClick={onBook}
          style={{ flex: 1 }}
        >
          Забронировать
        </Button>
      </div>
    </div>
  )
}

// --- Главный компонент ---

export default function ClubsListPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()

  const [clubs, setClubs] = useState<ClubListItemResponse[]>([])
  const [favorites, setFavorites] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [onlyFavorites, setOnlyFavorites] = useState(false)
  const [viewMode, setViewMode] = useState<ViewMode>('list')

  const isLoggedIn = user !== null

  useEffect(() => {
    async function load() {
      setLoading(true)
      try {
        if (isLoggedIn) {
          try {
            // авторизованным показываем /available — содержит флаг isBlocked
            const { data } = await apiClient.get<ClubListItemResponse[]>('/clubs/available')
            setClubs(data)
          } catch {
            const { data } = await apiClient.get<ClubListItemResponse[]>('/clubs')
            setClubs(data)
          }
        } else {
          const { data } = await apiClient.get<ClubListItemResponse[]>('/clubs')
          setClubs(data)
        }
      } catch {
        message.error('Не удалось загрузить список клубов')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [isLoggedIn]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!isLoggedIn) return
    apiClient.get<number[]>('/me/favorites')
      .then(({ data }) => setFavorites(new Set(data)))
      .catch(() => {})
  }, [isLoggedIn])

  const filtered = useMemo(() => {
    return filterClubs(clubs, search, onlyFavorites, favorites)
  }, [clubs, search, onlyFavorites, favorites])

  async function toggleFavorite(clubId: number, isFav: boolean) {
    if (!isLoggedIn) {
      navigate(`/login?from=/`)
      return
    }
    try {
      if (isFav) {
        await apiClient.delete(`/me/favorites/${clubId}`)
        setFavorites((prev) => { const s = new Set(prev); s.delete(clubId); return s })
      } else {
        await apiClient.put(`/me/favorites/${clubId}`)
        setFavorites((prev) => new Set(prev).add(clubId))
      }
    } catch {
      message.error('Не удалось обновить избранное')
    }
  }

  function resetFilters() {
    setSearch('')
    setOnlyFavorites(false)
  }

  const hasActiveFilter = search.trim().length > 0 || onlyFavorites

  if (loading) return <Spin style={{ display: 'block', margin: '64px auto' }} />

  return (
    <div>
      <PageHeader
        title="Компьютерные клубы"
        subtitle="Выберите клуб, чтобы посмотреть свободные места, тарифы и товары"
      />

      {/* Панель фильтров */}
      <div style={{
        display: 'flex',
        gap: 10,
        marginBottom: 24,
        flexWrap: 'wrap',
        alignItems: 'center',
        padding: '12px 16px',
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.md,
        boxShadow: tokens.shadow.card,
      }}>
        <Input.Search
          placeholder="Поиск по названию или адресу"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
          style={{ flex: 1, minWidth: 200, maxWidth: 380 }}
        />

        {isLoggedIn && (
          <Button
            type={onlyFavorites ? 'primary' : 'default'}
            icon={onlyFavorites ? <HeartFilled /> : <HeartOutlined />}
            onClick={() => setOnlyFavorites((v) => !v)}
          >
            Избранные
          </Button>
        )}

        <Segmented
          value={viewMode}
          onChange={(v) => setViewMode(v as ViewMode)}
          options={[
            { value: 'list', icon: <UnorderedListOutlined />, label: 'Список' },
            { value: 'map',  icon: <EnvironmentOutlined />,   label: 'Карта' },
          ]}
          style={{ marginLeft: 'auto' }}
        />
      </div>

      {/* Карта */}
      {viewMode === 'map' && (
        <ClubsMap
          clubs={filtered}
          favorites={favorites}
          onBook={(clubId) => navigate(`/clubs/${clubId}/booking`)}
        />
      )}

      {/* Список */}
      {viewMode === 'list' && (
        filtered.length === 0 ? (
          <EmptyState
            icon={<EnvironmentOutlined />}
            title={onlyFavorites ? 'Нет избранных клубов' : 'Клубы не найдены'}
            description={
              onlyFavorites
                ? 'Добавляйте клубы в избранное, нажимая на сердечко'
                : hasActiveFilter
                  ? 'Попробуйте изменить поисковый запрос'
                  : 'Клубы пока не добавлены'
            }
            actionLabel={hasActiveFilter ? 'Сбросить фильтры' : undefined}
            onAction={hasActiveFilter ? resetFilters : undefined}
          />
        ) : (
          <Row gutter={[20, 20]}>
            {filtered.map((club) => (
              <Col key={club.id} xs={24} sm={12} lg={8}>
                <ClubCard
                  club={club}
                  isFav={favorites.has(club.id)}
                  onFav={() => toggleFavorite(club.id, favorites.has(club.id))}
                  onDetails={() => navigate(`/clubs/${club.id}`)}
                  onBook={() => navigate(`/clubs/${club.id}/booking`)}
                />
              </Col>
            ))}
          </Row>
        )
      )}
    </div>
  )
}
