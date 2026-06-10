import { useEffect, useState } from 'react'
import { Badge, Layout } from 'antd'
import {
  ShoppingCartOutlined,
  UserOutlined,
  HistoryOutlined,
  HomeOutlined,
  MenuOutlined,
  CloseOutlined,
} from '@ant-design/icons'
import { Link, Outlet, useLocation, useMatch, useNavigate } from 'react-router-dom'
import { useClient } from '../contexts/ClientContext'
import { useAuth } from '../contexts/AuthContext'
import { tokens } from '../theme/tokens'

const { Header, Content } = Layout

const NAV_ITEMS = [
  { key: '/clubs', label: 'Клубы', icon: <HomeOutlined /> },
  { key: '/history', label: 'История', icon: <HistoryOutlined /> },
]

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

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

  const isLoggedIn = user !== null
  const isRoot = location.pathname === '/'
  const activeKey = isRoot
    ? '/clubs'
    : (NAV_ITEMS.find((item) => location.pathname.startsWith(item.key))?.key ?? '')

  return (
    <Layout style={{ minHeight: '100vh', background: tokens.colors.background }}>
      <Header
        style={{
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
        }}
      >
        <Link to="/" style={{ textDecoration: 'none', flexShrink: 0, marginRight: 32 }}>
          <span
            style={{
              fontSize: 16,
              fontWeight: 800,
              color: tokens.colors.primary,
              letterSpacing: '-0.4px',
            }}
          >
            Компьютерный клуб
          </span>
        </Link>

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

        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0, marginLeft: 'auto' }}>
          <Badge count={cartCount} size="small" offset={[-3, 3]}>
            <button
              onClick={() => (currentClubId ? navigate(`/clubs/${currentClubId}/cart`) : navigate('/clubs'))}
              style={{
                width: 38,
                height: 38,
                borderRadius: tokens.radius.sm,
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
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

          {isLoggedIn ? (
            <button
              onClick={() => navigate('/profile')}
              style={{
                height: 38,
                padding: '0 14px',
                borderRadius: tokens.radius.sm,
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                color: tokens.colors.textSecondary,
                fontSize: 14,
                fontWeight: 600,
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = tokens.colors.surfaceAlt }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
              title="Профиль"
            >
              <UserOutlined style={{ fontSize: 16 }} />
              <span className="client-profile-label">Профиль</span>
            </button>
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

          <button
            onClick={() => setMobileMenuOpen((v) => !v)}
            className="client-nav-hamburger"
            style={{
              width: 38,
              height: 38,
              borderRadius: tokens.radius.sm,
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              display: 'none',
              alignItems: 'center',
              justifyContent: 'center',
              color: tokens.colors.textSecondary,
            }}
          >
            {mobileMenuOpen ? <CloseOutlined style={{ fontSize: 16 }} /> : <MenuOutlined style={{ fontSize: 16 }} />}
          </button>
        </div>
      </Header>

      {mobileMenuOpen && (
        <div
          className="client-nav-mobile"
          style={{
            position: 'fixed',
            top: 60,
            left: 0,
            right: 0,
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
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  width: '100%',
                  padding: '12px 24px',
                  background: active ? tokens.colors.primarySoft : 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  color: active ? tokens.colors.primary : tokens.colors.text,
                  fontWeight: active ? 600 : 400,
                  fontSize: 15,
                  textAlign: 'left',
                }}
              >
                {item.icon}
                {item.label}
              </button>
            )
          })}
          {isLoggedIn ? (
            <button
              onClick={() => navigate('/profile')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                width: '100%',
                padding: '12px 24px',
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: tokens.colors.text,
                fontWeight: 500,
                fontSize: 15,
                textAlign: 'left',
              }}
            >
              <UserOutlined />
              Профиль
            </button>
          ) : null}
        </div>
      )}

      <Content
        style={{
          padding: '32px 24px',
          width: '100%',
          maxWidth: 1120,
          margin: '0 auto',
          boxSizing: 'border-box',
        }}
      >
        <Outlet />
      </Content>

      <style>{`
        @media (max-width: 600px) {
          .client-nav-desktop { display: none !important; }
          .client-nav-hamburger { display: flex !important; }
          .client-nav-mobile { display: block; }
          .client-profile-label { display: none; }
        }
      `}</style>
    </Layout>
  )
}
