import { useEffect, useRef, useState } from 'react'
import { Spin, Typography } from 'antd'
import { loadYmaps } from '../utils/ymapsLoader'
import type { ClubListItemResponse } from '../types'

const { Text } = Typography

// Москва — центр по умолчанию, если ни один клуб не имеет координат
const DEFAULT_CENTER = [55.751574, 37.573856]
const DEFAULT_ZOOM = 10

interface Props {
  clubs: ClubListItemResponse[]
  favorites: Set<number>
  onBook: (clubId: number) => void
}

export default function ClubsMap({ clubs, favorites, onBook }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  // ref чтобы всегда иметь актуальный onBook без пересоздания карты
  const onBookRef = useRef(onBook)
  useEffect(() => { onBookRef.current = onBook }, [onBook])

  const [ready, setReady] = useState(false)
  const [loadError, setLoadError] = useState(false)

  useEffect(() => {
    let cancelled = false
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mapInstance: any = null

    // регистрируем глобальный колбэк до создания балунов
    window.__ymapsBook = (clubId: number) => onBookRef.current(clubId)

    loadYmaps()
      .then(() => {
        if (cancelled || !containerRef.current) return

        const withCoords = clubs.filter((c) => c.latitude !== null && c.longitude !== null)

        const center =
          withCoords.length > 0
            ? [withCoords[0].latitude!, withCoords[0].longitude!]
            : DEFAULT_CENTER

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const ymaps = (window as any).ymaps

        mapInstance = new ymaps.Map(containerRef.current, {
          center,
          zoom: DEFAULT_ZOOM,
          controls: ['zoomControl', 'geolocationControl', 'fullscreenControl'],
        })

        withCoords.forEach((club) => {
          const isFav = favorites.has(club.id)

          const placemark = new ymaps.Placemark(
            [club.latitude!, club.longitude!],
            {
              balloonContentHeader: `<strong>${club.name}</strong>`,
              balloonContentBody: `
                <div style="font-size:13px;color:#667085;margin-bottom:10px">${club.address}</div>
                <div style="display:flex;gap:8px">
                  <a
                    href="/clubs/${club.id}"
                    style="padding:4px 12px;background:#4F46E5;color:#fff;border-radius:8px;text-decoration:none;font-size:13px;font-weight:500"
                  >Подробнее</a>
                  <button
                    onclick="window.__ymapsBook(${club.id})"
                    style="padding:4px 12px;background:#fff;border:1px solid #E5E7EB;border-radius:8px;cursor:pointer;font-size:13px;color:#111827"
                  >Забронировать</button>
                </div>
              `,
              hintContent: club.name,
            },
            {
              // избранные выделяем другим цветом
              preset: isFav ? 'islands#redCircleDotIcon' : 'islands#blueCircleDotIcon',
            },
          )

          mapInstance.geoObjects.add(placemark)
        })

        // подгоняем область видимости под все метки
        if (withCoords.length > 1) {
          mapInstance.setBounds(mapInstance.geoObjects.getBounds(), {
            checkZoomRange: true,
            zoomMargin: 48,
          })
        }

        if (!cancelled) setReady(true)
      })
      .catch(() => {
        if (!cancelled) setLoadError(true)
      })

    return () => {
      cancelled = true
      if (mapInstance) mapInstance.destroy()
      delete window.__ymapsBook
    }
  }, [clubs, favorites]) // eslint-disable-line react-hooks/exhaustive-deps

  if (loadError) {
    return (
      <div style={{ height: 480, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f5f5', borderRadius: 8 }}>
        <Text type="secondary">Не удалось загрузить карту</Text>
      </div>
    )
  }

  return (
    <div style={{ position: 'relative' }}>
      {!ready && (
        <div style={{
          position: 'absolute', inset: 0, height: 480,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: '#f5f5f5', borderRadius: 8, zIndex: 1,
        }}>
          <Spin />
        </div>
      )}
      <div
        ref={containerRef}
        style={{ width: '100%', height: 480, borderRadius: 8, overflow: 'hidden' }}
      />
      <Text type="secondary" style={{ fontSize: 12, marginTop: 6, display: 'block' }}>
        Красные метки — избранные клубы
      </Text>
    </div>
  )
}
