import { useState, useEffect, useMemo } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Col, Input, Row, Spin, Tabs, Typography, App } from 'antd'
import { ShoppingCartOutlined } from '@ant-design/icons'
import apiClient from '../../../utils/apiClient'
import { useClient } from '../../../contexts/ClientContext'
import { useAuth } from '../../../contexts/AuthContext'
import { tokens } from '../../../theme/tokens'
import type { ClubProductClientResponse } from '../../../types'

const { Text } = Typography

export default function ShopPage() {
  const { clubId: clubIdParam } = useParams<{ clubId: string }>()
  const clubId = Number(clubIdParam)
  const navigate = useNavigate()
  const { refreshCartCount } = useClient()
  const { user } = useAuth()
  const { message } = App.useApp()

  const [products, setProducts] = useState<ClubProductClientResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [addingId, setAddingId] = useState<number | null>(null)

  const isLoggedIn = user !== null

  useEffect(() => {
    setLoading(true)
    apiClient.get<ClubProductClientResponse[]>(`/clubs/${clubId}/products`)
      .then(({ data }) => setProducts(data.filter((p) => p.isAvailable)))
      .catch(() => message.error('Не удалось загрузить товары'))
      .finally(() => setLoading(false))
  }, [clubId]) // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return products
    return products.filter((p) => p.title.toLowerCase().includes(q))
  }, [products, search])

  const grouped = useMemo(() => {
    const map = new Map<string, ClubProductClientResponse[]>()
    for (const p of filtered) {
      const list = map.get(p.categoryTitle) ?? []
      list.push(p)
      map.set(p.categoryTitle, list)
    }
    return map
  }, [filtered])

  async function handleAddToCart(productId: number) {
    if (!isLoggedIn) {
      navigate(`/login?from=/clubs/${clubId}/shop`)
      return
    }
    setAddingId(productId)
    try {
      await apiClient.post(`/cart/products?clubId=${clubId}`, { productId, qty: 1 })
      await refreshCartCount(clubId)
      message.success('Добавлено в корзину')
    } catch {
      message.error('Не удалось добавить товар')
    } finally {
      setAddingId(null)
    }
  }

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />

  const categories = Array.from(grouped.keys())

  return (
    <div>
      <Input.Search
        placeholder="Поиск товаров"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        allowClear
        style={{ maxWidth: 400, marginBottom: 20 }}
      />

      {categories.length === 0 ? (
        <Text type="secondary">Товары не найдены</Text>
      ) : (
        <Tabs
          items={categories.map((cat) => ({
            key: cat,
            label: cat,
            children: (
              <Row gutter={[16, 16]}>
                {(grouped.get(cat) ?? []).map((product) => (
                  <Col key={product.productId} xs={24} sm={12} lg={8}>
                    <Card
                      cover={
                        product.imageUrl ? (
                          <img
                            src={product.imageUrl}
                            alt={product.title}
                            style={{ height: 160, objectFit: 'cover' }}
                          />
                        ) : (
                          <div style={{ height: 160, background: tokens.colors.surfaceAlt, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <Text type="secondary">Нет фото</Text>
                          </div>
                        )
                      }
                      actions={[
                        <Button
                          key="add"
                          type="primary"
                          icon={<ShoppingCartOutlined />}
                          loading={addingId === product.productId}
                          onClick={() => handleAddToCart(product.productId)}
                        >
                          В корзину
                        </Button>,
                      ]}
                    >
                      <Card.Meta
                        title={
                          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <span>{product.title}</span>
                            <Text strong style={{ color: tokens.colors.primary, flexShrink: 0, marginLeft: 8 }}>
                              {product.priceRub} ₽
                            </Text>
                          </div>
                        }
                        description={product.description}
                      />
                    </Card>
                  </Col>
                ))}
              </Row>
            ),
          }))}
        />
      )}
    </div>
  )
}
