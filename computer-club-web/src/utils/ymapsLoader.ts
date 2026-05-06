const YMAPS_API_KEY = import.meta.env.VITE_YMAPS_API_KEY
const YMAPS_SRC = `https://api-maps.yandex.ru/2.1/?apikey=${YMAPS_API_KEY}&lang=ru_RU`

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ymaps: any
    __ymapsBook?: (clubId: number) => void
  }
}

let loadPromise: Promise<void> | null = null

export function loadYmaps(): Promise<void> {
  if (loadPromise) return loadPromise
  if (!YMAPS_API_KEY) {
    return Promise.reject(new Error('Не задан VITE_YMAPS_API_KEY'))
  }

  loadPromise = new Promise((resolve, reject) => {
    if (window.ymaps) {
      window.ymaps.ready(resolve)
      return
    }

    const script = document.createElement('script')
    script.src = YMAPS_SRC
    script.async = true
    script.onload = () => window.ymaps.ready(resolve)
    script.onerror = () => reject(new Error('Не удалось загрузить Яндекс.Карты'))
    document.head.appendChild(script)
  })

  return loadPromise
}
