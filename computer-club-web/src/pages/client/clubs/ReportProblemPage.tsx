import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Card, Form, Input, Typography, App } from 'antd'
import { ArrowLeftOutlined, WarningOutlined } from '@ant-design/icons'
import apiClient from '../../../utils/apiClient'

const { Title, Text } = Typography

export default function ReportProblemPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  async function handleSubmit(values: { message: string }) {
    setLoading(true)
    try {
      await apiClient.post(`/clubs/${clubId}/reports`, { message: values.message })
      setSent(true)
    } catch {
      message.error('Не удалось отправить жалобу. Попробуйте позже.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 560 }}>
      <Button
        type="text"
        icon={<ArrowLeftOutlined />}
        style={{ marginBottom: 16, padding: 0 }}
        onClick={() => navigate(-1)}
      >
        Назад
      </Button>

      <Card>
        {sent ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <WarningOutlined style={{ fontSize: 48, color: '#faad14', marginBottom: 16 }} />
            <Title level={4} style={{ marginBottom: 8 }}>Жалоба отправлена</Title>
            <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
              Мы рассмотрим её в ближайшее время.
            </Text>
            <Button type="primary" onClick={() => navigate(-1)}>
              Вернуться к клубу
            </Button>
          </div>
        ) : (
          <>
            <Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>Пожаловаться</Title>
            <Text type="secondary" style={{ display: 'block', marginBottom: 20 }}>
              Опишите проблему — администрация клуба получит уведомление.
            </Text>
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
              <Form.Item
                name="message"
                label="Описание проблемы"
                rules={[
                  { required: true, message: 'Введите описание' },
                  { min: 10, message: 'Минимум 10 символов' },
                ]}
              >
                <Input.TextArea
                  rows={5}
                  maxLength={1000}
                  showCount
                  placeholder="Опишите, что произошло..."
                  autoFocus
                />
              </Form.Item>
              <Form.Item style={{ marginBottom: 0 }}>
                <Button type="primary" htmlType="submit" loading={loading} block>
                  Отправить жалобу
                </Button>
              </Form.Item>
            </Form>
          </>
        )}
      </Card>
    </div>
  )
}
