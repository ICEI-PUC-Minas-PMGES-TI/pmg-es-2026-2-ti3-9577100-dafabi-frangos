export interface PurchaseDraftItem{productId:string;quantity:number;unitCost:number;batch?:string;expiry?:string}
export interface PurchaseDraft{supplierId:string;date:string;items:PurchaseDraftItem[]}
