import type {
  AccountDetail,
  AccountSummary,
  CreateAccountResponse,
  CustomerDetail,
  CustomerSummary,
  PhysicalCard,
  VirtualCard,
} from './types'

const TOKEN_KEY = 'cards-desk-token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null): void {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY)
    return
  }
  localStorage.setItem(TOKEN_KEY, token)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const res = await fetch(`/api${path}`, { ...init, headers })
  if (res.status === 401) {
    setToken(null)
    throw new Error('Sessão expirada. Entre novamente.')
  }
  if (!res.ok) {
    let detail = res.statusText
    try {
      const body = await res.json()
      detail = body.message || body.title || JSON.stringify(body)
    } catch {
      /* ignore */
    }
    throw new Error(detail || `Erro HTTP ${res.status}`)
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export async function loginWithPassword(username: string, password: string): Promise<string> {
  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: 'cards-admin',
    username,
    password,
  })
  const res = await fetch('/auth/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) {
    throw new Error('Credenciais inválidas ou Keycloak indisponível.')
  }
  const data = (await res.json()) as { access_token: string }
  return data.access_token
}

export const api = {
  listAccounts: () => request<AccountSummary[]>('/accounts'),
  getAccount: (id: string) => request<AccountDetail>(`/accounts/${id}`),
  createAccount: (payload: unknown) =>
    request<CreateAccountResponse>('/accounts', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  cancelAccount: (id: string) =>
    request<{ account_id: string; cancelled_at: string }>(`/accounts/${id}/cancel`, {
      method: 'POST',
      body: '{}',
    }),
  issueVirtual: (accountId: string) =>
    request<{ virtual_card_id: string; processor_card_id: string; cvv_expiration_at: string }>(
      `/accounts/${accountId}/virtual-cards`,
      { method: 'POST', body: '{}' },
    ),
  listCustomers: () => request<CustomerSummary[]>('/customers'),
  getCustomer: (id: string) => request<CustomerDetail>(`/customers/${id}`),
  listPhysical: (accountId?: string) =>
    request<PhysicalCard[]>(
      accountId ? `/physical-cards?accountId=${encodeURIComponent(accountId)}` : '/physical-cards',
    ),
  getPhysical: (id: string) => request<PhysicalCard>(`/physical-cards/${id}`),
  validatePhysical: (id: string) =>
    request<{ physical_card_id: string; validated_at: string }>(`/physical-cards/${id}/validate`, {
      method: 'POST',
      body: '{}',
    }),
  reissuePhysical: (id: string, reason: string) =>
    request<{ old_physical_card_id: string; new_physical_card_id: string; tracking_id: string }>(
      `/physical-cards/${id}/reissue`,
      { method: 'POST', body: JSON.stringify({ reason }) },
    ),
  listVirtual: (accountId?: string) =>
    request<VirtualCard[]>(
      accountId ? `/virtual-cards?accountId=${encodeURIComponent(accountId)}` : '/virtual-cards',
    ),
  getVirtual: (id: string) => request<VirtualCard>(`/virtual-cards/${id}`),
  getCvv: (id: string) =>
    request<{ cvv: number; expiration_date: string }>(`/virtual-cards/${id}/cvv`),
  reissueVirtual: (id: string, reason: string) =>
    request<{
      old_virtual_card_id: string
      new_virtual_card_id: string
      processor_card_id: string
      cvv_expiration_at: string
    }>(`/virtual-cards/${id}/reissue`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    }),
  webhookCarrier: (apiKey: string, payload: unknown) =>
    fetch('/api/webhooks/carrier/delivery', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Webhook-Api-Key': apiKey,
      },
      body: JSON.stringify(payload),
    }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    }),
  webhookProcessor: (apiKey: string, payload: unknown) =>
    fetch('/api/webhooks/processor/cvv-rotation', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Webhook-Api-Key': apiKey,
      },
      body: JSON.stringify(payload),
    }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    }),
}
