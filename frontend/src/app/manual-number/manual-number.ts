import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-manual-number',
  imports: [FormsModule],
  templateUrl: './manual-number.html',
  styleUrl: './manual-number.scss'
})
export class ManualNumber implements OnInit {
  http = inject(HttpClient);

  // List of active providers and templates from Database!
  activeProviders = signal<any[]>([]);
  allTemplates = signal<any[]>([]);

  ngOnInit() {
    this.fetchActiveProviders();
    this.fetchTemplates();
  }

  fetchActiveProviders() {
    this.http.get<any[]>('http://localhost:8080/api/providers').subscribe({
      next: (data) => {
        const onlyActive = data.filter(p => p.status === 'ACTIVE');
        this.activeProviders.set(onlyActive);
        if (onlyActive.length > 0) {
          this.selectedProvider = onlyActive[0].providerName.toLowerCase();
        }
      },
      error: (err) => console.error("Could not load providers", err)
    });
  }

  fetchTemplates() {
    this.http.get<any[]>('http://localhost:8080/api/templates').subscribe({
      next: (data) => {
        this.allTemplates.set(data);
        if (data.length > 0) {
          this.selectedTemplateId = data[0].id;
          this.onTemplateChange();
        }
      },
      error: (err) => console.error("Could not load templates", err)
    });
  }

  // Core Target
  phoneNumber: string = '';

  // Form Configuration Controls
  selectedProvider: string = 'infobip';
  selectedTemplateId: number = 0;
  
  // DYNAMIC VARIABLES SYSTEM
  templateVariables: { key: string, value: string }[] = [];

  onTemplateChange() {
    const template = this.allTemplates().find(t => t.id == this.selectedTemplateId);
    if (template) {
      // Logic to find all {{variableName}} patterns in the content
      const regex = /\{\{(.*?)\}\}/g;
      const matches = [...template.content.matchAll(regex)];
      
      // Extract unique variable names
      const varNames = [...new Set(matches.map(m => m[1]))];
      
      // Update the dynamic input list
      this.templateVariables = varNames.map(name => ({ key: name, value: '' }));
    }
  }

  isFormInvalid() {
    return !this.phoneNumber || this.templateVariables.some(v => !v.value.trim());
  }

  onSendClick() {
    // 1. Build variables map from the dynamic list
    const varsMap: any = {};
    this.templateVariables.forEach(v => varsMap[v.key] = v.value);

    // 2. Build the DTO
    const requestPayload = {
      mobileNumber: this.phoneNumber,
      templateId: this.selectedTemplateId,
      provider: this.selectedProvider,
      variables: varsMap
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
            alert(` PROVIDER OFFLINE:\n\nThe provider '${this.selectedProvider}' is flagged as INACTIVE! Please go to the Provider Management menu to turn it back on.`);
        } else {
            alert(` FAILED:\n\n${backendError}`);
        }
        
        console.error("Java API trace:", error);
      }
    });
  }
}
