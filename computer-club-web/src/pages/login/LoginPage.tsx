import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Card, Form, Input, Button, Typography, App } from 'antd'
import axios from 'axios'
import { useAuth } from '../../contexts/AuthContext'

const { Title, Text, Link } = Typography

type Mode = 'otp-phone' | 'otp-code' | 'admin'

function resolveRedirect(globalRole: string, clubs: { clubId: number }[]): string {
  if (globalRole === 'GLOBAL_ADMIN') return '/admin/platform/applications'
  return '/admin/my-clubs'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { message } = App.useApp()
  const { loadContext } = useAuth()

  const isPartnerMode = searchParams.get('partner') === '1'
  const [mode, setMode] = useState<Mode>(isPartnerMode ? 'otp-phone' : 'otp-phone')
  const [phone, setPhone] = useState('')
  const [challengeId, setChallengeId] = useState('')
  const [loading, setLoading] = useState(false)


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
    } catch (e: any) {
      if (e?.response?.status === 401) {
        message.error('Неверный номер телефона или пароль')
      } else {
        message.error('Не удалось подключиться к серверу')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 380 }}>
        {mode === 'otp-phone' && (
          <>
            <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
              {isPartnerMode ? 'Регистрация партнёра' : 'Вход'}
            </Title>
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
            <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <Link onClick={() => setMode('admin')}>Войти как администратор</Link>
              {!isPartnerMode && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Хотите открыть клуб?{' '}
                  <Link href="/login?partner=1">Стать партнёром</Link>
                </Text>
              )}
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
                label="Телефон или логин"
                rules={[{ required: true, message: 'Введите телефон или логин' }]}
              >
                <Input placeholder="+7XXXXXXXXXX или username" autoFocus />
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
