import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { FlapText } from './FlapText'
import { ToriiMark } from './ToriiMark'

/** Frame for login and sign up: the board's mark, one headline and a single panel. */
export function AuthShell({ title, subtitle, children, footer }: {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}) {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center bg-primary px-4 py-12">
      <div className="flex w-full max-w-sm flex-col gap-8">
        <Link
          to="/"
          className="flex items-center gap-3 self-start rounded-md outline-focus-ring focus-visible:outline-2 focus-visible:outline-offset-4"
        >
          <ToriiMark className="size-8 text-fg-brand-primary" />
          <FlapText text="Torii" still className="text-xl" />
        </Link>
        <header className="flex flex-col gap-1.5">
          <h1 className="font-display text-display-xs font-semibold text-primary">{title}</h1>
          <p className="text-md text-tertiary">{subtitle}</p>
        </header>
        <div className="board-form rounded-xl bg-secondary p-6 ring-1 ring-secondary ring-inset">{children}</div>
        <p className="text-sm text-tertiary">{footer}</p>
      </div>
    </div>
  )
}
