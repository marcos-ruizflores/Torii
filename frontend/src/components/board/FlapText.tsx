import { useEffect, useRef, useState } from 'react'
import { cx } from '@/utils/cx'

const CHARSET = ' ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
const FLIP_MS = 38
/** Extra flips per position, so tiles further right settle a little later. */
const STAGGER = 1.4
const BASE_FLIPS = 5

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

interface Props {
  text: string
  /** Pad to a fixed number of tiles so the board keeps its columns. */
  length?: number
  /** Which side the padding goes on: numbers sit right-aligned like on a board. */
  align?: 'left' | 'right'
  tone?: 'ink' | 'signal' | 'dim'
  className?: string
  /** Settle straight away (e.g. the wordmark on every page). */
  still?: boolean
}

/**
 * Text drawn as split-flap tiles. When `text` changes the tiles run through the
 * alphabet and settle on the new value, like a departures board updating (about
 * half a second, once per change). Under prefers-reduced-motion they just switch.
 * Screen readers get the plain text, the tiles are decoration.
 */
export function FlapText({ text, length, align = 'left', tone = 'ink', className, still = false }: Props) {
  const padded = length ? (align === 'right' ? text.padStart(length, ' ') : text.padEnd(length, ' ')) : text
  // A blank tile inside a figure reads as a missing digit, so "755 €" becomes "755€".
  const target = padded.replace(/\u00a0/g, ' ').replace(/(\d) (€|\$|£)/g, '$1$2').toUpperCase()
  const [shown, setShown] = useState(target)
  const timer = useRef<number | null>(null)

  useEffect(() => {
    if (still || prefersReducedMotion()) {
      setShown(target)
      return
    }
    let tick = 0
    const settleAt = [...target].map((_, i) => BASE_FLIPS + Math.round(i * STAGGER))
    timer.current = window.setInterval(() => {
      tick++
      setShown(
        [...target]
          .map((ch, i) => {
            if (tick >= settleAt[i] || ch === ' ' || !CHARSET.includes(ch)) return ch
            return CHARSET[(CHARSET.indexOf(ch) + settleAt[i] - tick + CHARSET.length) % CHARSET.length]
          })
          .join(''),
      )
      if (tick >= Math.max(...settleAt)) {
        window.clearInterval(timer.current!)
      }
    }, FLIP_MS)
    return () => {
      if (timer.current) window.clearInterval(timer.current)
    }
  }, [target, still])

  return (
    <span className={cx('inline-flex items-center gap-[0.08em]', className)}>
      <span className="sr-only">{text}</span>
      <span aria-hidden="true" className="inline-flex gap-[0.08em]">
        {[...shown].map((ch, i) => (
          <span
            key={i}
            className={cx(
              'flap',
              ch === ' ' && 'flap-blank',
              tone === 'signal' && 'text-fg-brand-primary',
              tone === 'dim' && 'text-tertiary',
              tone === 'ink' && 'text-primary',
            )}
          >
            {ch === ' ' ? ' ' : ch}
          </span>
        ))}
      </span>
    </span>
  )
}

/** A row of blank tiles, ticking while Torii scans (static with reduced motion). */
export function FlapBlank({ length, ticking = false, className }: { length: number; ticking?: boolean; className?: string }) {
  return (
    <span aria-hidden="true" className={cx('inline-flex gap-[0.08em]', ticking && 'flap-ticking', className)}>
      {Array.from({ length }).map((_, i) => (
        <span key={i} className="flap flap-blank">
          {' '}
        </span>
      ))}
    </span>
  )
}
