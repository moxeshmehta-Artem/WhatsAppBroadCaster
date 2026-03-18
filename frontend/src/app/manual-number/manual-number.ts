import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms'; // 1. REQUIRED for Two-Way Data Binding!

@Component({
  selector: 'app-manual-number',
  imports: [FormsModule], // 2. Add FormsModule here!
  templateUrl: './manual-number.html',
  styleUrl: './manual-number.scss'
})
export class ManualNumber {
  // These variables automatically update when the user types!
  phoneNumber: string = '';
  messageBody: string = '';

  onSendClick() {
    alert(`Sending to ${this.phoneNumber}:\n"${this.messageBody}"`);
    // Later: POST request to Spring Boot /api/broadcast
  }
}
