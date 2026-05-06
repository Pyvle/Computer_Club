import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Col, DatePicker, Row, Spin, TimePicker, App } from 'antd'
import {
  ClockCircleOutlined,
  CalendarOutlined,
  ArrowRightOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons'
import dayjs, { Dayjs } from 'dayjs'
import apiClient from '../../../utils/apiClient'
import { useClient } from '../../../contexts/ClientContext'
import SectionCard from '../../../components/ui/SectionCard'
import StepBar from '../../../components/ui/StepBar'
import { tokens } from '../../../theme/tokens'
import type { TimePackageClientResponse, SeatPriceClientResponse } from '../../../types'

const BOOKING_STEPS = ['Время', 'Места', 'Корзина']

// --- Блок резюме (правая колонка) ---

function BookingSummary({
  clubName,
  date,
  startTime,
  endTime,
  durationHours,
  estimatedPrice,
  canProceed,
  onProceed,
}: {
  clubName: string
  date: Dayjs | null
  startTime: Dayjs | null
  endTime: Dayjs | null
  durationHours: number | null
  estimatedPrice: number | null
  canProceed: boolean
  onProceed: () => void
}) {
  return (
    <div style={{
      background: tokens.colors.surface,
      border: `1px solid ${tokens.colors.border}`,
      borderRadius: tokens.radius.lg,
      boxShadow: tokens.shadow.card,
      overflow: 'hidden',
      position: 'sticky',
      top: 80,
    }}>
      {/* Заголовок */}
      <div style={{
        padding: '14px 20px',
        borderBottom: `1px solid ${tokens.colors.border}`,
        background: tokens.colors.surfaceAlt,
        fontWeight: 700,
        fontSize: 14,
        color: tokens.colors.text,
      }}>
        Ваш сеанс
      </div>

      <div style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {/* Клуб */}
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: 13 }}>
          <EnvironmentOutlined style={{ color: tokens.colors.primary, marginTop: 2, flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginBottom: 1 }}>Клуб</div>
            <div style={{ fontWeight: 600, color: tokens.colors.text }}>{clubName || '—'}</div>
          </div>
        </div>

        {/* Дата */}
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: 13 }}>
          <CalendarOutlined style={{ color: tokens.colors.primary, marginTop: 2, flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginBottom: 1 }}>Дата</div>
            <div style={{ fontWeight: 600, color: date ? tokens.colors.text : tokens.colors.textMuted }}>
              {date ? date.format('D MMMM, dddd') : 'Не выбрана'}
            </div>
          </div>
        </div>

        {/* Время */}
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: 13 }}>
          <ClockCircleOutlined style={{ color: tokens.colors.primary, marginTop: 2, flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginBottom: 1 }}>Время</div>
            <div style={{ fontWeight: 600, color: startTime && endTime ? tokens.colors.text : tokens.colors.textMuted }}>
              {startTime && endTime
                ? `${startTime.format('HH:mm')} — ${endTime.format('HH:mm')}`
                : 'Не выбрано'}
            </div>
          </div>
        </div>

        {/* Длительность */}
        {durationHours !== null && durationHours > 0 && (
          <div style={{
            padding: '10px 14px',
            background: tokens.colors.primarySoft,
            borderRadius: tokens.radius.sm,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}>
            <span style={{ fontSize: 13, color: tokens.colors.primary, fontWeight: 500 }}>
              {durationHours % 1 === 0 ? durationHours : durationHours.toFixed(1)} ч
            </span>
            {estimatedPrice !== null && (
              <span style={{ fontSize: 13, fontWeight: 700, color: tokens.colors.primary }}>
                от {estimatedPrice} ₽
              </span>
            )}
          </div>
        )}

        {/* Подсказка под ценой */}
        {estimatedPrice !== null && (
          <div style={{ fontSize: 11, color: tokens.colors.textMuted }}>
            Стоимость за одно стандартное место. Итог зависит от типа выбранных мест.
          </div>
        )}
      </div>

      {/* CTA */}
      <div style={{ padding: '0 20px 20px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Button
          type="primary"
          size="large"
          icon={<ArrowRightOutlined />}
          iconPosition="end"
          block
          disabled={!canProceed}
          onClick={onProceed}
          style={{ fontWeight: 600 }}
        >
          Выбрать место
        </Button>
        {!canProceed && (
          <div style={{ fontSize: 11, color: tokens.colors.textMuted, textAlign: 'center' }}>
            Выберите дату и время сеанса
          </div>
        )}
      </div>
    </div>
  )
}

// --- Кнопки быстрого выбора длительности ---

function DurationPresets({
  selected,
  onSelect,
}: {
  selected: number | null
  onSelect: (hours: number) => void
}) {
  const presets = [
    { hours: 1, label: '1 ч' },
    { hours: 2, label: '2 ч' },
    { hours: 3, label: '3 ч' },
    { hours: 4, label: '4 ч' },
  ]

  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      {presets.map((p) => {
        const active = selected === p.hours
        return (
          <button
            key={p.hours}
            onClick={() => onSelect(p.hours)}
            style={{
              padding: '8px 18px',
              borderRadius: tokens.radius.sm,
              border: `2px solid ${active ? tokens.colors.primary : tokens.colors.border}`,
              background: active ? tokens.colors.primarySoft : tokens.colors.surface,
              color: active ? tokens.colors.primary : tokens.colors.text,
              fontWeight: active ? 700 : 500,
              fontSize: 14,
              cursor: 'pointer',
              transition: 'all 0.15s',
            }}
          >
            {p.label}
          </button>
        )
      })}
    </div>
  )
}

// --- Основной компонент ---

export default function BookingSetupPage() {
  const { clubId: clubIdParam } = useParams<{ clubId: string }>()
  const clubId = Number(clubIdParam)
  const navigate = useNavigate()
  const { bookingDraft, setBookingDraft } = useClient()
  const { message } = App.useApp()

  const [packages, setPackages] = useState<TimePackageClientResponse[]>([])
  const [seatPrices, setSeatPrices] = useState<SeatPriceClientResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [clubName, setClubName] = useState('')

  // инициализируем из черновика если clubId совпадает
  const draft = bookingDraft?.clubId === clubId ? bookingDraft : null

  const [date, setDate] = useState<Dayjs | null>(draft ? dayjs(draft.date) : dayjs())
  const [startTime, setStartTime] = useState<Dayjs | null>(draft ? dayjs(draft.startTime, 'HH:mm') : null)
  const [endTime, setEndTime] = useState<Dayjs | null>(draft ? dayjs(draft.endTime, 'HH:mm') : null)
  const [selectedPackageId, setSelectedPackageId] = useState<number | null>(draft?.packageId ?? null)
  // пресет в часах: null если не выбран или выбрано нестандартное время
  const [durationPreset, setDurationPreset] = useState<number | null>(null)
  const [showCustomEnd, setShowCustomEnd] = useState(false)

  useEffect(() => {
    setLoading(true)
    Promise.all([
      apiClient.get<TimePackageClientResponse[]>(`/clubs/${clubId}/time-packages`),
      apiClient.get<SeatPriceClientResponse[]>(`/clubs/${clubId}/seat-prices`),
      apiClient.get<{ name: string }>(`/clubs/${clubId}`),
    ])
      .then(([pkgRes, pricesRes, clubRes]) => {
        setPackages(pkgRes.data)
        setSeatPrices(pricesRes.data)
        setClubName(clubRes.data.name)
      })
      .catch(() => message.error('Не удалось загрузить данные клуба'))
      .finally(() => setLoading(false))
  }, [clubId]) // eslint-disable-line react-hooks/exhaustive-deps

  const durationMinutes = startTime && endTime ? endTime.diff(startTime, 'minute') : null
  const durationHours = durationMinutes !== null && durationMinutes > 0 ? durationMinutes / 60 : null
  const minPrice = seatPrices.length > 0 ? Math.min(...seatPrices.map((p) => p.pricePerHourRub)) : null
  const estimatedPrice = durationHours && minPrice ? Math.round(durationHours * minPrice) : null
  const canProceed = !!(date && startTime && endTime && durationMinutes !== null && durationMinutes > 0)

  function handleStartTimeChange(t: Dayjs | null) {
    setStartTime(t)
    // пересчитываем конец если пресет активен
    if (t && durationPreset) {
      setEndTime(t.add(durationPreset, 'hour'))
    }
  }

  function handlePresetSelect(hours: number) {
    setDurationPreset(hours)
    setSelectedPackageId(null)
    setShowCustomEnd(false)
    if (startTime) setEndTime(startTime.add(hours, 'hour'))
  }

  function handleCustomEndToggle() {
    setDurationPreset(null)
    setSelectedPackageId(null)
    setShowCustomEnd(true)
  }

  function handleEndTimeChange(t: Dayjs | null) {
    setEndTime(t)
    // ручной ввод сбрасывает пресет и пакет
    setDurationPreset(null)
    setSelectedPackageId(null)
  }

  function handlePackageClick(pkg: TimePackageClientResponse) {
    setSelectedPackageId(pkg.id)
    setDurationPreset(null)
    setShowCustomEnd(false)
    if (startTime) setEndTime(startTime.add(pkg.hours, 'hour'))
  }

  function handleProceed() {
    if (!date || !startTime || !endTime) {
      message.warning('Выберите дату и время')
      return
    }
    if (endTime.diff(startTime, 'minute') <= 0) {
      message.warning('Время окончания должно быть позже начала')
      return
    }
    const selectedPkg = packages.find((p) => p.id === selectedPackageId) ?? null
    setBookingDraft({
      clubId,
      clubName,
      date: date.format('YYYY-MM-DD'),
      startTime: startTime.format('HH:mm'),
      endTime: endTime.format('HH:mm'),
      packageId: selectedPkg?.id ?? null,
      packageHours: selectedPkg?.hours ?? null,
      selectedSeatIds: [],
    })
    navigate(`/clubs/${clubId}/booking/seats`)
  }

  if (loading) return <Spin style={{ display: 'block', margin: '48px auto' }} />

  return (
    <div style={{ maxWidth: 1040 }}>
      <StepBar steps={BOOKING_STEPS} current={0} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 24, alignItems: 'start' }}
        className="booking-layout"
      >
        {/* Левая колонка — форма */}
        <div>
          {/* Блок 1: Дата */}
          <SectionCard
            title={<span><CalendarOutlined style={{ marginRight: 8, color: tokens.colors.primary }} />Дата</span>}
            style={{ marginBottom: 16 }}
          >
            <DatePicker
              value={date}
              onChange={setDate}
              disabledDate={(d) => d.isBefore(dayjs(), 'day')}
              format="D MMMM YYYY, dddd"
              style={{ width: '100%' }}
              allowClear={false}
              size="large"
            />
          </SectionCard>

          {/* Блок 2: Начало + длительность */}
          <SectionCard
            title={<span><ClockCircleOutlined style={{ marginRight: 8, color: tokens.colors.primary }} />Время сеанса</span>}
            style={{ marginBottom: 16 }}
          >
            {/* Начало */}
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: tokens.colors.textSecondary, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Начало
              </div>
              <TimePicker
                value={startTime}
                onChange={handleStartTimeChange}
                format="HH:mm"
                minuteStep={30}
                style={{ width: '100%' }}
                allowClear={false}
                size="large"
                placeholder="Выберите время начала"
              />
            </div>

            {/* Длительность: пресеты */}
            <div>
              <div style={{ fontSize: 12, fontWeight: 600, color: tokens.colors.textSecondary, marginBottom: 10, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Длительность
              </div>
              <DurationPresets selected={durationPreset} onSelect={handlePresetSelect} />

              <button
                onClick={handleCustomEndToggle}
                style={{
                  marginTop: 8,
                  padding: '8px 14px',
                  borderRadius: tokens.radius.sm,
                  border: `2px solid ${showCustomEnd ? tokens.colors.primary : tokens.colors.border}`,
                  background: showCustomEnd ? tokens.colors.primarySoft : tokens.colors.surface,
                  color: showCustomEnd ? tokens.colors.primary : tokens.colors.textSecondary,
                  fontWeight: showCustomEnd ? 700 : 400,
                  fontSize: 13,
                  cursor: 'pointer',
                  display: 'inline-block',
                }}
              >
                Своё время
              </button>

              {/* Кастомный выбор конца */}
              {showCustomEnd && (
                <div style={{ marginTop: 12 }}>
                  <div style={{ fontSize: 12, fontWeight: 600, color: tokens.colors.textSecondary, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Окончание
                  </div>
                  <TimePicker
                    value={endTime}
                    onChange={handleEndTimeChange}
                    format="HH:mm"
                    minuteStep={30}
                    style={{ width: '100%' }}
                    allowClear={false}
                    size="large"
                    placeholder="Время окончания"
                  />
                </div>
              )}

              {/* Информационная плашка */}
              {durationHours !== null && durationHours > 0 && endTime && (
                <div style={{
                  marginTop: 14,
                  padding: '10px 14px',
                  background: tokens.colors.primarySoft,
                  borderRadius: tokens.radius.sm,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  fontSize: 13,
                  flexWrap: 'wrap',
                }}>
                  <span style={{ color: tokens.colors.primary, fontWeight: 600 }}>
                    <ClockCircleOutlined style={{ marginRight: 5 }} />
                    {durationHours % 1 === 0 ? durationHours : durationHours.toFixed(1)} ч
                    {startTime && endTime && (
                      <span style={{ fontWeight: 400, marginLeft: 6 }}>
                        · {startTime.format('HH:mm')} — {endTime.format('HH:mm')}
                      </span>
                    )}
                  </span>
                  {estimatedPrice && (
                    <span style={{ color: tokens.colors.textSecondary }}>
                      от <strong style={{ color: tokens.colors.text }}>{estimatedPrice} ₽</strong> за место
                    </span>
                  )}
                </div>
              )}
            </div>
          </SectionCard>

          {/* Блок 3: Пакеты времени */}
          {packages.length > 0 && (
            <SectionCard title="Пакеты времени">
              <Row gutter={[12, 12]}>
                {packages.map((pkg) => {
                  const selected = selectedPackageId === pkg.id
                  return (
                    <Col key={pkg.id} xs={12} sm={8}>
                      <div
                        onClick={() => handlePackageClick(pkg)}
                        style={{
                          cursor: 'pointer',
                          padding: '14px 16px',
                          borderRadius: tokens.radius.md,
                          border: `2px solid ${selected ? tokens.colors.primary : tokens.colors.border}`,
                          background: selected ? tokens.colors.primarySoft : tokens.colors.surface,
                          transition: 'all 0.15s',
                          height: '100%',
                        }}
                      >
                        <div style={{ fontWeight: 700, fontSize: 14, color: tokens.colors.text, marginBottom: 4 }}>
                          {pkg.name}
                        </div>
                        <div style={{ fontSize: 12, color: tokens.colors.textSecondary, marginBottom: 6 }}>
                          {pkg.hours} ч · {pkg.pricePerHourRub} ₽/ч
                        </div>
                        <div style={{ fontSize: 17, fontWeight: 800, color: selected ? tokens.colors.primary : tokens.colors.text }}>
                          {pkg.totalPriceRub} ₽
                        </div>
                      </div>
                    </Col>
                  )
                })}
              </Row>
              <div style={{ fontSize: 11, color: tokens.colors.textMuted, marginTop: 10 }}>
                При выборе пакета время окончания рассчитывается автоматически от времени начала
              </div>
            </SectionCard>
          )}
        </div>

        {/* Правая колонка — резюме */}
        <BookingSummary
          clubName={clubName}
          date={date}
          startTime={startTime}
          endTime={endTime}
          durationHours={durationHours}
          estimatedPrice={estimatedPrice}
          canProceed={canProceed}
          onProceed={handleProceed}
        />
      </div>

      <style>{`
        @media (max-width: 720px) {
          .booking-layout {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  )
}
