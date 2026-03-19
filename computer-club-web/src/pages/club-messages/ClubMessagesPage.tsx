import { useEffect, useState } from 'react'
import { Tabs, List, Typography, Tag, App } from 'antd'
import { WarningOutlined, UserOutlined } from '@ant-design/icons'
import { useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import apiClient from '../../utils/apiClient'
import type { ClubUserReportResponse, PlatformMessageResponse } from '../../types'

const { Text } = Typography

export default function ClubMessagesPage() {
  const { clubId } = useParams<{ clubId: string }>()
  const { message } = App.useApp()

  const [userReports, setUserReports] = useState<ClubUserReportResponse[]>([])
  const [platformMessages, setPlatformMessages] = useState<PlatformMessageResponse[]>([])
  const [reportsLoading, setReportsLoading] = useState(true)
  const [platformLoading, setPlatformLoading] = useState(true)

  useEffect(() => {
    if (!clubId) return
    setReportsLoading(true)
    apiClient
      .get<ClubUserReportResponse[]>(`/admin/clubs/${clubId}/user-reports`)
      .then(({ data }) => setUserReports(data))
      .catch(() => message.error('Не удалось загрузить жалобы'))
      .finally(() => setReportsLoading(false))
  }, [clubId])

  useEffect(() => {
    if (!clubId) return
    setPlatformLoading(true)
    apiClient
      .get<PlatformMessageResponse[]>(`/admin/clubs/${clubId}/platform-messages`)
      .then(({ data }) => setPlatformMessages(data))
      .catch(() => message.error('Не удалось загрузить сообщения платформы'))
      .finally(() => setPlatformLoading(false))
  }, [clubId])

  const tabs = [
    {
      key: 'reports',
      label: (
        <span>
          <UserOutlined /> От пользователей{' '}
          {userReports.length > 0 && <Tag color="red">{userReports.length}</Tag>}
        </span>
      ),
      children: (
        <List
          loading={reportsLoading}
          dataSource={userReports}
          locale={{ emptyText: 'Жалоб нет' }}
          renderItem={(r) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <span>
                    <Text strong>{r.userPhone ?? `Пользователь #${r.userId}`}</Text>
                    <Text type="secondary" style={{ marginLeft: 12, fontSize: 12 }}>
                      {dayjs(r.createdAt).format('DD.MM.YYYY HH:mm')}
                    </Text>
                  </span>
                }
                description={r.message}
              />
            </List.Item>
          )}
        />
      ),
    },
    {
      key: 'platform',
      label: (
        <span>
          <WarningOutlined /> От администрации платформы{' '}
          {platformMessages.length > 0 && <Tag color="orange">{platformMessages.length}</Tag>}
        </span>
      ),
      children: (
        <List
          loading={platformLoading}
          dataSource={platformMessages}
          locale={{ emptyText: 'Сообщений нет' }}
          renderItem={(m) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {dayjs(m.createdAt).format('DD.MM.YYYY HH:mm')}
                  </Text>
                }
                description={m.message}
              />
            </List.Item>
          )}
        />
      ),
    },
  ]

  return (
    <div>
      <h2 style={{ marginTop: 0, marginBottom: 16 }}>Сообщения</h2>
      <Tabs items={tabs} />
    </div>
  )
}
