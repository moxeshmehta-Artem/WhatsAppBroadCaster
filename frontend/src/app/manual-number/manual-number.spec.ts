import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManualNumber } from './manual-number';

describe('ManualNumber', () => {
  let component: ManualNumber;
  let fixture: ComponentFixture<ManualNumber>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManualNumber],
    }).compileComponents();

    fixture = TestBed.createComponent(ManualNumber);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
