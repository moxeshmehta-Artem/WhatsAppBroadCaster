import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateApprovals } from './template-approvals';

describe('TemplateApprovals', () => {
  let component: TemplateApprovals;
  let fixture: ComponentFixture<TemplateApprovals>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateApprovals],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateApprovals);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
