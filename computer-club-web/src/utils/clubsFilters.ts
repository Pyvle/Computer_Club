import type { ClubListItemResponse } from '../types'

export function excerpt(text: string | null | undefined, max = 90): string | null {
  if (!text) return null
  return text.length > max ? `${text.slice(0, max).trimEnd()}...` : text
}

export function filterClubs(
  clubs: ClubListItemResponse[],
  search: string,
  onlyFavorites: boolean,
  favorites: Set<number>,
): ClubListItemResponse[] {
  let result = clubs
  const query = search.trim().toLowerCase()

  if (query) {
    result = result.filter((club) =>
      club.name.toLowerCase().includes(query) || club.address.toLowerCase().includes(query),
    )
  }

  if (onlyFavorites) {
    result = result.filter((club) => favorites.has(club.id))
  }

  return result
}
