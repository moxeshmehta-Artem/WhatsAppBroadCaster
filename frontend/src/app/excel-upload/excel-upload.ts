import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms'; // 1. Added for two-way binding!
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-excel-upload',
  imports: [FormsModule], // 2. Add it here too!
  templateUrl: './excel-upload.html',
  styleUrl: './excel-upload.scss'
})
export class ExcelUpload {
  http = inject(HttpClient);

  selectedFile: File | null = null;
  selectedFileName: string = '';

  // New Form Controls!
  selectedProvider: string = 'infobip';
  selectedTemplate: number = 2; // Assuming Bloodcamp is ID 2
  variableDate: string = '';
  variableAddress: string = '';

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
    }
  }

  onUploadClick() {
    if (!this.selectedFile) return;

    // We use FormData because we are sending an actual physical File over HTTP, not just JSON!
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('provider', this.selectedProvider);
    formData.append('templateId', this.selectedTemplate.toString());
    formData.append('varDate', this.variableDate);
    formData.append('varAddress', this.variableAddress);

    // This alert proves all the data was captured perfectly!
    alert(`Sending Campaign to Java!\nFile: ${this.selectedFileName}\nProvider: ${this.selectedProvider}\nTemplate: ${this.selectedTemplate}\nDate: ${this.variableDate}\nAddress: ${this.variableAddress}`);

    this.http.post('http://localhost:8080/api/broadcast/bulk', formData).subscribe({
      next: (res: any) => {
          alert(`Blast Complete!\n\nSuccessfully Delivered: ${res.totalSuccessful} contacts\nFailed to Deliver: ${res.totalFailed} contacts`);
      },
      error: (err) => {
          // Extract the exact error message from the Java backend (e.g. INACTIVE provider!)
          const backendErrorMessage = typeof err.error === 'string' ? err.error : 'Unknown Server Error';
          alert(`SYSTEM REJECTED:\n\n${backendErrorMessage}`);
          console.error(err);
      }
    });
  }

  // --- NEW: Preview Logic ---
  previewContacts: string[] = [];

  onPreviewClick() {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    // Call our new Java /preview endpoint!
    this.http.post<string[]>('http://localhost:8080/api/broadcast/preview', formData).subscribe({
      next: (contacts) => {
        this.previewContacts = contacts;
      },
      error: (err) => {
        alert("Failed to fetch contacts! Is the Java backend running?");
        console.error(err);
      }
    });
  }
}

