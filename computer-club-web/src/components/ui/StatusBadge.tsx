import { tokens } from '../../theme/tokens'

type StatusVariant = 'success' | 'warning' | 'error' | 'info' | 'default'

interface StatusBadgeProps {
  label: string
  variant?: StatusVariant
}

const variantMap: Record<StatusVariant, { color: string; bg: string }> = {
  success: { color: tokens.colors.success, bg: tokens.colors.successSoft },
  warning: { color: tokens.colors.warning, bg: tokens.colors.warningSoft },
  error:   { color: tokens.colors.error,   bg: tokens.colors.errorSoft   },
  info:    { color: tokens.colors.info,    bg: tokens.colors.infoSoft    },
  default: { color: tokens.colors.textSecondary, bg: tokens.colors.surfaceAlt },
}

export default function StatusBadge({ label, variant = 'default' }: StatusBadgeProps) {
  const { color, bg } = variantMap[variant]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '3px 10px',
        borderRadius: 20,
        fontSize: 12,
        fontWeight: 600,
        lineHeight: '18px',
        background: bg,
        color,
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </span>
  )
}
