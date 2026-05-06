import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Col, Row, Spin, App, Tag } from 'antd'
import {
  EnvironmentOutlined,
  HeartOutlined,
  HeartFilled,
  ShoppingOutlined,
  WarningOutlined,
  CalendarOutlined,
  StarOutlined,
  ClockCircleOutlined,
  LaptopOutlined,
  TeamOutlined,
  WifiOutlined,
  CoffeeOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import apiClient from '../../../utils/apiClient'
import { useAuth } from '../../../contexts/AuthContext'
import SectionCard from '../../../components/ui/SectionCard'
import { tokens } from '../../../theme/tokens'
import type {
  ClubListItemResponse,
  SeatSpecResponse,
  SeatPriceClientResponse,
  TimePackageClientResponse,
  SeatClientResponse,
  ClubProductResponse,
} from '../../../types'

// --- Hero ---

function ClubHero({
  club,
  isFav,
  isLoggedIn,
  onFav,
  onBack,
}: {
  club: ClubListItemResponse
  isFav: boolean
  isLoggedIn: boolean
  onFav: () => void
  onBack: () => void
}) {
  const isBlocked = club.isBlocked ?? false

  return (
    <div style={{
      position: 'relative',
      height: 380,
      borderRadius: tokens.radius.lg,
      overflow: 'hidden',
      marginBottom: 28,
      background: `linear-gradient(135deg, ${tokens.colors.primarySoft} 0%, ${tokens.colors.surfaceAlt} 100%)`,
    }}>
      {/* Фото */}
      {club.imageUrl ? (
        <img
          src={club.imageUrl}
          alt={club.name}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      ) : (
        <div style={{
          width: '100%', height: '100%',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <EnvironmentOutlined style={{ fontSize: 72, color: tokens.colors.primary, opacity: 0.2 }} />
        </div>
      )}

      {/* Градиентный оверлей снизу */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(to top, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0.3) 45%, transparent 70%)',
      }} />

      {/* Кнопка назад */}
      <button
        onClick={onBack}
        style={{
          position: 'absolute', top: 16, left: 16,
          width: 36, height: 36, borderRadius: '50%',
          background: 'rgba(255,255,255,0.85)',
          border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          backdropFilter: 'blur(4px)',
        }}
        title="Назад"
      >
        <ArrowLeftOutlined style={{ fontSize: 14, color: tokens.colors.text }} />
      </button>

      {/* Кнопка избранного */}
      <button
        onClick={onFav}
        style={{
          position: 'absolute', top: 16, right: 16,
          width: 36, height: 36, borderRadius: '50%',
          background: 'rgba(255,255,255,0.85)',
          border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          backdropFilter: 'blur(4px)',
        }}
        title={isFav ? 'Убрать из избранного' : 'Добавить в избранное'}
      >
        {isFav
          ? <HeartFilled style={{ fontSize: 16, color: tokens.colors.error }} />
          : <HeartOutlined style={{ fontSize: 16, color: tokens.colors.textSecondary }} />}
      </button>

      {/* Бейдж заблокирован */}
      {isBlocked && (
        <div style={{
          position: 'absolute', top: 16, left: 60,
          background: tokens.colors.error, color: '#fff',
          fontSize: 11, fontWeight: 700, padding: '3px 12px', borderRadius: 20,
          letterSpacing: '0.04em', textTransform: 'uppercase',
        }}>
          Заблокирован
        </div>
      )}

      {/* Информация внизу hero */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        padding: '24px 24px 20px',
      }}>
        <div style={{ fontSize: 28, fontWeight: 800, color: '#fff', lineHeight: 1.2, marginBottom: 6, textShadow: '0 1px 3px rgba(0,0,0,0.3)' }}>
          {club.name}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'rgba(255,255,255,0.85)' }}>
          <EnvironmentOutlined />
          <span>{club.address}</span>
          {club.locationText && (
            <span style={{ opacity: 0.7 }}>· {club.locationText}</span>
          )}
        </div>
      </div>
    </div>
  )
}

// --- Карточка действий (правая колонка) ---

function ActionCard({
  isBlocked,
  isLoggedIn,
  regularPrice,
  vipPrice,
  packages,
  regularCount,
  vipCount,
  onBook,
  onShop,
  onReport,
}: {
  isBlocked: boolean
  isLoggedIn: boolean
  regularPrice: SeatPriceClientResponse | undefined
  vipPrice: SeatPriceClientResponse | undefined
  packages: TimePackageClientResponse[]
  regularCount: number
  vipCount: number
  onBook: () => void
  onShop: () => void
  onReport: () => void
}) {
  const totalSeats = regularCount + vipCount

  return (
    <div style={{
      background: tokens.colors.surface,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.lg,
      boxShadow: tokens.shadow.card,
      overflow: 'hidden',
    }}>
      {/* Цены */}
      {(regularPrice || vipPrice) && (
        <div style={{ padding: '16px 20px', borderBottom: `1px solid ${tokens.colors.border}` }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: tokens.colors.textMuted, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 10 }}>
            Стоимость
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            {regularPrice && (
              <div style={{
                flex: 1, padding: '10px 12px',
                background: tokens.colors.surfaceAlt,
                borderRadius: tokens.radius.sm,
                textAlign: 'center',
              }}>
                <div style={{ fontSize: 11, color: tokens.colors.textSecondary, marginBottom: 2 }}>Стандарт</div>
                <div style={{ fontSize: 22, fontWeight: 800, color: tokens.colors.text }}>
                  {regularPrice.pricePerHourRub}
                  <span style={{ fontSize: 13, fontWeight: 500, color: tokens.colors.textSecondary }}> ₽/ч</span>
                </div>
              </div>
            )}
            {vipPrice && (
              <div style={{
                flex: 1, padding: '10px 12px',
                background: '#FFFBEB',
                border: `1px solid #FDE68A`,
                borderRadius: tokens.radius.sm,
                textAlign: 'center',
              }}>
                <div style={{ fontSize: 11, color: tokens.colors.warning, fontWeight: 600, marginBottom: 2 }}>VIP</div>
                <div style={{ fontSize: 22, fontWeight: 800, color: tokens.colors.warning }}>
                  {vipPrice.pricePerHourRub}
                  <span style={{ fontSize: 13, fontWeight: 500 }}> ₽/ч</span>
                </div>
              </div>
            )}
          </div>
          {!regularPrice && !vipPrice && (
            <div style={{ fontSize: 13, color: tokens.colors.textMuted }}>Цены уточняются</div>
          )}
        </div>
      )}

      {/* Пакеты времени */}
      {packages.length > 0 && (
        <div style={{ padding: '14px 20px', borderBottom: `1px solid ${tokens.colors.border}` }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: tokens.colors.textMuted, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 10 }}>
            Пакеты времени
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {packages.map((pkg) => (
              <div key={pkg.id} style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '6px 10px',
                background: tokens.colors.surfaceAlt,
                borderRadius: tokens.radius.sm,
                fontSize: 13,
              }}>
                <div>
                  <span style={{ fontWeight: 600, color: tokens.colors.text }}>{pkg.name}</span>
                  <span style={{ color: tokens.colors.textMuted, marginLeft: 6 }}>{pkg.hours} ч</span>
                </div>
                <span style={{ fontWeight: 700, color: tokens.colors.primary }}>{pkg.totalPriceRub} ₽</span>
              </div>
            ))}
          </div>
          <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 8 }}>
            Стоимость рассчитывается за всё время сеанса
          </div>
        </div>
      )}

      {/* Мест */}
      {totalSeats > 0 && (
        <div style={{ padding: '12px 20px', borderBottom: `1px solid ${tokens.colors.border}` }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: tokens.colors.textMuted, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>
            Места
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            {regularCount > 0 && (
              <div style={{ flex: 1, textAlign: 'center', padding: '6px 0' }}>
                <div style={{ fontSize: 22, fontWeight: 800, color: tokens.colors.text }}>{regularCount}</div>
                <div style={{ fontSize: 11, color: tokens.colors.textSecondary }}>стандартных</div>
              </div>
            )}
            {vipCount > 0 && (
              <div style={{ flex: 1, textAlign: 'center', padding: '6px 0' }}>
                <div style={{ fontSize: 22, fontWeight: 800, color: tokens.colors.warning }}>{vipCount}</div>
                <div style={{ fontSize: 11, color: tokens.colors.textSecondary }}>VIP</div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* CTA кнопки */}
      <div style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Button
          type="primary"
          size="large"
          icon={<CalendarOutlined />}
          block
          disabled={isBlocked}
          onClick={onBook}
          style={{ fontWeight: 600 }}
        >
          Забронировать место
        </Button>
        <Button
          size="large"
          icon={<ShoppingOutlined />}
          block
          disabled={isBlocked}
          onClick={onShop}
        >
          Открыть магазин
        </Button>
        {isLoggedIn && (
          <Button
            type="text"
            icon={<WarningOutlined />}
            block
            style={{ color: tokens.colors.textMuted, fontSize: 12, marginTop: 2 }}
            onClick={onReport}
          >
            Пожаловаться на клуб
          </Button>
        )}
      </div>
    </div>
  )
}

// --- Блок "Что есть в клубе" ---

function AmenitiesSection({
  hasRegular,
  hasVip,
  products,
}: {
  hasRegular: boolean
  hasVip: boolean
  products: ClubProductResponse[]
}) {
  const categories = [...new Set(products.filter(p => p.isAvailable).map(p => p.categoryTitle))]

  const amenities: { label: string; icon: React.ReactNode; active: boolean }[] = [
    { label: 'Игровые ПК', icon: <LaptopOutlined />, active: true },
    { label: 'Wi-Fi', icon: <WifiOutlined />, active: true },
    { label: `${hasRegular ? 'Стандартные места' : 'Места'}`, icon: <TeamOutlined />, active: hasRegular },
    { label: 'VIP-зона', icon: <StarOutlined />, active: hasVip },
    { label: 'Магазин', icon: <ShoppingOutlined />, active: products.length > 0 },
    ...categories.map(cat => ({
      label: cat,
      icon: <CoffeeOutlined />,
      active: true,
    })),
  ]

  const unique = amenities.filter((a, i, arr) =>
    a.active && arr.findIndex(b => b.label === a.label) === i
  )

  if (unique.length === 0) return null

  return (
    <SectionCard title="В клубе" style={{ marginBottom: 20 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {unique.map((a) => (
          <div
            key={a.label}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '6px 14px',
              background: tokens.colors.primarySoft,
              borderRadius: 20,
              fontSize: 13,
              fontWeight: 500,
              color: tokens.colors.primary,
            }}
          >
            {a.icon}
            {a.label}
          </div>
        ))}
      </div>
    </SectionCard>
  )
}

// --- Блок характеристик мест ---

function SpecsSection({
  regularSpec,
  vipSpec,
}: {
  regularSpec: SeatSpecResponse | undefined
  vipSpec: SeatSpecResponse | undefined
}) {
  if (!regularSpec && !vipSpec) return null

  return (
    <>
      <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 14, color: tokens.colors.text }}>
        Характеристики мест
      </div>
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        {regularSpec && (
          <Col xs={24} md={vipSpec ? 12 : 24}>
            <div style={{
              background: tokens.colors.surface,
              border: `1px solid ${tokens.colors.border}`,
              borderRadius: tokens.radius.lg,
              overflow: 'hidden',
              height: '100%',
            }}>
              <div style={{
                padding: '12px 18px',
                background: tokens.colors.surfaceAlt,
                borderBottom: `1px solid ${tokens.colors.border}`,
                fontWeight: 700, fontSize: 14, color: tokens.colors.text,
                display: 'flex', alignItems: 'center', gap: 6,
              }}>
                <LaptopOutlined style={{ fontSize: 13, color: tokens.colors.primary }} />
                {regularSpec.title || 'Стандартные места'}
              </div>
              <div style={{ padding: '6px 0' }}>
                {regularSpec.specs.map((line) => (
                  <div key={line.name} style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '7px 18px', gap: 12, fontSize: 13,
                  }}>
                    <span style={{ color: tokens.colors.textSecondary }}>{line.name}</span>
                    <span style={{ fontWeight: 600, color: tokens.colors.text, textAlign: 'right' }}>{line.value}</span>
                  </div>
                ))}
              </div>
            </div>
          </Col>
        )}
        {vipSpec && (
          <Col xs={24} md={regularSpec ? 12 : 24}>
            <div style={{
              background: '#FFFBEB',
              border: `1px solid #FDE68A`,
              borderRadius: tokens.radius.lg,
              overflow: 'hidden',
              height: '100%',
            }}>
              <div style={{
                padding: '12px 18px',
                background: '#FEF3C7',
                borderBottom: `1px solid #FDE68A`,
                fontWeight: 700, fontSize: 14, color: tokens.colors.warning,
                display: 'flex', alignItems: 'center', gap: 6,
              }}>
                <StarOutlined style={{ fontSize: 13 }} />
                {vipSpec.title || 'VIP места'}
              </div>
              <div style={{ padding: '6px 0' }}>
                {vipSpec.specs.map((line) => (
                  <div key={line.name} style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '7px 18px', gap: 12, fontSize: 13,
                  }}>
                    <span style={{ color: tokens.colors.textSecondary }}>{line.name}</span>
                    <span style={{ fontWeight: 600, color: tokens.colors.text, textAlign: 'right' }}>{line.value}</span>
                  </div>
                ))}
              </div>
            </div>
          </Col>
        )}
      </Row>
    </>
  )
}

// --- Превью товаров ---

function ProductsPreview({
  products,
  onShop,
}: {
  products: ClubProductResponse[]
  onShop: () => void
}) {
  const available = products.filter((p) => p.isAvailable)
  if (available.length === 0) return null

  // группируем по категории для показа разнообразия
  const preview = available.slice(0, 6)

  return (
    <SectionCard
      title="Магазин"
      extra={
        <Button type="link" size="small" onClick={onShop} style={{ padding: 0 }}>
          Все товары →
        </Button>
      }
      style={{ marginBottom: 20 }}
    >
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
        gap: 10,
        marginBottom: 14,
      }}>
        {preview.map((product) => (
          <div
            key={product.productId}
            style={{
              background: tokens.colors.surfaceAlt,
              border: `1px solid ${tokens.colors.border}`,
              borderRadius: tokens.radius.md,
              overflow: 'hidden',
            }}
          >
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.title}
                style={{ width: '100%', height: 80, objectFit: 'cover', display: 'block' }}
              />
            ) : (
              <div style={{
                height: 80, display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: tokens.colors.primarySoft,
              }}>
                <ShoppingOutlined style={{ fontSize: 24, color: tokens.colors.primary, opacity: 0.4 }} />
              </div>
            )}
            <div style={{ padding: '8px 10px' }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: tokens.colors.text, marginBottom: 2, lineHeight: 1.3 }}>
                {product.title}
              </div>
              <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginBottom: 4 }}>
                {product.categoryTitle}
              </div>
              <div style={{ fontSize: 13, fontWeight: 700, color: tokens.colors.primary }}>
                {product.priceRub} ₽
              </div>
            </div>
          </div>
        ))}
      </div>
      <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
        Товары можно добавить в корзину вместе с бронированием
      </div>
    </SectionCard>
  )
}

// --- Основной компонент ---

export default function ClubDetailsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()

  const [club, setClub] = useState<ClubListItemResponse | null>(null)
  const [specs, setSpecs] = useState<SeatSpecResponse[]>([])
  const [prices, setPrices] = useState<SeatPriceClientResponse[]>([])
  const [packages, setPackages] = useState<TimePackageClientResponse[]>([])
  const [seats, setSeats] = useState<SeatClientResponse[]>([])
  const [products, setProducts] = useState<ClubProductResponse[]>([])
  const [isFav, setIsFav] = useState(false)
  const [loading, setLoading] = useState(true)

  const isLoggedIn = user !== null
  const id = Number(clubId)

  useEffect(() => {
    if (!id) return
    async function load() {
      setLoading(true)
      try {
        const [clubRes, specsRes, pricesRes, packagesRes, seatsRes, productsRes] = await Promise.all([
          apiClient.get<ClubListItemResponse>(`/clubs/${id}`),
          apiClient.get<SeatSpecResponse[]>(`/clubs/${id}/seat-specs`).catch(() => ({ data: [] as SeatSpecResponse[] })),
          apiClient.get<SeatPriceClientResponse[]>(`/clubs/${id}/seat-prices`).catch(() => ({ data: [] as SeatPriceClientResponse[] })),
          apiClient.get<TimePackageClientResponse[]>(`/clubs/${id}/time-packages`).catch(() => ({ data: [] as TimePackageClientResponse[] })),
          apiClient.get<SeatClientResponse[]>(`/clubs/${id}/seats`).catch(() => ({ data: [] as SeatClientResponse[] })),
          apiClient.get<ClubProductResponse[]>(`/clubs/${id}/products`).catch(() => ({ data: [] as ClubProductResponse[] })),
        ])
        setClub(clubRes.data)
        setSpecs(specsRes.data)
        setPrices(pricesRes.data)
        setPackages(packagesRes.data)
        setSeats(seatsRes.data)
        setProducts(productsRes.data)
      } catch {
        message.error('Не удалось загрузить информацию о клубе')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!isLoggedIn || !id) return
    apiClient.get<number[]>('/me/favorites')
      .then(({ data }) => setIsFav(data.includes(id)))
      .catch(() => {})
  }, [isLoggedIn, id])

  async function toggleFavorite() {
    if (!isLoggedIn) {
      navigate(`/login?from=/clubs/${id}`)
      return
    }
    try {
      if (isFav) {
        await apiClient.delete(`/me/favorites/${id}`)
        setIsFav(false)
      } else {
        await apiClient.put(`/me/favorites/${id}`)
        setIsFav(true)
      }
    } catch {
      message.error('Не удалось обновить избранное')
    }
  }

  if (loading) return <Spin style={{ display: 'block', margin: '64px auto' }} />
  if (!club) return null

  const regularSpec = specs.find((s) => s.seatType === 'REGULAR')
  const vipSpec = specs.find((s) => s.seatType === 'VIP')
  const regularPrice = prices.find((p) => p.seatType === 'REGULAR')
  const vipPrice = prices.find((p) => p.seatType === 'VIP')
  const regularCount = seats.filter((s) => s.type === 'REGULAR').length
  const vipCount = seats.filter((s) => s.type === 'VIP').length
  const isBlocked = club.isBlocked ?? false

  return (
    <div style={{ maxWidth: 1040 }}>
      <ClubHero
        club={club}
        isFav={isFav}
        isLoggedIn={isLoggedIn}
        onFav={toggleFavorite}
        onBack={() => navigate('/clubs')}
      />

      {/* Основная двухколоночная компоновка */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 300px',
        gap: 24,
        alignItems: 'start',
      }}
        className="club-details-layout"
      >
        {/* Левая колонка — контент */}
        <div>
          {/* О клубе */}
          {club.description && (
            <SectionCard title="О клубе" style={{ marginBottom: 20 }}>
              <p style={{ fontSize: 15, lineHeight: 1.8, color: tokens.colors.text, margin: 0 }}>
                {club.description}
              </p>
            </SectionCard>
          )}

          {/* Что есть в клубе */}
          <AmenitiesSection
            hasRegular={regularCount > 0 || !!regularSpec}
            hasVip={vipCount > 0 || !!vipSpec}
            products={products}
          />

          {/* Характеристики мест */}
          <SpecsSection regularSpec={regularSpec} vipSpec={vipSpec} />

          {/* Товары */}
          <ProductsPreview products={products} onShop={() => navigate(`/clubs/${id}/shop`)} />
        </div>

        {/* Правая колонка — действия (sticky) */}
        <div style={{ position: 'sticky', top: 80 }}>
          <ActionCard
            isBlocked={isBlocked}
            isLoggedIn={isLoggedIn}
            regularPrice={regularPrice}
            vipPrice={vipPrice}
            packages={packages}
            regularCount={regularCount}
            vipCount={vipCount}
            onBook={() => navigate(`/clubs/${id}/booking`)}
            onShop={() => navigate(`/clubs/${id}/shop`)}
            onReport={() => navigate(`/clubs/${id}/report`)}
          />
        </div>
      </div>

      {/* Адаптив */}
      <style>{`
        @media (max-width: 720px) {
          .club-details-layout {
            grid-template-columns: 1fr !important;
          }
          .club-details-layout > div:last-child {
            position: static !important;
            order: -1;
          }
        }
      `}</style>
    </div>
  )
}
