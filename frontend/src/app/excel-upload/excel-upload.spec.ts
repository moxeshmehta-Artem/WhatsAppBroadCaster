import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExcelUpload } from './excel-upload';

describe('ExcelUpload', () => {
  let component: ExcelUpload;
  let fixture: ComponentFixture<ExcelUpload>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExcelUpload],
    }).compileComponents();

    fixture = TestBed.createComponent(ExcelUpload);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
