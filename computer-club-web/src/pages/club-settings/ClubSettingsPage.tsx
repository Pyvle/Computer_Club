import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Alert, App, Button, Form, Image, Input, List, Spin, Switch, Upload } from 'antd'
import {
  SearchOutlined,
  UploadOutlined,
  CheckCircleOutlined,
  EnvironmentOutlined,
  PictureOutlined,
  InfoCircleOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { tokens } from '../../theme/tokens'
import type {
  AddressSearchResult,
  ClubSettingsResponse,
  UpdateClubSettingsRequest,
} from '../../types'

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
    // addressShort формируется автоматически из Nominatim — руками не редактируется
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
    <div style={{ maxWidth: 700 }}>
      <PageHeader title="Настройки клуба" subtitle="Название, адрес, медиа и публикация" />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSave}
        onValuesChange={(changed) => {
          if ('isActive' in changed) setIsActive(changed.isActive)
          // сбрасываем подтверждённый адрес при ручном изменении поля
          if ('addressFull' in changed && confirmedAddress) {
            setConfirmedAddress(null)
            form.setFieldsValue({ addressShort: '', latitude: null, longitude: null })
          }
        }}
      >
        {/* скрытые поля — заполняются при выборе адреса */}
        <Form.Item name="addressShort" hidden><Input /></Form.Item>
        <Form.Item name="latitude"     hidden><Input /></Form.Item>
        <Form.Item name="longitude"    hidden><Input /></Form.Item>
        <Form.Item name="imageUrl"     hidden><Input /></Form.Item>

        {/* --- Секция 1: Основное --- */}
        <SectionCard
          title={<SectionTitle icon={<InfoCircleOutlined />} text="Основное" />}
          style={{ marginBottom: 16 }}
        >
          <Form.Item
            name="name"
            label="Название клуба"
            rules={[{ required: true, message: 'Введите название' }]}
          >
            <Input placeholder="Например: Club Zero" maxLength={120} />
          </Form.Item>

          <Form.Item name="description" label="Описание клуба" style={{ marginBottom: 0 }}>
            <Input.TextArea
              rows={4}
              placeholder="Краткое описание, которое видят пользователи в приложении"
            />
          </Form.Item>
        </SectionCard>

        {/* --- Секция 2: Адрес --- */}
        <SectionCard
          title={<SectionTitle icon={<EnvironmentOutlined />} text="Адрес" />}
          style={{ marginBottom: 16 }}
        >
          <Form.Item
            name="addressFull"
            label="Адрес клуба"
            rules={[{ required: true, message: 'Введите адрес для поиска' }]}
            extra="Введите адрес и нажмите «Найти», затем выберите из списка"
          >
            <Input
              placeholder="Москва, ул. Тверская, 1"
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
            <Alert type="warning" message={searchError} showIcon style={{ marginBottom: 12 }} />
          )}

          {searchResults.length > 0 && (
            <div
              style={{
                border: `1px solid ${tokens.colors.border}`,
                borderRadius: tokens.radius.md,
                overflow: 'hidden',
                marginBottom: 16,
              }}
            >
              <div
                style={{
                  padding: '8px 12px',
                  background: tokens.colors.surfaceAlt,
                  borderBottom: `1px solid ${tokens.colors.border}`,
                  fontSize: 12,
                  fontWeight: 600,
                  color: tokens.colors.textMuted,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                Найдено вариантов: {searchResults.length}
              </div>
              <List
                dataSource={searchResults}
                renderItem={(item, idx) => (
                  <List.Item
                    style={{
                      padding: '10px 16px',
                      cursor: 'pointer',
                      borderBottom: idx < searchResults.length - 1 ? `1px solid ${tokens.colors.border}` : 'none',
                      transition: 'background 0.1s',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = tokens.colors.surfaceAlt)}
                    onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                    onClick={() => handleSelectAddress(item)}
                  >
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text }}>
                        {item.addressShort}
                      </div>
                      <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 2 }}>
                        {item.addressFull}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </div>
          )}

          {confirmedAddress && (
            <div
              style={{
                background: tokens.colors.successSoft,
                border: `1px solid ${tokens.colors.success}40`,
                borderRadius: tokens.radius.md,
                padding: '12px 14px',
                marginBottom: 16,
                display: 'flex',
                gap: 10,
                alignItems: 'flex-start',
              }}
            >
              <CheckCircleOutlined style={{ color: tokens.colors.success, fontSize: 16, marginTop: 2, flexShrink: 0 }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 14, color: tokens.colors.text, marginBottom: 4 }}>
                  {confirmedAddress.addressShort}
                </div>
                <div style={{ fontSize: 12, color: tokens.colors.textSecondary, marginBottom: 2 }}>
                  {confirmedAddress.addressFull}
                </div>
                <div style={{ fontSize: 12, color: tokens.colors.textMuted }}>
                  {confirmedAddress.latitude.toFixed(6)}, {confirmedAddress.longitude.toFixed(6)}
                </div>
              </div>
            </div>
          )}

          <Form.Item name="locationText" label="Уточнение локации" style={{ marginBottom: 0 }}>
            <Input
              placeholder="3 этаж ТЦ «Галактика», вход со стороны парковки"
              maxLength={255}
            />
          </Form.Item>
        </SectionCard>

        {/* --- Секция 3: Фото --- */}
        <SectionCard
          title={<SectionTitle icon={<PictureOutlined />} text="Фото" />}
          style={{ marginBottom: 16 }}
        >
          <div style={{ display: 'flex', gap: 20, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontSize: 14, color: tokens.colors.textSecondary, marginBottom: 10 }}>
                Фото отображается на карточке клуба в приложении. Рекомендуемое соотношение 16:9, не более 5 МБ.
              </div>
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
                    message.success('Фото загружено')
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
            </div>

            {imagePreview && (
              <Image
                src={imagePreview.startsWith('/uploads') ? `http://localhost:8080${imagePreview}` : imagePreview}
                alt="Фото клуба"
                style={{ maxHeight: 160, maxWidth: 280, objectFit: 'cover', borderRadius: tokens.radius.md }}
              />
            )}

            {!imagePreview && (
              <div
                style={{
                  width: 280,
                  height: 160,
                  border: `2px dashed ${tokens.colors.border}`,
                  borderRadius: tokens.radius.md,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: tokens.colors.textMuted,
                  fontSize: 13,
                  gap: 6,
                }}
              >
                <PictureOutlined style={{ fontSize: 28 }} />
                Фото не загружено
              </div>
            )}
          </div>
        </SectionCard>

        {/* --- Секция 4: Публикация --- */}
        <SectionCard
          title={<SectionTitle icon={isActive ? <EyeOutlined /> : <EyeInvisibleOutlined />} text="Публикация" />}
          style={{ marginBottom: 24 }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Form.Item name="isActive" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch />
            </Form.Item>
            <div>
              <div style={{ fontSize: 14, fontWeight: 500, color: tokens.colors.text }}>
                {isActive ? 'Клуб активен и виден пользователям' : 'Клуб скрыт из приложения'}
              </div>
              <div style={{ fontSize: 12, color: tokens.colors.textMuted, marginTop: 2 }}>
                {isActive
                  ? 'Пользователи могут найти клуб и создать бронирование'
                  : 'Новые бронирования недоступны. Активные бронирования сохраняются.'}
              </div>
            </div>
          </div>

          {!isActive && (
            <Alert
              type="warning"
              showIcon
              message="Клуб будет скрыт из приложения"
              description="Пользователи не смогут видеть клуб и создавать новые бронирования. Активные бронирования останутся."
              style={{ marginTop: 14, marginBottom: 0 }}
            />
          )}
        </SectionCard>

        {/* Кнопка сохранения */}
        <Form.Item style={{ marginBottom: 0 }}>
          <Button type="primary" htmlType="submit" loading={saving} size="large">
            Сохранить настройки
          </Button>
        </Form.Item>
      </Form>
    </div>
  )
}

// вспомогательный компонент — заголовок секции с иконкой
function SectionTitle({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <span style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
      <span style={{ color: tokens.colors.primary, fontSize: 15 }}>{icon}</span>
      {text}
    </span>
  )
}
