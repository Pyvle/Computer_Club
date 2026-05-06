import { ReactNode } from 'react'
import { Button } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { tokens } from '../../theme/tokens'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
}

export default function EmptyState({ icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 24px',
        textAlign: 'center',
      }}
    >
      <div
        style={{
          width: 64,
          height: 64,
          borderRadius: tokens.radius.xl,
          background: tokens.colors.primarySoft,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: tokens.colors.primary,
          fontSize: 28,
          marginBottom: 16,
        }}
      >
        {icon ?? <InboxOutlined />}
      </div>
      <div style={{ fontSize: 16, fontWeight: 600, color: tokens.colors.text, marginBottom: 6 }}>
        {title}
      </div>
      {description && (
        <div style={{ fontSize: 14, color: tokens.colors.textSecondary, marginBottom: 20, maxWidth: 320 }}>
          {description}
        </div>
      )}
      {actionLabel && onAction && (
        <Button type="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  )
}
