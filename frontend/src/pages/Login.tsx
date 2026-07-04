import { useState } from 'react'
import { Mail01, Plane } from '@untitledui/icons'
import { Link, useNavigate } from 'react-router'
import { Button } from '@/components/base/buttons/button'
import { Checkbox } from '@/components/base/checkbox/checkbox'
import { Input } from '@/components/base/input/input'

/**
 * Pantalla de inicio de sesión. SOLO INTERFAZ por ahora: el submit no llama a
 * ningún sitio. Cuando exista la base de datos de usuarios, aquí se llamará a un
 * endpoint de autenticación del backend (p. ej. POST /api/auth/login) y se
 * guardará la sesión.
 */
export function Login() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    // TODO(auth): llamar al backend cuando exista la lógica de usuarios.
    navigate('/')
  }

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center bg-primary px-4 py-12">
      <div className="flex w-full max-w-sm flex-col gap-8">
        <header className="flex flex-col items-center gap-4 text-center">
          <div className="flex size-12 items-center justify-center rounded-xl bg-brand-solid text-white shadow-xs">
            <Plane className="size-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h1 className="text-display-xs font-semibold text-primary">Bienvenido de nuevo</h1>
            <p className="text-md text-tertiary">Inicia sesión para seguir cazando ofertas</p>
          </div>
        </header>

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <Input
            label="Email"
            type="email"
            placeholder="tu@email.com"
            icon={Mail01}
            isRequired
            value={email}
            onChange={setEmail}
          />
          <Input
            label="Contraseña"
            type="password"
            placeholder="••••••••"
            isRequired
            value={password}
            onChange={setPassword}
          />

          <div className="flex items-center justify-between">
            <Checkbox label="Recuérdame" />
            <Link to="#" className="text-sm font-semibold text-brand-secondary hover:underline">
              ¿Olvidaste la contraseña?
            </Link>
          </div>

          <Button type="submit" size="lg" color="primary">
            Iniciar sesión
          </Button>
        </form>

        <p className="text-center text-sm text-tertiary">
          ¿No tienes cuenta?{' '}
          <Link to="/signup" className="font-semibold text-brand-secondary hover:underline">
            Regístrate gratis
          </Link>
        </p>
      </div>
    </div>
  )
}
