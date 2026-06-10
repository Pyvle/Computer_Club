import { describe, expect, it } from 'vitest'
import { excerpt, filterClubs } from './clubsFilters'
import type { ClubListItemResponse } from '../types'

const clubs: ClubListItemResponse[] = [
  {
    id: 1,
    name: 'Cyber Arena',
    address: 'Lenina 10',
    locationText: null,
    description: 'Large club with VIP hall and console zone',
    imageUrl: null,
    latitude: null,
    longitude: null,
    minPricePerHourRub: 120,
  },
  {
    id: 2,
    name: 'Pixel Base',
    address: 'Tverskaya 15',
    locationText: null,
    description: 'Downtown club',
    imageUrl: null,
    latitude: null,
    longitude: null,
    minPricePerHourRub: 150,
  },
]

describe('clubs filters', () => {
  it('shortens long description previews', () => {
    expect(excerpt('short text', 20)).toBe('short text')
    expect(excerpt('12345678901234567890 tail', 20)).toBe('12345678901234567890...')
    expect(excerpt(null, 20)).toBeNull()
  })

  it('filters clubs by search query in name and address', () => {
    expect(filterClubs(clubs, 'pixel', false, new Set()).map((club) => club.id)).toEqual([2])
    expect(filterClubs(clubs, 'lenina', false, new Set()).map((club) => club.id)).toEqual([1])
  })

  it('filters clubs by favorites only', () => {
    expect(filterClubs(clubs, '', true, new Set([2])).map((club) => club.id)).toEqual([2])
  })

  it('combines search and favorites filters', () => {
    expect(filterClubs(clubs, 'arena', true, new Set([1, 2])).map((club) => club.id)).toEqual([1])
    expect(filterClubs(clubs, 'arena', true, new Set([2]))).toEqual([])
  })
})
