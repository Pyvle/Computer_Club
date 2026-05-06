import { Layout, Menu, Button, Typography, App, Tooltip, Space } from 'antd'
import {
  FileDoneOutlined,
  AppstoreOutlined,
  UserOutlined,
  LogoutOutlined,
  ShopOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { tokens } from '../theme/tokens'

const { Sider, Header, Content } = Layout

const PLATFORM_NAV = [
  { key: '/admin/platform/applications', icon: <FileDoneOutlined />, label: 'Заявки на клубы' },
  { key: '/admin/platform/clubs',        icon: <ShopOutlined />,      label: 'Клубы' },
  { key: '/admin/platform/catalog',      icon: <AppstoreOutlined />,  label: 'Глобальный каталог' },
  { key: '/admin/platform/users',        icon: <UserOutlined />,      label: 'Пользователи' },
]

export default function PlatformLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()
  const { user, logout } = useAuth()

  async function handleLogout() {
    logout()
    message.success('Вы вышли из системы')
    navigate('/')
  }

  const activeItem = PLATFORM_NAV.find((item) => location.pathname.startsWith(item.key))
  const selectedKey = activeItem?.key ?? ''

  return (
    <Layout style={{ minHeight: '100vh', background: tokens.colors.background }}>
      <Sider
        width={224}
        className="admin-sider"
        style={{ background: tokens.colors.surface }}
      >
        {/* Логотип */}
        <div
          style={{
            padding: '18px 20px 14px',
            borderBottom: `1px solid ${tokens.colors.border}`,
          }}
        >
          <Typography.Text strong style={{ color: tokens.colors.primary, fontSize: 15 }}>
            Компьютерный клуб
          </Typography.Text>
        </div>

        {/* Навигация */}
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={PLATFORM_NAV}
          onClick={({ key }) => navigate(key)}
          style={{
            flex: 1,
            border: 'none',
            marginTop: 8,
            background: 'transparent',
          }}
        />

        {/* Выход внизу */}
        <div style={{ padding: '12px 12px 20px', borderTop: `1px solid ${tokens.colors.border}` }}>
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
          <div />

          {/* Правая часть: пользователь + выход */}
          <Space size={8} style={{ flexShrink: 0 }}>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}
            >
              <UserOutlined style={{ fontSize: 11 }} />
              Менеджер платформы
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
    </Layout>
  )
}
