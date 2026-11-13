export interface SearchHit {
  id: string;
  /** May contain <mark> highlight tags (HTML-encoded by the API). */
  title: string;
  snippet: string;
  category: string;
  tags: string[];
  author: string;
  createdAt: string;
  score: number;
}

export interface FacetBucket {
  value: string;
  count: number;
}

export interface SearchResult {
  total: number;
  page: number;
  size: number;
  tookMs: number;
  hits: SearchHit[];
  facets: { category: FacetBucket[]; tags: FacetBucket[] };
}

export interface SearchQuery {
  q: string;
  categories: string[];
  tags: string[];
  from: string | null; // yyyy-MM-dd
  to: string | null;
  page: number;
  size: number;
}

export interface EnterpriseDocument {
  id: string;
  title: string;
  content: string;
  category: string;
  tags: string[];
  author: string;
  createdAt: string;
  updatedAt: string;
}
