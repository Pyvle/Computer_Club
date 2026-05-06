import { ReactNode } from 'react'
import { Typography } from 'antd'

interface PageHeaderProps {
  title: ReactNode
  subtitle?: ReactNode
  /** Кнопки действий справа */
  extra?: ReactNode
}

export default function PageHeader({ title, subtitle, extra }: PageHeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 24,
        gap: 16,
        flexWrap: 'wrap',
      }}
    >
      <div>
        <Typography.Title level={2} style={{ margin: 0, fontSize: 28, fontWeight: 700 }}>
          {title}
        </Typography.Title>
        {subtitle && (
          <Typography.Text type="secondary" style={{ display: 'block', marginTop: 4, fontSize: 14 }}>
            {subtitle}
          </Typography.Text>
        )}
      </div>
      {extra && <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>{extra}</div>}
    </div>
  )
}
