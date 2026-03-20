import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-message-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-logs.html',
  styleUrl: './message-logs.scss'
})
export class MessageLogs implements OnInit {
  private http = inject(HttpClient);

  // Toggle state
  isWhatsApp = signal(true);

  // Pagination state
  logs = signal<any[]>([]);
  currentPage = signal(0);
  totalPages = signal(0);
  totalItems = signal(0);
  pageSize = signal(15);
  loading = signal(false);

  ngOnInit() {
    this.refreshLogs();
  }

  toggleView(type: 'whatsapp' | 'sms') {
    this.isWhatsApp.set(type === 'whatsapp');
    this.currentPage.set(0);
    this.refreshLogs();
  }

  refreshLogs() {
    this.loading.set(true);
    const endpoint = this.isWhatsApp() ? 'whatsapp' : 'sms';
    const url = `http://localhost:8080/api/logs/${endpoint}?page=${this.currentPage()}&size=${this.pageSize()}`;

    this.http.get<any>(url).subscribe({
      next: (data) => {
        this.logs.set(data.logs || []);
        this.currentPage.set(data.currentPage);
        this.totalPages.set(data.totalPages);
        this.totalItems.set(data.totalItems);
        this.loading.set(false);
      },
      error: (err) => {
        console.error("Failed to load logs", err);
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.refreshLogs();
    }
  }

  changePageSize(size: number) {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.refreshLogs();
  }

  getStatusClass(status: string) {
    if (status === 'SENT') return 'badge-success';
    return 'badge-danger';
  }

  // Generate visible page numbers for pagination bar
  getPageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: number[] = [];

    let start = Math.max(0, current - 2);
    let end = Math.min(total - 1, current + 2);

    // Always show at least 5 pages if available
    if (end - start < 4) {
      if (start === 0) end = Math.min(total - 1, 4);
      else start = Math.max(0, end - 4);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }
}
