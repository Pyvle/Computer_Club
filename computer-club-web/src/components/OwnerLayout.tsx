import { Layout, Menu, Select, Button, Typography, App, Tooltip, Space } from 'antd'
import {
  AppstoreOutlined,
  DashboardOutlined,
  TeamOutlined,
  LayoutOutlined,
  EnvironmentOutlined,
  CalendarOutlined,
  ShoppingCartOutlined,
  StopOutlined,
  AuditOutlined,
  HomeOutlined,
  LogoutOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  MessageOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import SetPasswordModal from './SetPasswordModal'
import { tokens } from '../theme/tokens'

const { Sider, Header, Content } = Layout

// Плоский список — используется для поиска метки текущей секции
const CLUB_NAV_KEYS = [
  'dashboard',
  'bookings',
  'purchases',
  'messages',
  'catalog',
  'time-packages',
  'seats',
  'floorplans',
  'staff',
  'users',
  'audit',
  'settings',
]

function buildNavGroups(isOwner: boolean) {
  return [
    {
      type: 'group' as const,
      label: 'Операционная часть',
      children: [
        { key: 'dashboard',  icon: <DashboardOutlined />,   label: 'Дашборд' },
        { key: 'bookings',   icon: <CalendarOutlined />,     label: 'Бронирования' },
        { key: 'purchases',  icon: <ShoppingCartOutlined />, label: 'Покупки' },
        { key: 'messages',   icon: <MessageOutlined />,      label: 'Сообщения' },
      ],
    },
    {
      type: 'group' as const,
      label: 'Структура клуба',
      children: [
        { key: 'catalog',       icon: <AppstoreOutlined />,    label: 'Каталог' },
        { key: 'time-packages', icon: <ClockCircleOutlined />, label: 'Тарифы' },
        { key: 'seats',         icon: <EnvironmentOutlined />, label: 'Места' },
        { key: 'floorplans',    icon: <LayoutOutlined />,      label: 'Схемы зала' },
      ],
    },
    {
      type: 'group' as const,
      label: 'Управление',
      children: [
        { key: 'users', icon: <UserOutlined />,    label: 'Пользователи клуба' },
        ...(isOwner ? [
          { key: 'staff',    icon: <TeamOutlined />,    label: 'Персонал' },
          { key: 'audit',    icon: <AuditOutlined />,   label: 'Аудит' },
          { key: 'settings', icon: <SettingOutlined />, label: 'Настройки' },
        ] : []),
      ],
    },
  ]
}

export default function OwnerLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()
  const { user, logout } = useAuth()

  const needsPassword = !!user && !user.hasPassword
  const clubs = user?.clubs ?? []
  const activeClubId = clubId ? Number(clubId) : (clubs[0]?.clubId ?? null)
  const activeClub = clubs.find((c) => c.clubId === activeClubId)
  const isOwner = activeClub?.role === 'OWNER'
  const headerRoleLabel = activeClub?.role === 'OWNER'
    ? 'Владелец'
    : activeClub?.role === 'ADMIN'
      ? 'Администратор'
      : 'Партнёр'

  function handleLogout() {
    logout()
    message.success('Вы вышли из системы')
    navigate('/')
  }

  function handleClubChange(id: number) {
    navigate(`/admin/club/${id}/dashboard`)
  }

  // более надёжный поиск активного ключа — проверяем все сегменты пути
  const pathParts = location.pathname.split('/')
  const selectedClubNavKey = CLUB_NAV_KEYS.find((key) => pathParts.includes(key)) ?? ''
  const isMyClubs = location.pathname === '/admin/my-clubs'

  return (
    <Layout style={{ minHeight: '100vh', background: tokens.colors.background }}>
      <Sider
        width={224}
        className="admin-sider"
        style={{ background: tokens.colors.surface, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          {/* Логотип */}
          <div style={{ padding: '18px 20px 14px', borderBottom: `1px solid ${tokens.colors.border}` }}>
            <Typography.Text strong style={{ color: tokens.colors.primary, fontSize: 15 }}>
              Компьютерный клуб
            </Typography.Text>
          </div>

          {/* Мои клубы */}
          <Menu
            mode="inline"
            selectedKeys={isMyClubs ? ['/admin/my-clubs'] : []}
            items={[{ key: '/admin/my-clubs', icon: <HomeOutlined />, label: 'Мои клубы' }]}
            onClick={() => navigate('/admin/my-clubs')}
            style={{
              border: 'none',
              background: 'transparent',
              marginTop: 4,
              borderBottom: clubs.length > 0 ? `1px solid ${tokens.colors.border}` : undefined,
            }}
          />

          {/* Выбор клуба + сгруппированная навигация */}
          {clubs.length > 0 && (
            <>
              <div style={{ padding: '10px 12px 4px' }}>
                <Select
                  style={{ width: '100%' }}
                  value={activeClubId}
                  onChange={handleClubChange}
                  options={clubs.map((c) => ({ value: c.clubId, label: c.clubName }))}
                  size="small"
                />
              </div>
              <Menu
                mode="inline"
                selectedKeys={[selectedClubNavKey]}
                items={buildNavGroups(isOwner)}
                onClick={({ key }) => {
                  if (activeClubId) navigate(`/admin/club/${activeClubId}/${key}`)
                }}
                style={{ flex: 1, border: 'none', background: 'transparent', overflowY: 'auto' }}
              />
            </>
          )}

          {/* Выход */}
          <div style={{ padding: '12px 12px 20px', borderTop: `1px solid ${tokens.colors.border}`, marginTop: 'auto' }}>
            <Button
              icon={<LogoutOutlined />}
              onClick={handleLogout}
              type="text"
              block
              style={{ textAlign: 'left', color: tokens.colors.textSecondary, justifyContent: 'flex-start' }}
            >
              Выйти
            </Button>
          </div>
        </div>
      </Sider>

      <Layout style={{ background: tokens.colors.background }}>
        <Header
          className="admin-header"
          style={{
            padding: '0 20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            height: 56,
            lineHeight: '56px',
          }}
        >
          {/* Левая часть: выбранный клуб */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
            {activeClub ? (
              <div style={{ lineHeight: 1.3 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: tokens.colors.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 260 }}>
                  {activeClub.clubName}
                </div>
              </div>
            ) : isMyClubs ? (
              <Typography.Text style={{ fontSize: 13, fontWeight: 600 }}>Мои клубы</Typography.Text>
            ) : null}
          </div>

          {/* Правая часть: пользователь + выход */}
          <Space size={8} style={{ flexShrink: 0 }}>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}
            >
              <UserOutlined style={{ fontSize: 11 }} />
              {headerRoleLabel}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {user?.phone ?? '—'}
            </Typography.Text>
            <Tooltip title="Выйти">
              <Button
                type="text"
                size="small"
                icon={<LogoutOutlined />}
                onClick={handleLogout}
                style={{ color: tokens.colors.textSecondary }}
              />
            </Tooltip>
          </Space>
        </Header>

        <Content style={{ margin: 24, minHeight: 0 }}>
          <Outlet />
        </Content>
      </Layout>

      <SetPasswordModal open={needsPassword} onClose={() => {}} />
    </Layout>
  )
}
