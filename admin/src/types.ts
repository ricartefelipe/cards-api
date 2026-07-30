export type AccountSummary = {
  id: string
  status: string
  customer_id: string
  customer_name: string
  document: string
  created_at: string
  cancelled_at: string | null
}

export type CustomerSummary = {
  id: string
  full_name: string
  document: string
  email: string
  phone: string | null
  created_at: string
}

export type CustomerDetail = CustomerSummary & {
  address: {
    street: string
    number: string
    city: string
    state: string
    zip_code: string
    country: string
  }
  account_id: string | null
}

export type AccountDetail = {
  id: string
  status: string
  created_at: string
  cancelled_at: string | null
  customer: CustomerSummary
  physical_card_ids: string[]
  virtual_card_ids: string[]
}

export type PhysicalCard = {
  id: string
  account_id: string
  status: string
  tracking_id: string
  delivery_status: string
  delivery_date: string | null
  delivery_return_reason: string | null
  delivery_address: string | null
  delivered_at: string | null
  validated_at: string | null
  reissue_reason: string | null
  previous_physical_card_id: string | null
  created_at: string
  deactivated_at: string | null
}

export type VirtualCard = {
  id: string
  account_id: string
  status: string
  processor_account_id: string
  processor_card_id: string
  cvv_expiration_at: string | null
  reissue_reason: string | null
  previous_virtual_card_id: string | null
  created_at: string
  deactivated_at: string | null
}

export type CreateAccountResponse = {
  customer_id: string
  account_id: string
  physical_card_id: string
  tracking_id: string
}
