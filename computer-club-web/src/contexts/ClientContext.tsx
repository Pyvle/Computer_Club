import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react'
import apiClient from '../utils/apiClient'

export interface BookingDraft {
  clubId: number
  clubName: string
  date: string        // 'YYYY-MM-DD'
  startTime: string   // 'HH:mm'
  endTime: string     // 'HH:mm'
  packageId: number | null
  packageHours: number | null
  selectedSeatIds: number[]
}

interface ClientContextValue {
  bookingDraft: BookingDraft | null
  setBookingDraft: (draft: BookingDraft | null) => void
  cartCount: number
  /** Обновить счётчик корзины для конкретного клуба */
  refreshCartCount: (clubId: number) => Promise<void>
}

const ClientContext = createContext<ClientContextValue | null>(null)

const DRAFT_KEY = 'bookingDraft'

export function ClientProvider({ children }: { children: ReactNode }) {
  const [bookingDraft, setBookingDraftState] = useState<BookingDraft | null>(() => {
    try {
      const raw = sessionStorage.getItem(DRAFT_KEY)
      return raw ? (JSON.parse(raw) as BookingDraft) : null
    } catch {
      return null
    }
  })
  const [cartCount, setCartCount] = useState(0)

  const setBookingDraft = useCallback((draft: BookingDraft | null) => {
    setBookingDraftState(draft)
  }, [])

  useEffect(() => {
    if (bookingDraft) {
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(bookingDraft))
    } else {
      sessionStorage.removeItem(DRAFT_KEY)
    }
  }, [bookingDraft])

  const refreshCartCount = useCallback(async (clubId: number) => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setCartCount(0)
      return
    }
    try {
      const { data } = await apiClient.get(`/cart?clubId=${clubId}`)
      const count = (data.bookings?.length ?? 0) + (data.products?.length ?? 0)
      setCartCount(count)
    } catch {
      setCartCount(0)
    }
  }, [])

  return (
    <ClientContext.Provider value={{ bookingDraft, setBookingDraft, cartCount, refreshCartCount }}>
      {children}
    </ClientContext.Provider>
  )
}

export function useClient(): ClientContextValue {
  const ctx = useContext(ClientContext)
  if (!ctx) throw new Error('useClient must be used within ClientProvider')
  return ctx
}
