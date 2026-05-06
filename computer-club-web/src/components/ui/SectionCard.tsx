import { CSSProperties, ReactNode } from 'react'
import { tokens } from '../../theme/tokens'

interface SectionCardProps {
  children: ReactNode
  title?: ReactNode
  extra?: ReactNode
  style?: CSSProperties
  /** Убрать внутренние отступы */
  noPadding?: boolean
}

export default function SectionCard({ children, title, extra, style, noPadding }: SectionCardProps) {
  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        overflow: 'hidden',
        ...style,
      }}
    >
      {(title || extra) && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 24px',
            borderBottom: `1px solid ${tokens.colors.border}`,
          }}
        >
          <span style={{ fontWeight: 600, fontSize: 16, color: tokens.colors.text }}>{title}</span>
          {extra && <div>{extra}</div>}
        </div>
      )}
      <div style={noPadding ? undefined : { padding: 24 }}>{children}</div>
    </div>
  )
}
