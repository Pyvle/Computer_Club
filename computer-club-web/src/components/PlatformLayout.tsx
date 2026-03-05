import { Layout, Menu, Button, Typography, App } from 'antd'
import {
  FileDoneOutlined,
  AppstoreOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const { Sider, Header, Content } = Layout

const PLATFORM_NAV = [
  { key: '/admin/platform/applications', icon: <FileDoneOutlined />, label: 'Заявки на клубы' },
  { key: '/admin/platform/catalog', icon: <AppstoreOutlined />, label: 'Глобальный каталог' },
  { key: '/admin/platform/users', icon: <UserOutlined />, label: 'Пользователи' },
]

export default function PlatformLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()
  const { user, logout } = useAuth()

  async function handleLogout() {
    logout()
    message.success('Вы вышли из системы')
    navigate('/login')
  }

  const selectedKey = PLATFORM_NAV.find((item) => location.pathname.startsWith(item.key))?.key ?? ''

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <Typography.Text strong style={{ color: '#fff', fontSize: 14 }}>
            Computer Club
          </Typography.Text>
          <br />
          <Typography.Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12 }}>
            {user?.phone ?? '—'} · GLOBAL_ADMIN
          </Typography.Text>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={PLATFORM_NAV}
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
