import { Routes } from '@angular/router';
import { ExcelUpload } from './excel-upload/excel-upload';
import { ManualNumber } from './manual-number/manual-number';
import { TemplateApprovals } from './template-approvals/template-approvals';
import { ProviderManagement } from './provider-management/provider-management';
import { MessageLogs } from './message-logs/message-logs';

export const routes: Routes = [
    { path: 'upload', component: ExcelUpload },
    { path: 'manual', component: ManualNumber },
    { path: 'templates', component: TemplateApprovals },
    { path: 'providers', component: ProviderManagement },
    { path: 'logs', component: MessageLogs },
    { path: '', redirectTo: '/upload', pathMatch: 'full' },
];
