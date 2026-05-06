import { ReactNode, CSSProperties } from 'react'
import { tokens } from '../../theme/tokens'

interface StatCardProps {
  label: string
  value: ReactNode
  icon?: ReactNode
  /** Цвет иконки-бейджа (фон будет мягким) */
  accentColor?: string
  style?: CSSProperties
}

export default function StatCard({ label, value, icon, accentColor = tokens.colors.primary, style }: StatCardProps) {
  // конвертируем hex в мягкий фон: используем фиксированные пары из палитры
  const softBg =
    accentColor === tokens.colors.success ? tokens.colors.successSoft :
    accentColor === tokens.colors.warning ? tokens.colors.warningSoft :
    accentColor === tokens.colors.error   ? tokens.colors.errorSoft   :
    accentColor === tokens.colors.info    ? tokens.colors.infoSoft    :
    tokens.colors.primarySoft

  return (
    <div
      style={{
        background: tokens.colors.surface,
        border: `1px solid ${tokens.colors.border}`,
        borderRadius: tokens.radius.lg,
        boxShadow: tokens.shadow.card,
        padding: '20px 24px',
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        ...style,
      }}
    >
      {icon && (
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: tokens.radius.md,
            background: softBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: accentColor,
            fontSize: 22,
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
      )}
      <div>
        <div style={{ fontSize: 12, fontWeight: 500, color: tokens.colors.textMuted, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 4 }}>
          {label}
        </div>
        <div style={{ fontSize: 24, fontWeight: 700, color: tokens.colors.text, lineHeight: 1 }}>
          {value}
        </div>
      </div>
    </div>
  )
}
