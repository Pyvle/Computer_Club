import { useEffect, useState } from 'react'
import {
  App,
  Button,
  Form,
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
  Tooltip,
  Typography,
  Upload,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadRequestOption } from 'rc-upload/lib/interface'
import type { ColumnsType } from 'antd/es/table'
import apiClient from '../../utils/apiClient'
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
    setProdModalOpen(true)
  }

  async function handleProdSubmit(values: CreateProductRequest | UpdateProductRequest) {
    setProdSubmitting(true)
    try {
      if (prodModalMode === 'create') {
        await apiClient.post('/admin/global/catalog/products', values as CreateProductRequest)
        message.success('Товар создан')
      } else {
        await apiClient.put(
          `/admin/global/catalog/products/${selectedProduct!.id}`,
          values as UpdateProductRequest,
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
    { title: 'ID', dataIndex: 'id', width: 60 },
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
            description="Удаление невозможно, если в категории есть товары."
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

  function makeProductImageUploader(productId: number) {
    return async (options: UploadRequestOption) => {
      const formData = new FormData()
      formData.append('file', options.file as File)
      try {
        const res = await apiClient.post<{ imageUrl: string }>(
          `/admin/global/catalog/products/${productId}/image`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        )
        setProducts((prev) =>
          prev.map((p) => (p.id === productId ? { ...p, imageUrl: res.data.imageUrl } : p)),
        )
        message.success('Изображение загружено')
        options.onSuccess?.(res.data)
      } catch {
        message.error('Не удалось загрузить изображение')
        options.onError?.(new Error('Upload failed'))
      }
    }
  }

  const productColumns: ColumnsType<AdminProductResponse> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    {
      title: 'Фото',
      dataIndex: 'imageUrl',
      width: 72,
      render: (url: string | null, record) => (
        <Upload accept="image/*" showUploadList={false} customRequest={makeProductImageUploader(record.id)}>
          {url ? (
            <Tooltip title="Нажмите, чтобы заменить">
              <img
                src={url}
                alt="фото"
                style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 6, cursor: 'pointer', border: '1px solid #f0f0f0' }}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Загрузить фото">
              <Button icon={<UploadOutlined />} size="small" />
            </Tooltip>
          )}
        </Upload>
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
            description="Удаление невозможно, если товар привязан к каталогу клуба."
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
      <Typography.Title level={4} style={{ marginBottom: 16 }}>
        Глобальный каталог
      </Typography.Title>

      <Tabs
        defaultActiveKey="categories"
        items={[
          { key: 'categories', label: 'Категории', children: categoryTab },
          { key: 'products', label: 'Товары', children: productTab },
        ]}
      />

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
          <Form.Item name="isActive" label="Активен" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
