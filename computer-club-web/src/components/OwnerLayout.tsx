import { Layout, Menu, Select, Button, Typography, App } from 'antd'
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
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import SetPasswordModal from './SetPasswordModal'

const { Sider, Header, Content } = Layout

const CLUB_NAV = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: 'Дашборд' },
  { key: 'catalog', icon: <AppstoreOutlined />, label: 'Каталог' },
  { key: 'seats', icon: <EnvironmentOutlined />, label: 'Места' },
  { key: 'floorplans', icon: <LayoutOutlined />, label: 'Схемы зала' },
  { key: 'staff', icon: <TeamOutlined />, label: 'Персонал' },
  { key: 'bookings', icon: <CalendarOutlined />, label: 'Бронирования' },
  { key: 'purchases', icon: <ShoppingCartOutlined />, label: 'Покупки' },
  { key: 'user-blocks', icon: <StopOutlined />, label: 'Блокировки' },
  { key: 'audit', icon: <AuditOutlined />, label: 'Аудит' },
]

export default function OwnerLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()
  const { user, logout } = useAuth()

  const needsPassword = !!user && !user.hasPassword

  const clubs = user?.clubs ?? []
  const activeClubId = clubId ? Number(clubId) : (clubs[0]?.clubId ?? null)

  function handleLogout() {
    logout()
    message.success('Вы вышли из системы')
    navigate('/')
  }

  function handleClubChange(id: number) {
    navigate(`/admin/club/${id}/dashboard`)
  }

  const pathSegment = location.pathname.split('/').pop()
  const selectedClubNavKey = CLUB_NAV.find((n) => n.key === pathSegment)?.key ?? ''
  const isMyClubs = location.pathname === '/admin/my-clubs'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <Typography.Text strong style={{ color: '#fff', fontSize: 14 }}>
            Computer Club
          </Typography.Text>
          <br />
          <Typography.Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12 }}>
            {user?.phone ?? '—'}
          </Typography.Text>
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={isMyClubs ? ['/admin/my-clubs'] : []}
          items={[{ key: '/admin/my-clubs', icon: <HomeOutlined />, label: 'Мои клубы' }]}
          onClick={() => navigate('/admin/my-clubs')}
          style={{ marginTop: 8, borderBottom: clubs.length > 0 ? '1px solid rgba(255,255,255,0.1)' : undefined }}
        />

        {clubs.length > 0 && (
          <>
            <div style={{ padding: '12px 16px' }}>
              <Select
                style={{ width: '100%' }}
                value={activeClubId}
                onChange={handleClubChange}
                options={clubs.map((c) => ({ value: c.clubId, label: c.clubName }))}
                variant="filled"
              />
            </div>
            <Menu
              theme="dark"
              mode="inline"
              selectedKeys={[selectedClubNavKey]}
              items={CLUB_NAV}
              onClick={({ key }) => {
                if (activeClubId) navigate(`/admin/club/${activeClubId}/${key}`)
              }}
            />
          </>
        )}

        <div style={{ position: 'absolute', bottom: 16, left: 0, right: 0, padding: '0 16px' }}>
          <Button
            icon={<LogoutOutlined />}
            onClick={handleLogout}
            type="text"
            style={{ color: 'rgba(255,255,255,0.65)', width: '100%', textAlign: 'left' }}
          >
            Выйти
          </Button>
        </div>
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
      {/* пользователь с ролью ADMIN/OWNER без пароля должен установить пароль для доступа к панели */}
      <SetPasswordModal open={needsPassword} onClose={() => {}} />
    </Layout>
  )
}
