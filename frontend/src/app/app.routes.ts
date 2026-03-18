import { Routes } from '@angular/router';
import { ExcelUpload } from './excel-upload/excel-upload';
import { ManualNumber } from './manual-number/manual-number';

export const routes: Routes = [
    { path: 'upload', component: ExcelUpload },
    { path: 'manual', component: ManualNumber },
    { path: '', redirectTo: '/upload', pathMatch: 'full' },
];
