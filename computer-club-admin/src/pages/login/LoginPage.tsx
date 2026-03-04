import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, Typography, App } from 'antd'
import axios from 'axios'

const { Title } = Typography

export default function LoginPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()

  useEffect(() => {
    if (localStorage.getItem('accessToken')) {
      navigate('/', { replace: true })
    }
  }, [navigate])

  async function onFinish(values: { username: string; password: string }) {
    try {
      const { data } = await axios.post('/api/v1/admin/auth/login', values)
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      navigate('/', { replace: true })
    } catch {
      message.error('Неверный логин или пароль')
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 360 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>Панель администратора</Title>
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="Логин" rules={[{ required: true, message: 'Введите логин' }]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item name="password" label="Пароль" rules={[{ required: true, message: 'Введите пароль' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block>Войти</Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
