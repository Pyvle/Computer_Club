import { Layout, Typography } from 'antd'
import { Outlet } from 'react-router-dom'

const { Header, Content } = Layout
const { Title } = Typography

export default function PublicLayout() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#001529' }}>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          Компьютерный клуб
        </Title>
      </Header>
      <Content>
        <Outlet />
      </Content>
    </Layout>
  )
}
