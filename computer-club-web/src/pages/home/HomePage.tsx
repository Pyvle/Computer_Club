import { Button, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'

const { Title, Paragraph } = Typography

export default function HomePage() {
  const navigate = useNavigate()

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 'calc(100vh - 64px)', padding: 24 }}>
      <Title>Добро пожаловать в Computer Club</Title>
      <Paragraph type="secondary" style={{ fontSize: 16, marginBottom: 32 }}>
        Сеть компьютерных клубов — бронирование, магазин, личный кабинет
      </Paragraph>
      <Space size="large">
        <Button type="primary" size="large" onClick={() => navigate('/login')}>
          Войти
        </Button>
        <Button size="large" onClick={() => navigate('/login?partner=1')}>
          Стать партнёром
        </Button>
      </Space>
    </div>
  )
}
