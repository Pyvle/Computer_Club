import { CheckOutlined } from '@ant-design/icons'
import { tokens } from '../../theme/tokens'

interface Props {
  steps: string[]
  /** 0-indexed */
  current: number
}

export default function StepBar({ steps, current }: Props) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: 28 }}>
      {steps.map((label, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', flex: i < steps.length - 1 ? 1 : undefined }}>
          {/* Шаг: кружок + текст */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
            <div style={{
              width: 28, height: 28, borderRadius: '50%',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 13, fontWeight: 700,
              background: i < current
                ? tokens.colors.success
                : i === current
                  ? tokens.colors.primary
                  : tokens.colors.surfaceAlt,
              color: i <= current ? '#fff' : tokens.colors.textMuted,
              border: `2px solid ${
                i < current
                  ? tokens.colors.success
                  : i === current
                    ? tokens.colors.primary
                    : tokens.colors.border
              }`,
            }}>
              {i < current ? <CheckOutlined style={{ fontSize: 11 }} /> : i + 1}
            </div>
            <span style={{
              fontSize: 13,
              fontWeight: i === current ? 600 : 400,
              color: i === current ? tokens.colors.text : tokens.colors.textMuted,
            }}>
              {label}
            </span>
          </div>

          {/* Соединитель */}
          {i < steps.length - 1 && (
            <div style={{
              flex: 1,
              height: 2,
              background: i < current ? tokens.colors.success : tokens.colors.border,
              margin: '0 12px',
              minWidth: 24,
            }} />
          )}
        </div>
      ))}
    </div>
  )
}
