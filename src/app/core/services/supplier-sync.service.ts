import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SupplierSyncRequest, SupplierSyncResult } from '../models/minimarket.models';
import { API_BASE_URL } from './api-base';

@Injectable({ providedIn: 'root' })
export class SupplierSyncService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/supplier-sync`;

  sync(payload: SupplierSyncRequest): Observable<SupplierSyncResult> {
    return this.http.post<SupplierSyncResult>(this.apiUrl, payload);
  }
}
