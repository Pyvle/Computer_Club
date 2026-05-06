import { useEffect, useState } from 'react'
import { Badge, Dropdown, Layout } from 'antd'
import {
  ShoppingCartOutlined,
  UserOutlined,
  LogoutOutlined,
  HistoryOutlined,
  HomeOutlined,
  MenuOutlined,
  CloseOutlined,
} from '@ant-design/icons'
import { Link, Outlet, useNavigate, useLocation, useMatch } from 'react-router-dom'
import { useClient } from '../contexts/ClientContext'
import { useAuth } from '../contexts/AuthContext'
import { tokens } from '../theme/tokens'

const { Header, Content } = Layout

const NAV_ITEMS = [
  { key: '/clubs',   label: 'Клубы',   icon: <HomeOutlined /> },
  { key: '/history', label: 'История', icon: <HistoryOutlined /> },
]

// --- Аватар пользователя ---

function UserAvatar({ phone }: { phone: string | null | undefined }) {
  const label = phone ? phone.replace(/\D/g, '').slice(-2) : '?'
  const colors = ['#4F46E5', '#7C3AED', '#059669', '#DC2626', '#D97706', '#2563EB']
  const bg = colors[parseInt(label, 10) % colors.length] ?? tokens.colors.primary
  return (
    <div style={{
      width: 32, height: 32, borderRadius: '50%',
      background: bg,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 12, fontWeight: 700, color: '#fff',
      cursor: 'pointer', userSelect: 'none', flexShrink: 0,
    }}>
      {label}
    </div>
  )
}

// --- Основной компонент ---

export default function ClientLayout() {
  const { cartCount, refreshCartCount } = useClient()
  const { user, loadContext, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const clubMatch = useMatch('/clubs/:clubId/*')
  const currentClubId = clubMatch ? Number(clubMatch.params.clubId) : null

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) return
    loadContext().catch(() => { logout() })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (currentClubId) refreshCartCount(currentClubId)
  }, [currentClubId]) // eslint-disable-line react-hooks/exhaustive-deps

  // закрываем мобильное меню при смене страницы
  useEffect(() => { setMobileMenuOpen(false) }, [location.pathname])

  const isLoggedIn = user !== null

  function handleLogout() {
    logout()
    navigate('/')
  }

  // определяем активный пункт навигации
  const isRoot = location.pathname === '/'
  const activeKey = isRoot
    ? '/clubs'
    : (NAV_ITEMS.find((item) => location.pathname.startsWith(item.key))?.key ?? '')

  return (
    <Layout style={{ minHeight: '100vh', background: tokens.colors.background }}>
      {/* Header */}
      <Header style={{
        display: 'flex',
        alignItems: 'center',
        gap: 0,
        padding: '0 24px',
        background: tokens.colors.surface,
        borderBottom: `1px solid ${tokens.colors.border}`,
        position: 'sticky',
        top: 0,
        zIndex: 100,
        height: 60,
        boxShadow: '0 1px 4px rgba(15, 23, 42, 0.05)',
      }}>
        {/* Логотип */}
        <Link to="/" style={{ textDecoration: 'none', flexShrink: 0, marginRight: 32 }}>
          <span style={{
            fontSize: 16, fontWeight: 800,
            color: tokens.colors.primary,
            letterSpacing: '-0.4px',
          }}>
            Компьютерный клуб
          </span>
        </Link>

        {/* Навигация — десктоп */}
        <nav style={{ display: 'flex', gap: 4, flex: 1 }} className="client-nav-desktop">
          {NAV_ITEMS.map((item) => {
            const active = activeKey === item.key
            return (
              <button
                key={item.key}
                onClick={() => navigate(item.key)}
                style={{
                  padding: '6px 14px',
                  borderRadius: tokens.radius.sm,
                  border: 'none',
                  background: active ? tokens.colors.primarySoft : 'transparent',
                  color: active ? tokens.colors.primary : tokens.colors.textSecondary,
                  fontWeight: active ? 600 : 400,
                  fontSize: 14,
                  cursor: 'pointer',
                  transition: 'all 0.15s',
                }}
                onMouseEnter={(e) => {
                  if (!active) e.currentTarget.style.background = tokens.colors.surfaceAlt
                }}
                onMouseLeave={(e) => {
                  if (!active) e.currentTarget.style.background = 'transparent'
                }}
              >
                {item.label}
              </button>
            )
          })}
        </nav>

        {/* Правая часть */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0, marginLeft: 'auto' }}>
          {/* Корзина */}
          <Badge count={cartCount} size="small" offset={[-3, 3]}>
            <button
              onClick={() => currentClubId ? navigate(`/clubs/${currentClubId}/cart`) : navigate('/clubs')}
              style={{
                width: 38, height: 38, borderRadius: tokens.radius.sm,
                background: 'transparent', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: tokens.colors.textSecondary,
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = tokens.colors.surfaceAlt }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
              title="Корзина"
            >
              <ShoppingCartOutlined style={{ fontSize: 18 }} />
            </button>
          </Badge>

          {/* Пользователь */}
          {isLoggedIn ? (
            <Dropdown
              menu={{
                items: [
                  {
                    key: 'phone',
                    disabled: true,
                    label: (
                      <span style={{ fontSize: 12, color: tokens.colors.textMuted }}>
                        {user.phone ?? 'Без телефона'}
                      </span>
                    ),
                  },
                  { type: 'divider' },
                  { key: 'profile', icon: <UserOutlined />, label: 'Профиль', onClick: () => navigate('/profile') },
                  { key: 'history', icon: <HistoryOutlined />, label: 'История заказов', onClick: () => navigate('/history') },
                  { type: 'divider' },
                  { key: 'logout', icon: <LogoutOutlined />, label: 'Выйти', danger: true, onClick: handleLogout },
                ],
              }}
              placement="bottomRight"
              trigger={['click']}
            >
              <div style={{ cursor: 'pointer' }}>
                <UserAvatar phone={user.phone} />
              </div>
            </Dropdown>
          ) : (
            <button
              onClick={() => navigate(`/login?from=${encodeURIComponent(location.pathname)}`)}
              style={{
                padding: '6px 16px',
                borderRadius: tokens.radius.sm,
                border: 'none',
                background: tokens.colors.primary,
                color: '#fff',
                fontWeight: 600,
                fontSize: 14,
                cursor: 'pointer',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = tokens.colors.primaryHover }}
              onMouseLeave={(e) => { e.currentTarget.style.background = tokens.colors.primary }}
            >
              Войти
            </button>
          )}

          {/* Гамбургер — мобильный */}
          <button
            onClick={() => setMobileMenuOpen((v) => !v)}
            className="client-nav-hamburger"
            style={{
              width: 38, height: 38, borderRadius: tokens.radius.sm,
              background: 'transparent', border: 'none', cursor: 'pointer',
              display: 'none', alignItems: 'center', justifyContent: 'center',
              color: tokens.colors.textSecondary,
            }}
          >
            {mobileMenuOpen ? <CloseOutlined style={{ fontSize: 16 }} /> : <MenuOutlined style={{ fontSize: 16 }} />}
          </button>
        </div>
      </Header>

      {/* Мобильное меню */}
      {mobileMenuOpen && (
        <div
          className="client-nav-mobile"
          style={{
            position: 'fixed', top: 60, left: 0, right: 0,
            background: tokens.colors.surface,
            borderBottom: `1px solid ${tokens.colors.border}`,
            zIndex: 99,
            padding: '8px 0',
            boxShadow: tokens.shadow.card,
          }}
        >
          {NAV_ITEMS.map((item) => {
            const active = activeKey === item.key
            return (
              <button
                key={item.key}
                onClick={() => navigate(item.key)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  width: '100%', padding: '12px 24px',
                  background: active ? tokens.colors.primarySoft : 'transparent',
                  border: 'none', cursor: 'pointer',
                  color: active ? tokens.colors.primary : tokens.colors.text,
                  fontWeight: active ? 600 : 400,
                  fontSize: 15, textAlign: 'left',
                }}
              >
                {item.icon}
                {item.label}
              </button>
            )
          })}
        </div>
      )}

      {/* Контент */}
      <Content style={{
        padding: '32px 24px',
        width: '100%',
        maxWidth: 1120,
        margin: '0 auto',
        boxSizing: 'border-box',
      }}>
        <Outlet />
      </Content>

      {/* Адаптив: прячем десктопную навигацию, показываем гамбургер */}
      <style>{`
        @media (max-width: 600px) {
          .client-nav-desktop { display: none !important; }
          .client-nav-hamburger { display: flex !important; }
          .client-nav-mobile { display: block; }
        }
      `}</style>
    </Layout>
  )
}
