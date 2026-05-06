/**
 * Единые маппинги статусов для всей административной панели.
 * Содержат label для отображения, variant для StatusBadge и tagColor для Ant Design Tag.
 */

import type { BookingStatus, PaymentStatus } from '../types'

type StatusVariant = 'success' | 'warning' | 'error' | 'info' | 'default'

export interface StatusMeta {
  label: string
  variant: StatusVariant
  /** Цвет для Ant Design <Tag color={...}> */
  tagColor: string
}

export const PAYMENT_STATUS: Record<PaymentStatus, StatusMeta> = {
  CREATED:  { label: 'Ожидает оплаты', variant: 'info',    tagColor: 'warning' },
  PAID:     { label: 'Оплачено',       variant: 'success', tagColor: 'success' },
  CANCELED: { label: 'Отменено',       variant: 'default', tagColor: 'default' },
  FAILED:   { label: 'Ошибка оплаты', variant: 'error',   tagColor: 'error'   },
  REFUND:   { label: 'Возврат',        variant: 'warning', tagColor: 'processing' },
}

export const BOOKING_STATUS: Record<BookingStatus, StatusMeta> = {
  UPCOMING: { label: 'Предстоящее', variant: 'info',    tagColor: 'blue'    },
  ACTIVE:   { label: 'Активное',    variant: 'success', tagColor: 'success' },
  DONE:     { label: 'Завершено',   variant: 'default', tagColor: 'default' },
  CANCELED: { label: 'Отменено',    variant: 'error',   tagColor: 'error'   },
}
