import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ReceiptConfirmRequest,
  ReceiptConfirmResult,
  ReceiptScanRequest,
  ReceiptScanResult
} from '../models/minimarket.models';
import { API_BASE_URL } from './api-base';

@Injectable({ providedIn: 'root' })
export class ReceiptsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/receipts`;

  scan(payload: ReceiptScanRequest): Observable<ReceiptScanResult> {
    return this.http.post<ReceiptScanResult>(`${this.apiUrl}/scan`, payload);
  }

  confirm(payload: ReceiptConfirmRequest): Observable<ReceiptConfirmResult> {
    return this.http.post<ReceiptConfirmResult>(`${this.apiUrl}/confirm`, payload);
  }
}
