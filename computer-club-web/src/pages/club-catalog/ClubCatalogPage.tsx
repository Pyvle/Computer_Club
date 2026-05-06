import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  Switch,
  Tag,
  Popconfirm,
  Select,
  Typography,
  Space,
  App,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { LinkOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import {
  AdminClubCatalogResponse,
  AdminClubCatalogProductResponse,
  AdminCategoryResponse,
  UpsertClubProductRequest,
} from '../../types'

interface UpsertForm {
  priceRub: number
  isAvailable: boolean
}

export default function ClubCatalogPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()

  const [catalog, setCatalog] = useState<AdminClubCatalogResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [filterCategoryId, setFilterCategoryId] = useState<number | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<AdminClubCatalogProductResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<UpsertForm>()

  async function fetchCatalog() {
    setLoading(true)
    try {
      const { data } = await apiClient.get<AdminClubCatalogResponse>(
        `/admin/clubs/${clubId}/catalog`
      )
      setCatalog(data)
    } catch {
      message.error('Не удалось загрузить каталог')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchCatalog()
  }, [clubId])

  function openUpsert(product: AdminClubCatalogProductResponse) {
    setEditingProduct(product)
    form.setFieldsValue({
      priceRub: product.clubPriceRub ?? 0,
      isAvailable: product.clubIsAvailable ?? true,
    })
    setModalOpen(true)
  }

  async function onUpsert(values: UpsertForm) {
    if (!editingProduct) return
    setSubmitting(true)
    try {
      const req: UpsertClubProductRequest = { priceRub: values.priceRub, isAvailable: values.isAvailable }
      await apiClient.put(`/admin/clubs/${clubId}/catalog/products/${editingProduct.productId}`, req)
      message.success(editingProduct.isLinkedToClub ? 'Изменения сохранены' : 'Товар подключён')
      setModalOpen(false)
      setEditingProduct(null)
      form.resetFields()
      await fetchCatalog()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    } finally {
      setSubmitting(false)
    }
  }

  async function onUnlink(productId: number) {
    try {
      await apiClient.delete(`/admin/clubs/${clubId}/catalog/products/${productId}`)
      message.success('Товар отключён')
      await fetchCatalog()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Ошибка')
    }
  }

  const categoryMap: Record<number, AdminCategoryResponse> = {}
  catalog?.categories.forEach((c) => { categoryMap[c.id] = c })

  const filteredProducts = (catalog?.products ?? []).filter(
    (p) => filterCategoryId === null || p.categoryId === filterCategoryId
  )

  const columns: ColumnsType<AdminClubCatalogProductResponse> = [
    {
      title: 'Название',
      dataIndex: 'productTitle',
      render: (title: string, row) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{title}</Typography.Text>
          {row.description && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {row.description}
            </Typography.Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Категория',
      dataIndex: 'categoryId',
      render: (id: number) => categoryMap[id]?.title ?? id,
      width: 160,
    },
    {
      title: 'Статус',
      dataIndex: 'isLinkedToClub',
      width: 140,
      render: (linked: boolean) =>
        linked ? (
          <Tag color="success">Подключён</Tag>
        ) : (
          <Tag color="default">Не подключён</Tag>
        ),
    },
    {
      title: 'Цена, ₽',
      dataIndex: 'clubPriceRub',
      width: 100,
      render: (price: number | null) => (price !== null ? price : '—'),
    },
    {
      title: 'Доступен',
      dataIndex: 'clubIsAvailable',
      width: 100,
      render: (val: boolean | null) =>
        val === null ? '—' : <Switch checked={val} disabled size="small" />,
    },
    {
      title: 'Действие',
      width: 180,
      render: (_, row) => (
        <Space>
          {!row.isLinkedToClub ? (
            <Button
              size="small"
              type="primary"
              icon={<LinkOutlined />}
              onClick={() => openUpsert(row)}
              disabled={!row.productIsActive}
            >
              Подключить
            </Button>
          ) : (
            <>
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => openUpsert(row)}
              >
                Изменить
              </Button>
              <Popconfirm
                title="Отключить товар?"
                description="Товар перестанет отображаться в меню клуба."
                onConfirm={() => onUnlink(row.productId)}
                okText="Отключить"
                cancelText="Отмена"
                okButtonProps={{ danger: true }}
              >
                <Button size="small" danger icon={<DeleteOutlined />} />
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  const linkedCount = (catalog?.products ?? []).filter((p) => p.isLinkedToClub).length

  return (
    <div>
      <PageHeader
        title="Каталог"
        subtitle="Управление товарами клуба — подключение глобальных товаров и настройка цен"
        extra={
          <Space>
            <Tag color="blue">{linkedCount} подключено</Tag>
            <Select
              allowClear
              placeholder="Все категории"
              style={{ width: 200 }}
              value={filterCategoryId}
              onChange={(v) => setFilterCategoryId(v ?? null)}
              options={catalog?.categories.map((c) => ({ value: c.id, label: c.title }))}
            />
          </Space>
        }
      />

      <SectionCard noPadding>
        <Table
          rowKey="productId"
          dataSource={filteredProducts}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          rowClassName={(row) => (!row.productIsActive ? 'ant-table-row-disabled' : '')}
        />
      </SectionCard>

      <Modal
        open={modalOpen}
        title={editingProduct?.isLinkedToClub ? 'Изменить настройки товара' : 'Подключить товар'}
        footer={null}
        onCancel={() => { setModalOpen(false); setEditingProduct(null); form.resetFields() }}
      >
        {editingProduct && (
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            {editingProduct.productTitle}
          </Typography.Text>
        )}
        <Form layout="vertical" form={form} onFinish={onUpsert}>
          <Form.Item
            name="priceRub"
            label="Цена в клубе (₽)"
            rules={[{ required: true, message: 'Укажите цену' }, { type: 'number', min: 0, message: 'Цена не может быть отрицательной' }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} autoFocus />
          </Form.Item>
          <Form.Item name="isAvailable" label="Доступен для заказа" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {editingProduct?.isLinkedToClub ? 'Сохранить' : 'Подключить'}
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
