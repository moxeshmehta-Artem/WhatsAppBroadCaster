import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-manual-number',
  imports: [FormsModule],
  templateUrl: './manual-number.html',
  styleUrl: './manual-number.scss'
})
export class ManualNumber {
  http = inject(HttpClient);

  // Core Target
  phoneNumber: string = '';

  // Form Configuration Controls
  selectedProvider: string = 'infobip';
  selectedTemplate: number = 2; // Default to Bloodcamp
  variableDate: string = '';
  variableAddress: string = '';

  onSendClick() {
    // 1. Build the DTO using the dynamic user input!
    const requestPayload = {
      mobileNumber: this.phoneNumber,
      templateId: this.selectedTemplate,
      provider: this.selectedProvider,
      variables: {
        "Date": this.variableDate,
        "address": this.variableAddress
      }
    };

    // 2. Fire the Request!
    this.http.post('http://localhost:8080/api/broadcast', requestPayload).subscribe({
      next: (response) => {
        alert("✅ Success! Java Backend sent the message!");
        console.log(response);
      },
      error: (error) => {
        // Extract the physical error response string injected by our Java Backend
        const backendError = typeof error.error === 'string' ? error.error : 'Unknown Server Error';
        
        // Explicitly check if Java threw the INACTIVE provider exception safely
        if (backendError.toUpperCase().includes('INACTIVE')) {
            alert(`⛔ PROVIDER OFFLINE:\n\nThe provider '${this.selectedProvider}' is flagged as INACTIVE! Please go to the Provider Management menu to turn it back on.`);
        } else {
            alert(`❌ FAILED:\n\n${backendError}`);
        }
        
        console.error("Java API trace:", error);
      }
    });
  }
}
