import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface Provider {
  providerID: number;
  providerName: string;
  status: string;
}

@Component({
  selector: 'app-provider-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './provider-management.html',
  styleUrl: './provider-management.scss'
})
export class ProviderManagement implements OnInit {
  http = inject(HttpClient);

  // Modal & Form State
  isPopupOpen = false;
  newProvider = {
    providerName: '',
    apiUrl: '',
    apiKey: '',
    payloadTemplate: '{"to":"{{phone}}", "body":"{{message}}"}'
  };

  // Use Angular Signals to trigger UI updates without ZoneJS!
  providers = signal<Provider[]>([]);

  ngOnInit() {
    this.fetchProviders();
  }

  fetchProviders() {
    this.http.get<Provider[]>('http://localhost:8080/api/providers').subscribe({
      next: (data) => {
        this.providers.set(data);
      },
      error: (err) => console.error("Failed to load providers", err)
    });
  }

  toggleStatus(provider: Provider) {
    if (provider.status === 'ACTIVE') {
      const activeCount = this.providers().filter(p => p.status === 'ACTIVE').length;
      if (activeCount <= 1) {
        alert("SYSTEM ERROR:\n\nYou cannot deactivate this provider. Atleast ONE provider must remain ACTIVE.");
        return;
      }
    }

    const newStatus = provider.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    this.http.put(`http://localhost:8080/api/providers/${provider.providerID}/status`, { status: newStatus }).subscribe({
      next: () => {
        this.providers.update(currentList =>
          currentList.map(p => p.providerID === provider.providerID ? { ...p, status: newStatus } : p)
        );
      },
      error: (err) => {
        alert("Failed to update status!");
        console.error(err);
      }
    });
  }

  openDialog() {
    this.isPopupOpen = true;
    this.newProvider = {
      providerName: '',
      apiUrl: '',
      apiKey: '',
      payloadTemplate: '{"to":"{{phone}}", "body":"{{message}}"}'
    };
  }

  closeDialog() {
    this.isPopupOpen = false;
  }

  saveNewProvider() {
    if (!this.newProvider.providerName.trim() || !this.newProvider.apiUrl.trim()) {
      alert("Please enter both Provider Name and API URL.");
      return;
    }

    // Capitalize & Sanitize
    this.newProvider.providerName = this.newProvider.providerName.toUpperCase().replace(/\s+/g, '_');

    this.http.post('http://localhost:8080/api/providers', this.newProvider).subscribe({
      next: () => {
        this.closeDialog();
        this.fetchProviders();
      },
      error: (err) => {
        console.error("Failed to save provider", err);
        alert(err.error || "Failed to create provider.");
      }
    });
  }
}
