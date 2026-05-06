export const tokens = {
  colors: {
    primary: '#4F46E5',
    primaryHover: '#4338CA',
    primarySoft: '#EEF2FF',

    background: '#F6F8FC',
    surface: '#FFFFFF',
    surfaceAlt: '#F9FAFB',
    border: '#E5E7EB',

    text: '#111827',
    textSecondary: '#667085',
    textMuted: '#98A2B3',

    success: '#16A34A',
    successSoft: '#DCFCE7',
    warning: '#D97706',
    warningSoft: '#FEF3C7',
    error: '#DC2626',
    errorSoft: '#FEE2E2',
    info: '#2563EB',
    infoSoft: '#DBEAFE',
  },

  radius: {
    sm: 8,
    md: 12,
    lg: 16,
    xl: 20,
  },

  spacing: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    xxl: 32,
  },

  fontSize: {
    caption: 12,
    body: 14,
    bodyLg: 16,
    cardTitle: 16,
    sectionTitle: 20,
    pageTitle: 28,
  },

  shadow: {
    card: '0 4px 20px rgba(15, 23, 42, 0.06)',
    hover: '0 8px 30px rgba(15, 23, 42, 0.10)',
  },
} as const
