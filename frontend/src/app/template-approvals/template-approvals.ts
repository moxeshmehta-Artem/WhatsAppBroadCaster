import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-template-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './template-approvals.html',
  styleUrl: './template-approvals.scss'
})
export class TemplateApprovals implements OnInit {
  isPopupOpen = false;
  http = inject(HttpClient);
  
  // New template state
  newTemplate = {
    name: '',
    content: ''
  };

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

  getStatusText(status: string) {
    if (status === 'APPROVED') return 'Approved';
    if (status === 'REJECTED') return 'Rejected';
    return 'Pending';
  }

  openDialog() {
    this.isPopupOpen = true;
    this.newTemplate = { name: '', content: '' }; // Reset form
  }

  closeDialog() {
    this.isPopupOpen = false;
  }

  /**
   * Quick add placeholders like {{name}} to the content
   */
  insertPlaceholder(tag: string) {
    this.newTemplate.content += `{{${tag}}}`;
  }

  /**
   * Save the new template to database as PENDING status
   */
  saveNewTemplate() {
    if (!this.newTemplate.name || !this.newTemplate.content) {
      alert("Please enter both Template Name and Content!");
      return;
    }

    const payload = {
      name: this.newTemplate.name,
      content: this.newTemplate.content,
      status: 'PENDING'
    };

    this.http.post('http://localhost:8080/api/templates', payload).subscribe({
      next: () => {
        this.closeDialog();
        this.fetchTemplates(); // Refresh the list
      },
      error: (err) => {
        console.error("Failed to save template", err);
        alert("Error saving template. Please check logs.");
      }
    });
  }

  /**
   * Delete template from DB
   */
  deleteTemplate(id: number) {
    if (confirm("Are you sure you want to delete this template?")) {
      this.http.delete(`http://localhost:8080/api/templates/${id}`).subscribe({
        next: () => {
          this.fetchTemplates(); // Refresh list
        },
        error: (err) => {
          console.error("Failed to delete template", err);
          alert("Error deleting template.");
        }
      });
    }
  }
}
