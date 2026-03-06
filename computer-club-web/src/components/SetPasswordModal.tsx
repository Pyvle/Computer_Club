import { useState } from 'react'
import { Modal, Form, Input, Button, App } from 'antd'
import { useAuth } from '../contexts/AuthContext'
import apiClient from '../utils/apiClient'

interface Props {
  open: boolean
  onClose: () => void
}

export default function SetPasswordModal({ open, onClose }: Props) {
  const { loadContext } = useAuth()
  const { message } = App.useApp()
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm()

  async function onFinish(values: { password: string }) {
    setSubmitting(true)
    try {
      await apiClient.post('/me/set-password', { password: values.password })
      message.success('Пароль установлен')
      await loadContext()
      form.resetFields()
      onClose()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка установки пароля')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal open={open} onCancel={onClose} title="Установить пароль" footer={null}>
      <Form layout="vertical" form={form} onFinish={onFinish} style={{ marginTop: 16 }}>
        <Form.Item
          name="password"
          label="Пароль"
          rules={[{ required: true }, { min: 6, message: 'Минимум 6 символов' }]}
        >
          <Input.Password autoFocus />
        </Form.Item>
        <Form.Item
          name="confirm"
          label="Повторите пароль"
          dependencies={['password']}
          rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) return Promise.resolve()
                return Promise.reject(new Error('Пароли не совпадают'))
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={submitting}>
          Установить пароль
        </Button>
      </Form>
    </Modal>
  )
}
