import { describe, expect, it } from 'vitest'
import { BOOKING_STATUS, PAYMENT_STATUS } from './statusMaps'

describe('status maps', () => {
  it('contains payment status metadata used by admin tables', () => {
    expect(PAYMENT_STATUS.CREATED).toEqual({
      label: 'Ожидает оплаты',
      variant: 'info',
      tagColor: 'warning',
    })
    expect(PAYMENT_STATUS.PAID.variant).toBe('success')
    expect(PAYMENT_STATUS.FAILED.tagColor).toBe('error')
    expect(PAYMENT_STATUS.REFUND.label).toBe('Возврат')
  })

  it('contains booking status metadata used by booking screens', () => {
    expect(BOOKING_STATUS.UPCOMING.label).toBe('Предстоящее')
    expect(BOOKING_STATUS.ACTIVE.variant).toBe('success')
    expect(BOOKING_STATUS.DONE.tagColor).toBe('default')
    expect(BOOKING_STATUS.CANCELED.variant).toBe('error')
  })
})
