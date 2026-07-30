import { FormEvent, useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, useNavigate, useParams } from 'react-router-dom'
import { api } from './api/client'
import { useAuth } from './auth/AuthContext'
import type {
  AccountDetail,
  AccountSummary,
  CustomerDetail,
  CustomerSummary,
  PhysicalCard,
  VirtualCard,
} from './types'

function Shell({ title, children }: { title: string; children: ReactNode }) {
  const { logout } = useAuth()
  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand">
          <span className="brand-mark" aria-hidden />
          Cards Desk
        </Link>
        <nav className="nav">
          <NavLink to="/" end>
            Ops
          </NavLink>
          <NavLink to="/accounts">Contas</NavLink>
          <NavLink to="/customers">Clientes</NavLink>
          <NavLink to="/physical-cards">Físicos</NavLink>
          <NavLink to="/virtual-cards">Virtuais</NavLink>
          <NavLink to="/webhooks">Webhooks</NavLink>
        </nav>
        <button type="button" className="btn ghost" onClick={logout}>
          Sair
        </button>
      </header>
      <div className="stack">
        <h1>{title}</h1>
        {children}
      </div>
    </div>
  )
}

function statusBadge(status: string) {
  const s = status.toUpperCase()
  const cls = s.includes('ACTIVE') || s.includes('DELIVERED') || s === 'VALIDATED'
    ? 'ok'
    : s.includes('CANCEL') || s.includes('INACTIVE') || s.includes('RETURN')
      ? 'danger'
      : 'warn'
  return <span className={`badge ${cls}`}>{status}</span>
}

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('alice')
  const [password, setPassword] = useState('alice')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha no login')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-hero">
      <section className="login-visual">
        <div className="login-copy">
          <p className="brand">Cards Desk</p>
          <p>Operações de emissão AltBank — contas, cartões físicos e virtuais em um console único.</p>
        </div>
      </section>
      <section className="login-panel">
        <form className="panel" onSubmit={onSubmit}>
          <h2>Entrar</h2>
          <p className="muted">Keycloak realm quarkus · demo portfolio</p>
          <label>
            Utilizador
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="btn" type="submit" disabled={loading}>
            {loading ? 'A autenticar…' : 'Aceder ao desk'}
          </button>
        </form>
      </section>
    </div>
  )
}

export function DashboardPage() {
  const [accounts, setAccounts] = useState<AccountSummary[]>([])
  const [customers, setCustomers] = useState<CustomerSummary[]>([])
  const [physical, setPhysical] = useState<PhysicalCard[]>([])
  const [virtual, setVirtual] = useState<VirtualCard[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([api.listAccounts(), api.listCustomers(), api.listPhysical(), api.listVirtual()])
      .then(([a, c, p, v]) => {
        setAccounts(a)
        setCustomers(c)
        setPhysical(p)
        setVirtual(v)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [])

  return (
    <Shell title="Ops desk">
      {error && <p className="error">{error}</p>}
      <div className="grid-stats">
        <div className="stat">
          <span>Contas</span>
          <strong>{accounts.length}</strong>
        </div>
        <div className="stat">
          <span>Clientes</span>
          <strong>{customers.length}</strong>
        </div>
        <div className="stat">
          <span>Cartões físicos</span>
          <strong>{physical.length}</strong>
        </div>
        <div className="stat">
          <span>Cartões virtuais</span>
          <strong>{virtual.length}</strong>
        </div>
      </div>
      <div className="panel">
        <p className="muted">Fluxo típico: criar conta → webhook de entrega → validar físico → emitir virtual.</p>
        <div className="row" style={{ marginTop: '0.8rem' }}>
          <Link className="btn" to="/accounts">
            Gerir contas
          </Link>
          <Link className="btn ghost" to="/webhooks">
            Consola webhooks
          </Link>
        </div>
      </div>
    </Shell>
  )
}

export function AccountsPage() {
  const [items, setItems] = useState<AccountSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    fullName: '',
    document: '',
    email: '',
    phone: '',
    street: '',
    number: '',
    city: 'São Paulo',
    state: 'SP',
    zipCode: '01000-000',
    country: 'BR',
  })

  async function load() {
    setItems(await api.listAccounts())
  }

  useEffect(() => {
    load().catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [])

  async function onCreate(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.createAccount({
        customer: {
          full_name: form.fullName,
          document: form.document,
          email: form.email,
          phone: form.phone || null,
        },
        address: {
          street: form.street,
          number: form.number,
          city: form.city,
          state: form.state,
          zip_code: form.zipCode,
          country: form.country,
        },
      })
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao criar')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Shell title="Contas">
      {error && <p className="error">{error}</p>}
      <div className="panel stack">
        <h2>Nova conta</h2>
        <form className="stack" onSubmit={onCreate}>
          <div className="form-grid">
            <label>
              Nome
              <input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            </label>
            <label>
              Documento
              <input required value={form.document} onChange={(e) => setForm({ ...form, document: e.target.value })} />
            </label>
            <label>
              Email
              <input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label>
              Telefone
              <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </label>
            <label>
              Rua
              <input required value={form.street} onChange={(e) => setForm({ ...form, street: e.target.value })} />
            </label>
            <label>
              Número
              <input required value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} />
            </label>
            <label>
              Cidade
              <input required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
            </label>
            <label>
              UF
              <input required value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} />
            </label>
            <label>
              CEP
              <input required value={form.zipCode} onChange={(e) => setForm({ ...form, zipCode: e.target.value })} />
            </label>
            <label>
              País
              <input required value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} />
            </label>
          </div>
          <button className="btn" disabled={busy} type="submit">
            Criar conta + cartão físico
          </button>
        </form>
      </div>
      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>Cliente</th>
              <th>Documento</th>
              <th>Status</th>
              <th>Criada</th>
            </tr>
          </thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.id}>
                <td>
                  <Link to={`/accounts/${a.id}`}>{a.customer_name}</Link>
                </td>
                <td className="mono">{a.document}</td>
                <td>{statusBadge(a.status)}</td>
                <td className="muted">{a.created_at}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  )
}

export function AccountDetailPage() {
  const { id = '' } = useParams()
  const [item, setItem] = useState<AccountDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  async function load() {
    setItem(await api.getAccount(id))
  }

  useEffect(() => {
    load().catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [id])

  async function cancel() {
    setError(null)
    try {
      await api.cancelAccount(id)
      setMsg('Conta cancelada')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  async function issueVirtual() {
    setError(null)
    try {
      const res = await api.issueVirtual(id)
      setMsg(`Virtual emitido: ${res.virtual_card_id}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  if (!item) {
    return (
      <Shell title="Conta">
        {error ? <p className="error">{error}</p> : <p className="muted">A carregar…</p>}
      </Shell>
    )
  }

  return (
    <Shell title="Detalhe da conta">
      {error && <p className="error">{error}</p>}
      {msg && <p className="muted">{msg}</p>}
      <div className="panel stack">
        <div className="row">
          {statusBadge(item.status)}
          <span className="mono muted">{item.id}</span>
        </div>
        <p>
          <strong>{item.customer.full_name}</strong> · {item.customer.email}
        </p>
        <div className="row">
          <button className="btn" type="button" onClick={issueVirtual}>
            Emitir virtual
          </button>
          <button className="btn danger" type="button" onClick={cancel}>
            Cancelar conta
          </button>
        </div>
        <div>
          <h3>Cartões físicos</h3>
          <ul>
            {item.physical_card_ids.map((cid) => (
              <li key={cid}>
                <Link to={`/physical-cards/${cid}`}>{cid}</Link>
              </li>
            ))}
          </ul>
          <h3>Cartões virtuais</h3>
          <ul>
            {item.virtual_card_ids.map((cid) => (
              <li key={cid}>
                <Link to={`/virtual-cards/${cid}`}>{cid}</Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </Shell>
  )
}

export function CustomersPage() {
  const [items, setItems] = useState<CustomerSummary[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.listCustomers()
      .then(setItems)
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [])

  return (
    <Shell title="Clientes">
      {error && <p className="error">{error}</p>}
      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Documento</th>
              <th>Email</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>
                  <Link to={`/customers/${c.id}`}>{c.full_name}</Link>
                </td>
                <td className="mono">{c.document}</td>
                <td>{c.email}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  )
}

export function CustomerDetailPage() {
  const { id = '' } = useParams()
  const [item, setItem] = useState<CustomerDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getCustomer(id)
      .then(setItem)
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [id])

  return (
    <Shell title="Cliente">
      {error && <p className="error">{error}</p>}
      {item && (
        <div className="panel stack">
          <h2>{item.full_name}</h2>
          <p className="mono muted">{item.id}</p>
          <p>
            {item.document} · {item.email} · {item.phone || 'sem telefone'}
          </p>
          <p>
            {item.address.street}, {item.address.number} — {item.address.city}/{item.address.state}{' '}
            {item.address.zip_code} ({item.address.country})
          </p>
          {item.account_id && (
            <p>
              Conta: <Link to={`/accounts/${item.account_id}`}>{item.account_id}</Link>
            </p>
          )}
        </div>
      )}
    </Shell>
  )
}

export function PhysicalCardsPage() {
  const [items, setItems] = useState<PhysicalCard[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.listPhysical()
      .then(setItems)
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [])

  return (
    <Shell title="Cartões físicos">
      {error && <p className="error">{error}</p>}
      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>Tracking</th>
              <th>Entrega</th>
              <th>Status</th>
              <th>Conta</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>
                  <Link to={`/physical-cards/${c.id}`}>{c.tracking_id}</Link>
                </td>
                <td>{statusBadge(c.delivery_status)}</td>
                <td>{statusBadge(c.status)}</td>
                <td className="mono muted">{c.account_id.slice(0, 8)}…</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  )
}

export function PhysicalCardDetailPage() {
  const { id = '' } = useParams()
  const [item, setItem] = useState<PhysicalCard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)
  const [reason, setReason] = useState('LOSS')

  async function load() {
    setItem(await api.getPhysical(id))
  }

  useEffect(() => {
    load().catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [id])

  async function validate() {
    try {
      await api.validatePhysical(id)
      setMsg('Cartão validado')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  async function reissue() {
    try {
      const res = await api.reissuePhysical(id, reason)
      setMsg(`Reemitido → ${res.new_physical_card_id}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  if (!item) {
    return (
      <Shell title="Cartão físico">
        {error ? <p className="error">{error}</p> : <p className="muted">A carregar…</p>}
      </Shell>
    )
  }

  return (
    <Shell title="Cartão físico">
      {error && <p className="error">{error}</p>}
      {msg && <p className="muted">{msg}</p>}
      <div className="panel stack">
        <div className="row">
          {statusBadge(item.status)}
          {statusBadge(item.delivery_status)}
        </div>
        <p className="mono">{item.tracking_id}</p>
        <p className="muted">
          Entrega: {item.delivered_at || '—'} · Validação: {item.validated_at || '—'}
        </p>
        <p>
          Conta: <Link to={`/accounts/${item.account_id}`}>{item.account_id}</Link>
        </p>
        <div className="row">
          <button className="btn" type="button" onClick={validate}>
            Validar
          </button>
          <select value={reason} onChange={(e) => setReason(e.target.value)}>
            <option value="LOSS">LOSS</option>
            <option value="THEFT">THEFT</option>
            <option value="DAMAGE">DAMAGE</option>
          </select>
          <button className="btn ghost" type="button" onClick={reissue}>
            Reemitir
          </button>
          <Link className="btn ghost" to="/webhooks">
            Simular entrega
          </Link>
        </div>
      </div>
    </Shell>
  )
}

export function VirtualCardsPage() {
  const [items, setItems] = useState<VirtualCard[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.listVirtual()
      .then(setItems)
      .catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [])

  return (
    <Shell title="Cartões virtuais">
      {error && <p className="error">{error}</p>}
      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>Processor card</th>
              <th>Status</th>
              <th>CVV expira</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>
                  <Link to={`/virtual-cards/${c.id}`}>{c.processor_card_id}</Link>
                </td>
                <td>{statusBadge(c.status)}</td>
                <td className="muted">{c.cvv_expiration_at || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Shell>
  )
}

export function VirtualCardDetailPage() {
  const { id = '' } = useParams()
  const [item, setItem] = useState<VirtualCard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cvv, setCvv] = useState<string | null>(null)
  const [reason, setReason] = useState('LOSS')

  async function load() {
    setItem(await api.getVirtual(id))
  }

  useEffect(() => {
    load().catch((err) => setError(err instanceof Error ? err.message : 'Erro'))
  }, [id])

  async function revealCvv() {
    try {
      const res = await api.getCvv(id)
      setCvv(`${res.cvv} (expira ${res.expiration_date})`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  async function reissue() {
    try {
      await api.reissueVirtual(id, reason)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  if (!item) {
    return (
      <Shell title="Cartão virtual">
        {error ? <p className="error">{error}</p> : <p className="muted">A carregar…</p>}
      </Shell>
    )
  }

  return (
    <Shell title="Cartão virtual">
      {error && <p className="error">{error}</p>}
      <div className="panel stack">
        {statusBadge(item.status)}
        <p className="mono">{item.processor_card_id}</p>
        <p>
          Conta: <Link to={`/accounts/${item.account_id}`}>{item.account_id}</Link>
        </p>
        <div className="row">
          <button className="btn" type="button" onClick={revealCvv}>
            Revelar CVV
          </button>
          <select value={reason} onChange={(e) => setReason(e.target.value)}>
            <option value="LOSS">LOSS</option>
            <option value="THEFT">THEFT</option>
            <option value="DAMAGE">DAMAGE</option>
          </select>
          <button className="btn ghost" type="button" onClick={reissue}>
            Reemitir
          </button>
        </div>
        {cvv && <p className="mono">CVV: {cvv}</p>}
      </div>
    </Shell>
  )
}

export function WebhooksPage() {
  const [carrierKey, setCarrierKey] = useState('carrier-local-key')
  const [processorKey, setProcessorKey] = useState('processor-local-key')
  const [trackingId, setTrackingId] = useState('')
  const [accountId, setAccountId] = useState('')
  const [cardId, setCardId] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function sendCarrier(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const res = await api.webhookCarrier(carrierKey, {
        tracking_id: trackingId,
        delivery_status: 'DELIVERED',
        delivery_date: new Date().toISOString().slice(0, 19),
        delivery_address: 'Endereço demo',
      })
      setMsg(JSON.stringify(res))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  async function sendProcessor(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const res = await api.webhookProcessor(processorKey, {
        account_id: accountId,
        card_id: cardId,
        next_cvv: 123,
        expiration_date: new Date(Date.now() + 900_000).toISOString().slice(0, 19),
      })
      setMsg(JSON.stringify(res))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro')
    }
  }

  return (
    <Shell title="Webhooks">
      <p className="muted">A API não persiste histórico de webhooks — esta consola dispara os POSTs operacionais.</p>
      {error && <p className="error">{error}</p>}
      {msg && <p className="mono muted">{msg}</p>}
      <div className="panel stack">
        <h2>Transportadora · entrega</h2>
        <form className="stack" onSubmit={sendCarrier}>
          <div className="form-grid">
            <label>
              API key
              <input value={carrierKey} onChange={(e) => setCarrierKey(e.target.value)} />
            </label>
            <label>
              Tracking ID
              <input required value={trackingId} onChange={(e) => setTrackingId(e.target.value)} />
            </label>
          </div>
          <button className="btn" type="submit">
            Marcar DELIVERED
          </button>
        </form>
      </div>
      <div className="panel stack">
        <h2>Processadora · rotação CVV</h2>
        <form className="stack" onSubmit={sendProcessor}>
          <div className="form-grid">
            <label>
              API key
              <input value={processorKey} onChange={(e) => setProcessorKey(e.target.value)} />
            </label>
            <label>
              Account ID
              <input required value={accountId} onChange={(e) => setAccountId(e.target.value)} />
            </label>
            <label>
              Card ID (processor)
              <input required value={cardId} onChange={(e) => setCardId(e.target.value)} />
            </label>
          </div>
          <button className="btn" type="submit">
            Rotacionar CVV
          </button>
        </form>
      </div>
    </Shell>
  )
}
