import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common'; // Required for @for loops!
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-template-approvals',
  imports: [CommonModule],
  templateUrl: './template-approvals.html',
  styleUrl: './template-approvals.scss'
})
export class TemplateApprovals implements OnInit {
  http = inject(HttpClient);
  
  // Real dynamic list from Java Backend
  templates = signal<any[]>([]);

  ngOnInit() {
    this.fetchTemplates();
  }

  fetchTemplates() {
    this.http.get<any[]>('http://localhost:8080/api/templates').subscribe({
      next: (data) => {
        this.templates.set(data);
      },
      error: (err) => console.error("Could not fetch real templates from DB", err)
    });
  }

  //i want to print status in written form not in code form


  getStatusText(status: string) {
    if (status === 'APPROVED') return 'Approved';
    if (status === 'REJECTED') return 'Rejected';
    return 'Pending';
  }
}
