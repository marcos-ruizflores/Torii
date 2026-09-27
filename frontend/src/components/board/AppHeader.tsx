import { useQuery } from '@tanstack/react-query'
import { LogOut01, Menu01 } from '@untitledui/icons'
import { Link, NavLink, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Dropdown } from '@/components/base/dropdown/dropdown'
import { cx } from '@/utils/cx'
import { fetchMyUsage } from '../../api/authApi'
import { useAuth } from '../../auth/AuthContext'
import { FlapText } from './FlapText'
import { ToriiMark } from './ToriiMark'

const PLAN_LABEL: Record<string, string> = { FREE: 'Gratis', PRO: 'Pro', BUSINESS: 'Business' }

function navClass({ isActive }: { isActive: boolean }) {
  return cx(
    'rounded-md px-2.5 py-1.5 text-sm font-semibold transition-colors duration-150',
    'outline-focus-ring focus-visible:outline-2 focus-visible:outline-offset-2',
    isActive ? 'text-fg-brand-primary' : 'text-secondary hover:text-primary',
  )
}

/** Monthly quota as a board counter: used / limit plus a bar that fills up. */
function QuotaCounter({ used, limit }: { used: number; limit: number | null }) {
  if (limit == null) {
    return <span className="board-label">Consultas ilimitadas</span>
  }
  const left = Math.max(0, limit - used)
  const ratio = Math.min(1, used / limit)
  const tone = left === 0 ? 'bg-error-solid' : ratio > 0.75 ? 'bg-warning-solid' : 'bg-fg-brand-primary'
  return (
    <div className="flex flex-col gap-1" title={`Has usado ${used} de ${limit} consultas este mes`}>
      <span className="board-label leading-none">
        <span className="text-primary">{used}</span>/{limit} consultas
      </span>
      <span className="h-1 w-16 overflow-hidden rounded-full bg-tertiary sm:w-24" aria-hidden="true">
        <span className={cx('block h-full rounded-full', tone)} style={{ width: `${ratio * 100}%` }} />
      </span>
    </div>
  )
}

/** Top bar shared by every page except login and sign up. */
export function AppHeader() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const usage = useQuery({ queryKey: ['my-usage'], queryFn: fetchMyUsage, enabled: !!user })

  const planLabel = user ? (PLAN_LABEL[user.plan] ?? user.plan) : ''

  return (
    <header className="flex items-center justify-between gap-x-4 gap-y-4 border-b border-secondary pb-5 max-sm:flex-nowrap sm:flex-wrap sm:gap-x-6">
      <Link
        to="/"
        className="flex items-center gap-3 rounded-md outline-focus-ring focus-visible:outline-2 focus-visible:outline-offset-4"
      >
        <ToriiMark className="size-8 text-fg-brand-primary" />
        <FlapText text="Torii" still className="text-xl" />
      </Link>

      {user ? (
        <>
          {/* Phones: quota and one account menu on the same line as the wordmark. */}
          <div className="flex items-center gap-3 sm:hidden">
            {usage.isSuccess && <QuotaCounter used={usage.data.used} limit={usage.data.limit} />}
            <Dropdown.Root>
              <Button color="secondary" size="sm" iconLeading={Menu01} aria-label={`Cuenta de ${user.name}`}>
                Cuenta
              </Button>
              <Dropdown.Popover className="w-56">
                <Dropdown.Menu
                  onAction={(key) => (key === 'logout' ? logout() : navigate(String(key)))}
                >
                  <Dropdown.Section>
                    <Dropdown.SectionHeader className="px-4 pt-3 pb-2 text-sm text-secondary">
                      {user.name} · <span className="text-tertiary">plan {planLabel}</span>
                    </Dropdown.SectionHeader>
                    <Dropdown.Item id="/mis-busquedas" label="Mis búsquedas" />
                    <Dropdown.Item id="/planes" label="Planes" />
                  </Dropdown.Section>
                  <Dropdown.Separator />
                  <Dropdown.Item id="logout" label="Salir" icon={LogOut01} />
                </Dropdown.Menu>
              </Dropdown.Popover>
            </Dropdown.Root>
          </div>

          <nav className="hidden items-center gap-x-2 sm:flex" aria-label="Principal">
            <NavLink to="/planes" className={navClass}>
              Planes
            </NavLink>
            <NavLink to="/mis-busquedas" className={navClass}>
              Mis búsquedas
            </NavLink>
            <span className="mx-2 h-8 w-px bg-border-secondary" aria-hidden="true" />
            {usage.isSuccess && <QuotaCounter used={usage.data.used} limit={usage.data.limit} />}
            <span className="ml-2 text-sm text-secondary">
              {user.name} · <span className="text-tertiary">{planLabel}</span>
            </span>
            <Button color="tertiary" size="sm" iconLeading={LogOut01} onClick={logout}>
              Salir
            </Button>
          </nav>
        </>
      ) : (
        <nav className="flex flex-wrap items-center gap-2" aria-label="Principal">
          <NavLink to="/planes" className={navClass}>
            Planes
          </NavLink>
          <Button color="secondary" size="sm" onClick={() => navigate('/login')}>
            Iniciar sesión
          </Button>
          <Button color="primary" size="sm" onClick={() => navigate('/signup')}>
            Crear cuenta
          </Button>
        </nav>
      )}
    </header>
  )
}
