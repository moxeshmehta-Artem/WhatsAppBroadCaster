import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Required for @for loops!

@Component({
  selector: 'app-template-approvals',
  imports: [CommonModule],
  templateUrl: './template-approvals.html',
  styleUrl: './template-approvals.scss'
})
export class TemplateApprovals {
  // We will replace this with a real HTTP GET call to Spring Boot later!
  templates = [
    { id: 1, name: 'Holiday Sale', status: 'APPROVED', content: 'Hi {{name}}, get 20% off today!' },
    { id: 2, name: 'Password Reset', status: 'APPROVED', content: 'Your code is {{code}}.' },
    { id: 3, name: 'Spammy Blast', status: 'REJECTED', content: 'BUY CRYPTO NOW!!' },
    { id: 4, name: 'Medical Camp', status: 'PENDING', content: 'Dr. Vora camp is at {{address}}.' }
  ];

  getStatusClass(status: string) {
    if (status === 'APPROVED') return 'badge-success';
    if (status === 'REJECTED') return 'badge-danger';
    return 'badge-warning';
  }
}
