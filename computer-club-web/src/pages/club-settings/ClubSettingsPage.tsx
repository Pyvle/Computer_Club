import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Alert, App, Button, Card, Form, Image, Input, List, Spin, Switch, Typography, Upload } from 'antd'
import { SearchOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadFile } from 'antd'
import apiClient from '../../utils/apiClient'
import type {
  AddressSearchResult,
  ClubSettingsResponse,
  UpdateClubSettingsRequest,
} from '../../types'

const { Text } = Typography

export default function ClubSettingsPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()
  const [form] = Form.useForm<UpdateClubSettingsRequest>()

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [isActive, setIsActive] = useState(true)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)

  // --- Поиск адреса ---
  const [searching, setSearching] = useState(false)
  const [searchResults, setSearchResults] = useState<AddressSearchResult[]>([])
  const [searchError, setSearchError] = useState<string | null>(null)
  const [confirmedAddress, setConfirmedAddress] = useState<AddressSearchResult | null>(null)

  useEffect(() => {
    if (!clubId) return
    setLoading(true)
    apiClient
      .get<ClubSettingsResponse>(`/admin/clubs/${clubId}/settings`)
      .then((r) => {
        form.setFieldsValue(r.data)
        setIsActive(r.data.isActive)
        setImagePreview(r.data.imageUrl)
        if (r.data.latitude != null && r.data.longitude != null) {
          setConfirmedAddress({
            addressFull: r.data.addressFull,
            addressShort: r.data.addressShort,
            latitude: r.data.latitude,
            longitude: r.data.longitude,
          })
        }
      })
      .catch(() => message.error('Не удалось загрузить настройки клуба'))
      .finally(() => setLoading(false))
  }, [clubId])

  async function handleFindAddress() {
    const query = form.getFieldValue('addressFull') as string
    if (!query?.trim()) return
    setSearching(true)
    setSearchError(null)
    setSearchResults([])
    try {
      const res = await apiClient.get<AddressSearchResult[]>('/admin/geo/search', {
        params: { query: query.trim() },
      })
      if (res.data.length === 0) {
        setSearchError('Ничего не найдено — уточните запрос')
      } else {
        setSearchResults(res.data)
      }
    } catch {
      setSearchError('Не удалось выполнить поиск адреса')
    } finally {
      setSearching(false)
    }
  }

  function handleSelectAddress(item: AddressSearchResult) {
    // addressShort формируется автоматически из Nominatim — глава не редактирует
    form.setFieldsValue({
      addressFull: item.addressFull,
      addressShort: item.addressShort,
      latitude: item.latitude,
      longitude: item.longitude,
    })
    setConfirmedAddress(item)
    setSearchResults([])
    setSearchError(null)
  }

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

      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
          onValuesChange={(changed) => {
            if ('isActive' in changed) setIsActive(changed.isActive)
            // сбрасываем подтверждение при ручном изменении адреса
            if ('addressFull' in changed && confirmedAddress) {
              setConfirmedAddress(null)
              form.setFieldsValue({ addressShort: '', latitude: null, longitude: null })
            }
          }}
        >
          <Form.Item
            name="name"
            label="Название клуба"
            rules={[{ required: true, message: 'Введите название' }]}
          >
            <Input placeholder="Например: Club Zero" maxLength={120} />
          </Form.Item>

          {/* скрытые поля — заполняются при выборе адреса из поиска */}
          <Form.Item name="addressShort" hidden><Input /></Form.Item>
          <Form.Item name="latitude" hidden><Input /></Form.Item>
          <Form.Item name="longitude" hidden><Input /></Form.Item>

          <Form.Item
            name="addressFull"
            label="Адрес клуба"
            rules={[{ required: true, message: 'Введите адрес для поиска' }]}
          >
            <Input
              placeholder="Введите адрес и нажмите Найти"
              maxLength={500}
              suffix={
                <Button
                  type="link"
                  size="small"
                  icon={<SearchOutlined />}
                  loading={searching}
                  onClick={handleFindAddress}
                  style={{ padding: 0, height: 'auto' }}
                >
                  Найти
                </Button>
              }
              onPressEnter={handleFindAddress}
            />
          </Form.Item>

          {searchError && (
            <Alert
              type="warning"
              message={searchError}
              showIcon
              style={{ marginBottom: 12 }}
            />
          )}

          {searchResults.length > 0 && (
            <Card
              size="small"
              title="Найденные адреса"
              style={{ marginBottom: 16 }}
              styles={{ body: { padding: 0 } }}
            >
              <List
                dataSource={searchResults}
                renderItem={(item) => (
                  <List.Item
                    style={{ padding: '10px 16px', cursor: 'pointer' }}
                    onClick={() => handleSelectAddress(item)}
                  >
                    <div>
                      <div>
                        <Text strong>{item.addressShort}</Text>
                      </div>
                      <div>
                        <Text type="secondary" style={{ fontSize: 12 }}>{item.addressFull}</Text>
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </Card>
          )}

          {confirmedAddress && (
            <Card
              size="small"
              style={{ marginBottom: 16, background: '#f6ffed', borderColor: '#b7eb8f' }}
            >
              <div style={{ marginBottom: 8 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>Короткий адрес (в приложении):</Text>
                <br />
                <Text strong>{confirmedAddress.addressShort}</Text>
              </div>
              <div style={{ marginBottom: 8 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>Полный адрес:</Text>
                <br />
                <Text>{confirmedAddress.addressFull}</Text>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>Координаты: </Text>
                <Text style={{ fontSize: 12 }}>
                  {confirmedAddress.latitude.toFixed(6)}, {confirmedAddress.longitude.toFixed(6)}
                </Text>
              </div>
            </Card>
          )}

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

          <Form.Item name="imageUrl" hidden><Input /></Form.Item>

          <Form.Item label="Фото клуба" colon={false}>
            <Upload
              accept="image/jpeg,image/png,image/webp"
              showUploadList={false}
              customRequest={async ({ file, onSuccess, onError }) => {
                setUploading(true)
                const formData = new FormData()
                formData.append('file', file as File)
                try {
                  const res = await apiClient.post<{ path: string }>(
                    '/admin/uploads/club-image',
                    formData,
                    { headers: { 'Content-Type': 'multipart/form-data' } }
                  )
                  form.setFieldValue('imageUrl', res.data.path)
                  setImagePreview(res.data.path)
                  onSuccess?.(res.data)
                } catch (e) {
                  message.error('Не удалось загрузить фото')
                  onError?.(e as Error)
                } finally {
                  setUploading(false)
                }
              }}
            >
              <Button icon={<UploadOutlined />} loading={uploading}>
                {imagePreview ? 'Заменить фото' : 'Загрузить фото'}
              </Button>
            </Upload>
          </Form.Item>

          {imagePreview && (
            <Form.Item>
              <Image
                src={imagePreview.startsWith('/uploads') ? `http://localhost:8080${imagePreview}` : imagePreview}
                alt="Предпросмотр"
                style={{ maxHeight: 200, objectFit: 'cover', borderRadius: 8 }}
              />
            </Form.Item>
          )}

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
