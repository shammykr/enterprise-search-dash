import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EnterpriseDocument, SearchQuery, SearchResult } from './models';

@Injectable({ providedIn: 'root' })
export class SearchApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  search(query: SearchQuery): Observable<SearchResult> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.q) params = params.set('q', query.q);
    query.categories.forEach((c) => (params = params.append('category', c)));
    query.tags.forEach((t) => (params = params.append('tag', t)));
    if (query.from) params = params.set('from', query.from);
    if (query.to) params = params.set('to', query.to);
    return this.http.get<SearchResult>(`${this.base}/search`, { params });
  }

  autocomplete(prefix: string, limit = 8): Observable<string[]> {
    const params = new HttpParams().set('q', prefix).set('limit', limit);
    return this.http.get<string[]>(`${this.base}/autocomplete`, { params });
  }

  getDocument(id: string): Observable<EnterpriseDocument> {
    return this.http.get<EnterpriseDocument>(`${this.base}/documents/${encodeURIComponent(id)}`);
  }
}
