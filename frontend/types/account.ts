export interface Account {
  id: string
  userId: string
  name: string
  institutionName: string | null
  accountType: 'BANK'
  lastFourDigits: string | null
  currency: string
  isActive: boolean
  createdAt: string
}
