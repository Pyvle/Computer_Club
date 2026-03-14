import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Alert, App, Button, Card, Form, Input, Spin, Switch, Upload } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from 'rc-upload/lib/interface'
import apiClient from '../../utils/apiClient'
import type { ClubSettingsResponse, UpdateClubSettingsRequest } from '../../types'

export default function ClubSettingsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()
  const [form] = Form.useForm<UpdateClubSettingsRequest>()

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [isActive, setIsActive] = useState(true)
  const [imageUrl, setImageUrl] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (!clubId) return
    setLoading(true)
    apiClient
      .get<ClubSettingsResponse>(`/admin/clubs/${clubId}/settings`)
      .then((r) => {
        form.setFieldsValue(r.data)
        setIsActive(r.data.isActive)
        setImageUrl(r.data.imageUrl)
      })
      .catch(() => message.error('Не удалось загрузить настройки клуба'))
      .finally(() => setLoading(false))
  }, [clubId])

  async function handleSave(values: UpdateClubSettingsRequest) {
    if (!clubId) return
    setSaving(true)
    try {
      await apiClient.put(`/admin/clubs/${clubId}/settings`, values)
      message.success('Настройки сохранены')
    } catch {
      message.error('Не удалось сохранить настройки')
    } finally {
      setSaving(false)
    }
  }

  async function handleImageUpload(options: UploadRequestOption) {
    if (!clubId) return
    setUploading(true)
    const formData = new FormData()
    formData.append('file', options.file as File)
    try {
      const res = await apiClient.post<{ imageUrl: string }>(
        `/admin/clubs/${clubId}/image`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      )
      setImageUrl(res.data.imageUrl)
      message.success('Изображение загружено')
      options.onSuccess?.(res.data)
    } catch {
      message.error('Не удалось загрузить изображение')
      options.onError?.(new Error('Upload failed'))
    } finally {
      setUploading(false)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 300 }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 640 }}>
      <h2 style={{ marginTop: 0, marginBottom: 20 }}>Настройки клуба</h2>

      <Card title="Изображение клуба" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 24 }}>
          {imageUrl && (
            <img
              src={imageUrl}
              alt="Клуб"
              style={{ width: 160, height: 100, objectFit: 'cover', borderRadius: 8, border: '1px solid #f0f0f0' }}
            />
          )}
          <Upload
            accept="image/*"
            showUploadList={false}
            customRequest={handleImageUpload}
          >
            <Button icon={<PlusOutlined />} loading={uploading}>
              {imageUrl ? 'Заменить фото' : 'Загрузить фото'}
            </Button>
          </Upload>
        </div>
      </Card>

      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
          onValuesChange={(changed) => {
            if ('isActive' in changed) setIsActive(changed.isActive)
          }}
        >
          <Form.Item
            name="name"
            label="Название клуба"
            rules={[{ required: true, message: 'Введите название' }]}
          >
            <Input placeholder="Например: Club Zero" maxLength={120} />
          </Form.Item>

          <Form.Item
            name="address"
            label="Адрес"
            rules={[{ required: true, message: 'Введите адрес' }]}
          >
            <Input placeholder="ул. Ленина, д. 1" maxLength={255} />
          </Form.Item>

          <Form.Item name="locationText" label="Описание локации">
            <Input
              placeholder="3 этаж ТЦ «Галактика», вход со стороны парковки"
              maxLength={255}
            />
          </Form.Item>

          <Form.Item name="description" label="Описание клуба">
            <Input.TextArea
              rows={4}
              placeholder="Краткое описание, которое видят пользователи в приложении"
            />
          </Form.Item>

          <Form.Item name="isActive" label="Клуб активен" valuePropName="checked">
            <Switch />
          </Form.Item>

          {!isActive && (
            <Alert
              type="warning"
              showIcon
              message="Клуб будет скрыт из приложения"
              description="Пользователи не смогут видеть клуб и создавать новые бронирования. Активные бронирования останутся."
              style={{ marginBottom: 16 }}
            />
          )}

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={saving}>
              Сохранить
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
