import { useState, useMemo, useEffect, useRef } from 'react'
import { Modal, Select, Button, Tag, Empty, Tooltip } from 'antd'
import { DeleteOutlined, ZoomInOutlined, ZoomOutOutlined, CompressOutlined } from '@ant-design/icons'
import type { AdminSeatResponse, FloorplanResponse } from '../../types'

interface FloorplanItem {
  type: 'SEAT'
  seatId: number
  col: number
  row: number
}

export interface FloorplanData {
  items: FloorplanItem[]
}

// конвертирует любой сохранённый формат (col/row, абсолютные пиксели, старые дроби) в col/row
function parseItems(raw: unknown, width: number, height: number, gridSize: number): FloorplanItem[] {
  if (!raw || typeof raw !== 'object') return []
  const d = raw as { items?: unknown[] }
  if (!Array.isArray(d.items)) return []
  const result: FloorplanItem[] = []
  for (const it of d.items) {
    if (!it || typeof it !== 'object') continue
    const item = it as Record<string, unknown>
    if (item.type !== 'SEAT') continue
    const seatId = typeof item.seatId === 'number' ? item.seatId : null
    if (seatId === null) continue
    // новый формат: col/row напрямую
    if (typeof item.col === 'number' && typeof item.row === 'number') {
      result.push({ type: 'SEAT', seatId, col: item.col, row: item.row })
      continue
    }
    // старый формат: x/y/w/h в пикселях (w >= 1) или дробях (w < 1)
    const x = typeof item.x === 'number' ? item.x : 0
    const y = typeof item.y === 'number' ? item.y : 0
    const w = typeof item.w === 'number' ? item.w : 0
    const col = w >= 1
      ? Math.round(x / gridSize)
      : Math.round((x * width) / gridSize)
    const row = w >= 1
      ? Math.round(y / gridSize)
      : Math.round((y * height) / gridSize)
    result.push({ type: 'SEAT', seatId, col, row })
  }
  return result
}

function cellKey(col: number, row: number) {
  return `${col},${row}`
}

// минимальный отображаемый размер ячейки в пикселях
const MIN_CELL_PX = 36

interface Props {
  floorplan: FloorplanResponse
  seats: AdminSeatResponse[]
  readOnly: boolean
  onChange: (data: FloorplanData) => void
}

export default function FloorplanEditor({ floorplan, seats, readOnly, onChange }: Props) {
  const { width, height, gridSize } = floorplan
  const cols = Math.max(1, Math.floor(width / gridSize))
  const rows = Math.max(1, Math.floor(height / gridSize))

  // zoom: множитель отображения. По умолчанию — минимальный, чтобы ячейки были >= MIN_CELL_PX
  const defaultZoom = Math.max(1, Math.ceil(MIN_CELL_PX / gridSize))
  const [zoom, setZoom] = useState(defaultZoom)
  const cellPx = gridSize * zoom

  const [items, setItems] = useState<FloorplanItem[]>(() => parseItems(floorplan.data, width, height, gridSize))
  const filteredRef = useRef(false)
  const [activeCell, setActiveCell] = useState<{ col: number; row: number } | null>(null)
  const [pickSeatId, setPickSeatId] = useState<number | null>(null)

  // "col,row" → seatId
  const cellMap = useMemo(() => {
    const map = new Map<string, number>()
    for (const item of items) {
      map.set(cellKey(item.col, item.row), item.seatId)
    }
    return map
  }, [items])

  const seatById = useMemo(() => {
    const map = new Map<number, AdminSeatResponse>()
    for (const s of seats) map.set(s.id, s)
    return map
  }, [seats])

  // когда список мест загружен — убираем с карты любые seatId, не принадлежащие клубу
  useEffect(() => {
    if (seats.length === 0 || filteredRef.current) return
    filteredRef.current = true
    const validIds = new Set(seats.map((s) => s.id))
    const cleaned = items.filter((item) => validIds.has(item.seatId))
    if (cleaned.length !== items.length) {
      setItems(cleaned)
      onChange({ items: cleaned })
    }
  }, [seats])

  const placedSeatIds = useMemo(() => new Set(items.map((i) => i.seatId)), [items])

  function commit(next: FloorplanItem[]) {
    setItems(next)
    onChange({ items: next })
  }

  function handleCellClick(col: number, row: number) {
    if (readOnly) return
    setActiveCell({ col, row })
    setPickSeatId(null)
  }

  function closeModal() {
    setActiveCell(null)
    setPickSeatId(null)
  }

  function placeSeat() {
    if (!activeCell || pickSeatId === null) return
    const { col, row } = activeCell
    const filtered = items.filter((item) => !(item.col === col && item.row === row))
    filtered.push({ type: 'SEAT', seatId: pickSeatId, col, row })
    commit(filtered)
    closeModal()
  }

  function removeSeat(col: number, row: number) {
    commit(items.filter((item) => !(item.col === col && item.row === row)))
    closeModal()
  }

  const activeSeatId =
    activeCell !== null ? (cellMap.get(cellKey(activeCell.col, activeCell.row)) ?? null) : null
  const activeSeat = activeSeatId !== null ? (seatById.get(activeSeatId) ?? null) : null

  const availableSeats = seats.filter(
    (s) => s.isActive && (!placedSeatIds.has(s.id) || s.id === activeSeatId),
  )

  const isPlaceModal = activeCell !== null && activeSeat === null
  const isInfoModal = activeCell !== null && activeSeat !== null

  // сколько пикселей нужно для отображения текста
  const showLabel = cellPx >= 28
  const showVipBadge = cellPx >= 44
  const fontSize = Math.min(13, Math.max(9, cellPx * 0.28))

  return (
    <div>
      {/* Toolbar */}
      <div
        style={{
          marginBottom: 10,
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        {/* Legend */}
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', fontSize: 12, color: '#555' }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <span
              style={{
                width: 18,
                height: 18,
                background: '#1677ff',
                borderRadius: 4,
                display: 'inline-block',
              }}
            />
            Обычное
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <span
              style={{
                width: 18,
                height: 18,
                background: '#faad14',
                borderRadius: 4,
                display: 'inline-block',
              }}
            />
            VIP
          </span>
          <span style={{ color: '#aaa' }}>
            {cols}×{rows} кл. · {gridSize}px
          </span>
        </div>

        {/* Zoom controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: 'auto' }}>
          <Tooltip title="Уменьшить">
            <Button
              size="small"
              icon={<ZoomOutOutlined />}
              onClick={() => setZoom((z) => Math.max(1, z - 1))}
              disabled={zoom <= 1}
            />
          </Tooltip>
          <span
            style={{
              minWidth: 48,
              textAlign: 'center',
              fontSize: 12,
              color: '#555',
              userSelect: 'none',
            }}
          >
            {zoom === 1 ? '1:1' : `×${zoom}`}
          </span>
          <Tooltip title="Увеличить">
            <Button
              size="small"
              icon={<ZoomInOutlined />}
              onClick={() => setZoom((z) => Math.min(8, z + 1))}
              disabled={zoom >= 8}
            />
          </Tooltip>
          {zoom !== defaultZoom && (
            <Tooltip title="По умолчанию">
              <Button
                size="small"
                icon={<CompressOutlined />}
                onClick={() => setZoom(defaultZoom)}
              />
            </Tooltip>
          )}
        </div>
      </div>

      {/* Hint */}
      {!readOnly && (
        <div style={{ marginBottom: 8, fontSize: 12, color: '#aaa' }}>
          Кликните по пустой ячейке, чтобы разместить место. По занятой — убрать.
        </div>
      )}

      {/* Scrollable grid */}
      <div
        style={{
          overflowX: 'auto',
          overflowY: 'auto',
          maxHeight: 'calc(100vh - 390px)',
          border: '1px solid #d9d9d9',
          borderRadius: 6,
          background: '#f0f2f5',
          padding: 8,
        }}
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: `repeat(${cols}, ${cellPx}px)`,
            gap: 2,
            width: cols * cellPx + (cols - 1) * 2,
          }}
        >
          {Array.from({ length: rows * cols }, (_, i) => {
            const row = Math.floor(i / cols)
            const col = i % cols
            const key = cellKey(col, row)
            const seatId = cellMap.get(key) ?? null
            const seat = seatId !== null ? seatById.get(seatId) : undefined
            const occupied = seatId !== null
            const isVip = seat?.type === 'VIP'
            const isActiveCell = activeCell?.col === col && activeCell?.row === row

            const bgColor = occupied ? (isVip ? '#faad14' : '#1677ff') : '#ffffff'
            const textColor = occupied ? '#ffffff' : '#c8c8c8'
            const borderColor = isActiveCell
              ? '#ff4d4f'
              : occupied
                ? isVip
                  ? '#d48806'
                  : '#0958d9'
                : '#d9d9d9'
            const borderWidth = isActiveCell ? 2 : 1

            return (
              <div
                key={key}
                onClick={() => handleCellClick(col, row)}
                title={
                  occupied
                    ? seat
                      ? `${seat.label}${seat.type === 'VIP' ? ' · VIP' : ''}`
                      : `#${seatId}`
                    : readOnly
                      ? ''
                      : `Разместить место (${col}, ${row})`
                }
                style={{
                  width: cellPx,
                  height: cellPx,
                  boxSizing: 'border-box',
                  borderRadius: 5,
                  border: `${borderWidth}px solid ${borderColor}`,
                  background: bgColor,
                  cursor: readOnly ? 'default' : occupied ? 'pointer' : 'cell',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 1,
                  userSelect: 'none',
                  overflow: 'hidden',
                  transition: 'opacity 0.1s',
                  opacity: occupied ? 1 : 0.7,
                  boxShadow: occupied ? '0 1px 3px rgba(0,0,0,0.15)' : 'none',
                }}
              >
                {occupied ? (
                  <>
                    {showLabel && (
                      <span
                        style={{
                          fontSize,
                          fontWeight: 700,
                          color: textColor,
                          lineHeight: 1,
                          textAlign: 'center',
                          maxWidth: '100%',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          padding: '0 2px',
                        }}
                      >
                        {seat ? seat.label : `#${seatId}`}
                      </span>
                    )}
                    {showVipBadge && isVip && (
                      <span
                        style={{
                          fontSize: Math.max(8, fontSize - 2),
                          color: 'rgba(255,255,255,0.9)',
                          lineHeight: 1,
                          fontWeight: 600,
                          letterSpacing: 0.5,
                        }}
                      >
                        VIP
                      </span>
                    )}
                  </>
                ) : (
                  !readOnly && (
                    <span style={{ fontSize: cellPx * 0.35, color: '#bbb', lineHeight: 1 }}>
                      +
                    </span>
                  )
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Stats */}
      <div
        style={{
          marginTop: 8,
          fontSize: 12,
          color: '#888',
          display: 'flex',
          gap: 16,
        }}
      >
        <span>
          Размещено:{' '}
          <strong style={{ color: '#333' }}>
            {items.length}
          </strong>{' '}
          / {seats.filter((s) => s.isActive).length} мест
        </span>
        <span>
          Свободных ячеек:{' '}
          <strong style={{ color: '#333' }}>{cols * rows - items.length}</strong>
        </span>
      </div>

      {/* Modal: place seat */}
      <Modal
        open={isPlaceModal}
        title={
          activeCell
            ? `Ячейка (${activeCell.col + 1}, ${activeCell.row + 1})`
            : ''
        }
        onCancel={closeModal}
        onOk={placeSeat}
        okText="Разместить"
        cancelText="Отмена"
        okButtonProps={{ disabled: pickSeatId === null }}
        destroyOnClose
      >
        {availableSeats.length === 0 ? (
          <Empty description="Все активные места уже размещены на схеме" />
        ) : (
          <>
            <p style={{ color: '#666', fontSize: 13, marginBottom: 8 }}>
              Выберите место:
            </p>
            <Select
              style={{ width: '100%' }}
              placeholder="Место..."
              value={pickSeatId}
              onChange={setPickSeatId}
              showSearch
              autoFocus
              filterOption={(input, option) =>
                (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
              }
              options={availableSeats
                .slice()
                .sort((a, b) => a.sortOrder - b.sortOrder)
                .map((s) => ({
                  value: s.id,
                  label: `${s.label} — ${s.type === 'VIP' ? 'VIP' : 'Обычное'}`,
                }))}
            />
          </>
        )}
      </Modal>

      {/* Modal: seat info + remove */}
      <Modal
        open={isInfoModal}
        title={
          activeSeat ? (
            <span>
              Место:{' '}
              <strong>{activeSeat.label}</strong>
              <Tag
                color={activeSeat.type === 'VIP' ? 'gold' : 'blue'}
                style={{ marginLeft: 8 }}
              >
                {activeSeat.type === 'VIP' ? 'VIP' : 'Обычное'}
              </Tag>
            </span>
          ) : (
            ''
          )
        }
        onCancel={closeModal}
        footer={
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <Button onClick={closeModal}>Закрыть</Button>
            {!readOnly && (
              <Button
                danger
                icon={<DeleteOutlined />}
                onClick={() => activeCell && removeSeat(activeCell.col, activeCell.row)}
              >
                Убрать с карты
              </Button>
            )}
          </div>
        }
        destroyOnClose
      >
        {activeSeat && (
          <div style={{ color: '#555', fontSize: 13 }}>
            Ячейка: ({activeCell?.col !== undefined ? activeCell.col + 1 : '—'},{' '}
            {activeCell?.row !== undefined ? activeCell.row + 1 : '—'})
          </div>
        )}
      </Modal>
    </div>
  )
}
