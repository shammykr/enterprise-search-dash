import { ParamMap, Params } from '@angular/router';
import { SearchQuery } from '../../core/models';

export const PAGE_SIZE = 10;

/** The search state lives in the URL so results are linkable and back/forward works. */
export function parseQuery(params: ParamMap): SearchQuery {
  return {
    q: params.get('q') ?? '',
    categories: params.getAll('category'),
    tags: params.getAll('tag'),
    from: params.get('from'),
    to: params.get('to'),
    page: Math.max(Number(params.get('page') ?? 0) || 0, 0),
    size: PAGE_SIZE,
  };
}

export function toParams(query: SearchQuery): Params {
  return {
    q: query.q || null,
    category: query.categories.length ? query.categories : null,
    tag: query.tags.length ? query.tags : null,
    from: query.from,
    to: query.to,
    page: query.page > 0 ? query.page : null,
  };
}

/** Local yyyy-MM-dd (avoids the UTC shift of toISOString). */
export function toIsoDate(date: Date | null): string | null {
  if (!date) return null;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function fromIsoDate(value: string | null): Date | null {
  if (!value) return null;
  const [y, m, d] = value.split('-').map(Number);
  return new Date(y, m - 1, d);
}
