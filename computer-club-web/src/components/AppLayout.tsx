import { Layout, Menu, Button, Typography, App } from 'antd'
import {
  FileDoneOutlined,
  AppstoreOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import apiClient from '../utils/apiClient'
import { getCurrentUser } from '../utils/auth'

const { Sider, Header, Content } = Layout

const GLOBAL_ADMIN_NAV = [
  { key: '/club-applications', icon: <FileDoneOutlined />, label: 'Заявки на клубы' },
  { key: '/global-catalog', icon: <AppstoreOutlined />, label: 'Глобальный каталог' },
  { key: '/users', icon: <UserOutlined />, label: 'Пользователи' },
]

export default function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()

  const user = getCurrentUser()
  const username = localStorage.getItem('username') ?? '—'
  const isGlobalAdmin = user?.gr === 'GLOBAL_ADMIN'
  const navItems = isGlobalAdmin ? GLOBAL_ADMIN_NAV : []
  const roleLabel = isGlobalAdmin ? 'GLOBAL_ADMIN' : 'Ограниченный доступ'

  async function handleLogout() {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      try {
        await apiClient.post('/admin/auth/logout', { refreshToken })
      } catch {
        // продолжаем выход даже при ошибке
      }
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    message.success('Вы вышли из системы')
    navigate('/login')
  }

  const selectedKey = navItems.find((item) => location.pathname.startsWith(item.key))?.key ?? ''

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <Typography.Text strong style={{ color: '#fff', fontSize: 14 }}>
            Computer Club
          </Typography.Text>
          <br />
          <Typography.Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12 }}>
            {username} · {roleLabel}
          </Typography.Text>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={navItems}
          onClick={({ key }) => navigate(key)}
          style={{ marginTop: 8 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <Button icon={<LogoutOutlined />} onClick={handleLogout} type="text">
            Выйти
          </Button>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
