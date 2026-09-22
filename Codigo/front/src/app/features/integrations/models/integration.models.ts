export interface SyncSummary{orders:number;gross:number;discounts:number;fees:number;cancellations:number;net:number;duplicates:number}
export interface ImportPreview{fileName:string;rows:number;valid:boolean;summary:SyncSummary}
