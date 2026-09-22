export interface PurchaseDraftItem{productId:string;quantity:number;unitCost:number}
export interface PurchaseDraft{supplierId:string;date:string;items:PurchaseDraftItem[]}
