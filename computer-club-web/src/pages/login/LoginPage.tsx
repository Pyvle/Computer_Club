import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, Typography, App } from 'antd'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'

const { Title, Text, Link } = Typography

type Mode = 'otp-phone' | 'otp-code' | 'admin'

function resolveRedirect(globalRole: string, clubs: { clubId: number }[]): string {
  if (globalRole === 'GLOBAL_ADMIN') return '/admin/platform/applications'
  if (clubs.length > 0) return `/admin/club/${clubs[0].clubId}`
  return '/admin/onboarding'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { loadContext } = useAuth()

  const [mode, setMode] = useState<Mode>('otp-phone')
  const [phone, setPhone] = useState('')
  const [challengeId, setChallengeId] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (localStorage.getItem('accessToken')) {
      navigate('/', { replace: true })
    }
  }, [navigate])

  // --- OTP: шаг 1 — запрос кода ---
  async function onRequestOtp(values: { phone: string }) {
    setLoading(true)
    try {
      const { data } = await axios.post('/api/v1/auth/otp/request', { phone: values.phone })
      setPhone(values.phone)
      setChallengeId(data.challengeId)
      setMode('otp-code')
    } catch {
      message.error('Не удалось отправить код. Проверьте номер телефона.')
    } finally {
      setLoading(false)
    }
  }

  // --- OTP: шаг 2 — подтверждение кода ---
  async function onVerifyOtp(values: { code: string }) {
    setLoading(true)
    try {
      const { data } = await axios.post('/api/v1/auth/otp/verify', {
        challengeId,
        code: values.code,
      })
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      const ctx = await loadContext()
      navigate(resolveRedirect(ctx.globalRole, ctx.clubs), { replace: true })
    } catch {
      message.error('Неверный или истёкший код')
    } finally {
      setLoading(false)
    }
  }

  // --- Admin: телефон + пароль ---
  async function onAdminLogin(values: { phone: string; password: string }) {
    setLoading(true)
    try {
      const { data } = await axios.post('/api/v1/admin/auth/login', values)
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      const ctx = await loadContext()
      navigate(resolveRedirect(ctx.globalRole, ctx.clubs), { replace: true })
    } catch {
      message.error('Неверный номер телефона или пароль')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 380 }}>
        {mode === 'otp-phone' && (
          <>
            <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>Вход</Title>
            <Form layout="vertical" onFinish={onRequestOtp}>
              <Form.Item
                name="phone"
                label="Номер телефона"
                rules={[{ required: true, message: 'Введите номер телефона' }]}
              >
                <Input placeholder="+7XXXXXXXXXX" autoFocus />
              </Form.Item>
              <Form.Item style={{ marginBottom: 12 }}>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  Получить код
                </Button>
              </Form.Item>
            </Form>
            <div style={{ textAlign: 'center' }}>
              <Link onClick={() => setMode('admin')}>Войти как администратор</Link>
            </div>
          </>
        )}

        {mode === 'otp-code' && (
          <>
            <Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>Введите код</Title>
            <Text type="secondary" style={{ display: 'block', textAlign: 'center', marginBottom: 24 }}>
              Код отправлен на {phone}
            </Text>
            <Form layout="vertical" onFinish={onVerifyOtp}>
              <Form.Item
                name="code"
                label="Код из SMS"
                rules={[{ required: true, message: 'Введите код' }]}
              >
                <Input maxLength={6} autoFocus style={{ letterSpacing: 4, fontSize: 20, textAlign: 'center' }} />
              </Form.Item>
              <Form.Item style={{ marginBottom: 12 }}>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  Войти
                </Button>
              </Form.Item>
            </Form>
            <div style={{ textAlign: 'center' }}>
              <Link onClick={() => setMode('otp-phone')}>← Изменить номер</Link>
            </div>
          </>
        )}

        {mode === 'admin' && (
          <>
            <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>Вход для администратора</Title>
            <Form layout="vertical" onFinish={onAdminLogin}>
              <Form.Item
                name="phone"
                label="Номер телефона"
                rules={[{ required: true, message: 'Введите номер телефона' }]}
              >
                <Input placeholder="+7XXXXXXXXXX" autoFocus />
              </Form.Item>
              <Form.Item
                name="password"
                label="Пароль"
                rules={[{ required: true, message: 'Введите пароль' }]}
              >
                <Input.Password />
              </Form.Item>
              <Form.Item style={{ marginBottom: 12 }}>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  Войти
                </Button>
              </Form.Item>
            </Form>
            <div style={{ textAlign: 'center' }}>
              <Link onClick={() => setMode('otp-phone')}>← Обычный вход</Link>
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
