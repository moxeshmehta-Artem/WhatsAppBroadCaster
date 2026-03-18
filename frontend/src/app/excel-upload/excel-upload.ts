import { Component } from '@angular/core';

@Component({
  selector: 'app-excel-upload',
  imports: [],
  templateUrl: './excel-upload.html',
  styleUrl: './excel-upload.scss'
})
export class ExcelUpload {
  selectedFile: File | null = null;
  selectedFileName: string = '';

  // 1. Angular catches the file whenever the user picks one
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
    }
  }

  // 2. Angular fires this when they click the giant Launch button
  onUploadClick() {
    if (this.selectedFile) {
      alert('File ready for backend: ' + this.selectedFileName);
      // Later, we will use HttpClient here to POST this to your Spring Boot API!
    }
  }
}
