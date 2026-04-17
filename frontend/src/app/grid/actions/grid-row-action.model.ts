export interface GridRowAction {
  id: string;
  label: string;
  icon: string;
  userQuery: (row: any) => string;
}
