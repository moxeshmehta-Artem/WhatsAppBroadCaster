import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface Provider {
  providerID: number;
  providerName: string;
  status: string;
}

@Component({
  selector: 'app-provider-management',
  imports: [CommonModule],
  templateUrl: './provider-management.html',
  styleUrl: './provider-management.scss'
})
export class ProviderManagement implements OnInit {
  http = inject(HttpClient);
  
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
    // SECURITY GUARD: Ensure atleast one provider stays active!
    if (provider.status === 'ACTIVE') {
      const activeCount = this.providers().filter(p => p.status === 'ACTIVE').length;
      if (activeCount <= 1) {
        alert("SYSTEM ERROR:\n\nYou cannot deactivate this provider. Atleast ONE provider must remain ACTIVE to ensure messages can be sent.");
        return;
      }
    }

    const newStatus = provider.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    
    this.http.put(`http://localhost:8080/api/providers/${provider.providerID}/status`, { status: newStatus }).subscribe({
      next: () => {
        // Update the signal intelligently
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
}
