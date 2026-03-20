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
  
  // Data lists
  whatsappLogs = signal<any[]>([]);
  smsLogs = signal<any[]>([]);

  ngOnInit() {
    this.refreshLogs();
  }

  toggleView(type: 'whatsapp' | 'sms') {
    this.isWhatsApp.set(type === 'whatsapp');
    this.refreshLogs();
  }

  refreshLogs() {
    if (this.isWhatsApp()) {
      this.http.get<any[]>('http://localhost:8080/api/logs/whatsapp').subscribe({
        next: (data) => this.whatsappLogs.set(data),
        error: (err) => console.error("Failed to load WhatsApp logs", err)
      });
    } else {
      // Placeholder for SMS
      this.smsLogs.set([]);
    }
  }

  getStatusClass(status: string) {
    if (status === 'SENT') return 'badge-success';
    if (status === 'FAILED' || status === 'ERROR') return 'badge-danger';
    return 'badge-warning';
  }
}
