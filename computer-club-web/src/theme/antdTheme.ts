import type { ThemeConfig } from 'antd'
import { tokens } from './tokens'

const { colors, radius } = tokens

export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary: colors.primary,
    colorBgLayout: colors.background,
    colorBgContainer: colors.surface,
    colorBgElevated: colors.surface,
    colorBorder: colors.border,
    colorText: colors.text,
    colorTextSecondary: colors.textSecondary,
    colorTextTertiary: colors.textMuted,
    colorSuccess: colors.success,
    colorWarning: colors.warning,
    colorError: colors.error,
    colorInfo: colors.info,
    borderRadius: radius.sm,
    borderRadiusLG: radius.md,
    borderRadiusSM: 6,
    fontSize: 14,
    fontSizeLG: 16,
    fontSizeSM: 12,
    lineHeight: 1.6,
    controlHeight: 38,
    boxShadow: tokens.shadow.card,
    boxShadowSecondary: tokens.shadow.hover,
  },
  components: {
    Card: {
      borderRadiusLG: radius.lg,
      boxShadow: tokens.shadow.card,
      paddingLG: 24,
    },
    Button: {
      borderRadius: radius.sm,
      primaryShadow: 'none',
      defaultShadow: 'none',
    },
    Input: {
      borderRadius: radius.sm,
    },
    Select: {
      borderRadius: radius.sm,
    },
    Table: {
      headerBg: colors.surfaceAlt,
      borderColor: colors.border,
      rowHoverBg: colors.primarySoft,
    },
    Menu: {
      itemBorderRadius: radius.sm,
      activeBarBorderWidth: 0,
    },
    Layout: {
      bodyBg: colors.background,
      siderBg: colors.surface,
      headerBg: colors.surface,
    },
    Tabs: {
      inkBarColor: colors.primary,
      itemActiveColor: colors.primary,
    },
    Badge: {
      colorBgContainer: colors.primary,
    },
  },
}
