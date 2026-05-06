import { useEffect, useState } from 'react'
import {
  App,
  Button,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Upload,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import type {
  AdminCategoryResponse,
  AdminProductResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  CreateProductRequest,
  UpdateProductRequest,
} from '../../types'

type CategoryModalMode = 'create' | 'edit'
type ProductModalMode = 'create' | 'edit'

export default function GlobalCatalogPage() {
  const { message } = App.useApp()

  // --- Категории ---
  const [categories, setCategories] = useState<AdminCategoryResponse[]>([])
  const [catLoading, setCatLoading] = useState(false)
  const [catModalOpen, setCatModalOpen] = useState(false)
  const [catModalMode, setCatModalMode] = useState<CategoryModalMode>('create')
  const [selectedCategory, setSelectedCategory] = useState<AdminCategoryResponse | null>(null)
  const [catSubmitting, setCatSubmitting] = useState(false)
  const [catForm] = Form.useForm()

  // --- Товары ---
  const [products, setProducts] = useState<AdminProductResponse[]>([])
  const [prodLoading, setProdLoading] = useState(false)
  const [prodModalOpen, setProdModalOpen] = useState(false)
  const [prodModalMode, setProdModalMode] = useState<ProductModalMode>('create')
  const [selectedProduct, setSelectedProduct] = useState<AdminProductResponse | null>(null)
  const [prodSubmitting, setProdSubmitting] = useState(false)
  const [prodForm] = Form.useForm()
  const [prodImageUrl, setProdImageUrl] = useState<string | null>(null)
  const [prodImageUploading, setProdImageUploading] = useState(false)

  async function fetchCategories() {
    setCatLoading(true)
    try {
      const res = await apiClient.get<AdminCategoryResponse[]>('/admin/global/catalog/categories')
      setCategories(res.data)
    } catch {
      message.error('Не удалось загрузить категории')
    } finally {
      setCatLoading(false)
    }
  }

  async function fetchProducts() {
    setProdLoading(true)
    try {
      const res = await apiClient.get<AdminProductResponse[]>('/admin/global/catalog/products')
      setProducts(res.data)
    } catch {
      message.error('Не удалось загрузить товары')
    } finally {
      setProdLoading(false)
    }
  }

  useEffect(() => {
    fetchCategories()
    fetchProducts()
  }, [])

  // --- Категории: модал ---
  function openCatCreate() {
    setCatModalMode('create')
    setSelectedCategory(null)
    catForm.resetFields()
    catForm.setFieldsValue({ sortOrder: 0, isActive: true })
    setCatModalOpen(true)
  }

  function openCatEdit(cat: AdminCategoryResponse) {
    setCatModalMode('edit')
    setSelectedCategory(cat)
    catForm.setFieldsValue({ title: cat.title, sortOrder: cat.sortOrder, isActive: cat.isActive })
    setCatModalOpen(true)
  }

  async function handleCatSubmit(values: CreateCategoryRequest | UpdateCategoryRequest) {
    setCatSubmitting(true)
    try {
      if (catModalMode === 'create') {
        await apiClient.post('/admin/global/catalog/categories', values as CreateCategoryRequest)
        message.success('Категория создана')
      } else {
        await apiClient.put(
          `/admin/global/catalog/categories/${selectedCategory!.id}`,
          values as UpdateCategoryRequest,
        )
        message.success('Категория обновлена')
      }
      setCatModalOpen(false)
      fetchCategories()
    } catch {
      message.error('Ошибка при сохранении категории')
    } finally {
      setCatSubmitting(false)
    }
  }

  async function handleCatDelete(id: number) {
    try {
      await apiClient.delete(`/admin/global/catalog/categories/${id}`)
      message.success('Категория удалена')
      fetchCategories()
      fetchProducts()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Не удалось удалить категорию')
    }
  }

  // --- Товары: модал ---
  function openProdCreate() {
    setProdModalMode('create')
    setSelectedProduct(null)
    prodForm.resetFields()
    prodForm.setFieldsValue({ isActive: true })
    setProdImageUrl(null)
    setProdModalOpen(true)
  }

  function openProdEdit(prod: AdminProductResponse) {
    setProdModalMode('edit')
    setSelectedProduct(prod)
    prodForm.setFieldsValue({
      categoryId: prod.categoryId,
      title: prod.title,
      description: prod.description ?? '',
      isActive: prod.isActive,
    })
    setProdImageUrl(prod.imageUrl)
    setProdModalOpen(true)
  }

  async function handleProdSubmit(values: CreateProductRequest | UpdateProductRequest) {
    setProdSubmitting(true)
    try {
      const payload = { ...values, imageUrl: prodImageUrl ?? null }
      if (prodModalMode === 'create') {
        await apiClient.post('/admin/global/catalog/products', payload as CreateProductRequest)
        message.success('Товар создан')
      } else {
        await apiClient.put(
          `/admin/global/catalog/products/${selectedProduct!.id}`,
          payload as UpdateProductRequest,
        )
        message.success('Товар обновлён')
      }
      setProdModalOpen(false)
      fetchProducts()
    } catch {
      message.error('Ошибка при сохранении товара')
    } finally {
      setProdSubmitting(false)
    }
  }

  async function handleProdDelete(id: number) {
    try {
      await apiClient.delete(`/admin/global/catalog/products/${id}`)
      message.success('Товар удалён')
      fetchProducts()
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? 'Не удалось удалить товар')
    }
  }

  const categoryColumns: ColumnsType<AdminCategoryResponse> = [
    { title: 'ID', dataIndex: 'id', width: 60, sorter: (a, b) => Number(a.id) - Number(b.id) },
    { title: 'Название', dataIndex: 'title' },
    { title: 'Порядок', dataIndex: 'sortOrder', width: 100 },
    {
      title: 'Активна',
      dataIndex: 'isActive',
      width: 100,
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? 'Да' : 'Нет'}</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 160,
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} size="small" onClick={() => openCatEdit(record)}>
            Изменить
          </Button>
          <Popconfirm
            title="Удалить категорию?"
            description="Все товары категории и их привязки к клубам будут удалены. Невозможно, если любой из товаров встречается в истории покупок."
            okText="Удалить"
            cancelText="Отмена"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleCatDelete(record.id)}
          >
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const productColumns: ColumnsType<AdminProductResponse> = [
    { title: 'ID', dataIndex: 'id', width: 60, sorter: (a, b) => Number(a.id) - Number(b.id) },
    {
      title: 'Фото',
      dataIndex: 'imageUrl',
      width: 72,
      render: (url: string | null) =>
        url ? (
          <Image src={url} width={48} height={48} style={{ objectFit: 'cover', borderRadius: 4 }} />
        ) : (
          <div style={{ width: 48, height: 48, background: '#f0f0f0', borderRadius: 4 }} />
        ),
    },
    {
      title: 'Категория',
      dataIndex: 'categoryId',
      width: 160,
      render: (id: number) => categories.find((c) => c.id === id)?.title ?? `#${id}`,
    },
    { title: 'Название', dataIndex: 'title' },
    {
      title: 'Описание',
      dataIndex: 'description',
      render: (v: string | null) => v ?? '—',
    },
    {
      title: 'Активен',
      dataIndex: 'isActive',
      width: 100,
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? 'Да' : 'Нет'}</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 160,
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} size="small" onClick={() => openProdEdit(record)}>
            Изменить
          </Button>
          <Popconfirm
            title="Удалить товар?"
            description="Товар будет удалён из глобального каталога и каталогов всех клубов. Удаление невозможно, если товар встречается в истории покупок."
            okText="Удалить"
            cancelText="Отмена"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleProdDelete(record.id)}
          >
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const categoryTab = (
    <>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCatCreate}>
          Добавить категорию
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={categoryColumns}
        dataSource={categories}
        loading={catLoading}
        pagination={{ pageSize: 20 }}
        size="middle"
      />
    </>
  )

  const productTab = (
    <>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openProdCreate}>
          Добавить товар
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={productColumns}
        dataSource={products}
        loading={prodLoading}
        pagination={{ pageSize: 20 }}
        size="middle"
      />
    </>
  )

  return (
    <>
      <PageHeader title="Глобальный каталог" subtitle="Категории и товары, доступные для подключения в любом клубе платформы" />

      <SectionCard>
        <Tabs
          defaultActiveKey="categories"
          items={[
            { key: 'categories', label: 'Категории', children: categoryTab },
            { key: 'products', label: 'Товары', children: productTab },
          ]}
        />
      </SectionCard>

      {/* Модал категории */}
      <Modal
        open={catModalOpen}
        title={catModalMode === 'create' ? 'Новая категория' : 'Редактировать категорию'}
        onCancel={() => setCatModalOpen(false)}
        onOk={() => catForm.submit()}
        okText="Сохранить"
        cancelText="Отмена"
        confirmLoading={catSubmitting}
        destroyOnClose
      >
        <Form form={catForm} layout="vertical" onFinish={handleCatSubmit} style={{ marginTop: 16 }}>
          <Form.Item name="title" label="Название" rules={[{ required: true, message: 'Введите название' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sortOrder" label="Порядок сортировки" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          {catModalMode === 'edit' && (
            <Form.Item name="isActive" label="Активна" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
        </Form>
      </Modal>

      {/* Модал товара */}
      <Modal
        open={prodModalOpen}
        title={prodModalMode === 'create' ? 'Новый товар' : 'Редактировать товар'}
        onCancel={() => setProdModalOpen(false)}
        onOk={() => prodForm.submit()}
        okText="Сохранить"
        cancelText="Отмена"
        confirmLoading={prodSubmitting}
        destroyOnClose
      >
        <Form form={prodForm} layout="vertical" onFinish={handleProdSubmit} style={{ marginTop: 16 }}>
          <Form.Item name="categoryId" label="Категория" rules={[{ required: true, message: 'Выберите категорию' }]}>
            <Select
              placeholder="Выберите категорию"
              options={categories
                .filter((c) => c.isActive)
                .map((c) => ({ label: c.title, value: c.id }))}
            />
          </Form.Item>
          <Form.Item name="title" label="Название" rules={[{ required: true, message: 'Введите название' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Описание">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="Фото товара">
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Upload
                accept="image/jpeg,image/png,image/webp"
                showUploadList={false}
                customRequest={async ({ file, onSuccess, onError }) => {
                  setProdImageUploading(true)
                  const formData = new FormData()
                  formData.append('file', file as File)
                  try {
                    const res = await apiClient.post<{ path: string }>(
                      '/admin/uploads/product-image',
                      formData,
                      { headers: { 'Content-Type': 'multipart/form-data' } },
                    )
                    setProdImageUrl(res.data.path)
                    onSuccess?.(res.data)
                    message.success('Фото загружено')
                  } catch (e) {
                    message.error('Не удалось загрузить фото')
                    onError?.(e as Error)
                  } finally {
                    setProdImageUploading(false)
                  }
                }}
              >
                <Button icon={<UploadOutlined />} loading={prodImageUploading}>
                  {prodImageUrl ? 'Заменить фото' : 'Загрузить фото'}
                </Button>
              </Upload>
              {prodImageUrl && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Image src={prodImageUrl} width={56} height={56} style={{ objectFit: 'cover', borderRadius: 4 }} />
                  <Button
                    size="small"
                    danger
                    onClick={() => setProdImageUrl(null)}
                  >
                    Удалить
                  </Button>
                </div>
              )}
            </div>
          </Form.Item>
          <Form.Item name="isActive" label="Активен" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
