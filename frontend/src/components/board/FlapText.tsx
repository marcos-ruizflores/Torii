import { useEffect, useRef, useState } from 'react'
import { cx } from '@/utils/cx'

const CHARSET = ' ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
/** One flip: the top leaf falls, then the bottom leaf lands (see .flap-leaf in board.css). */
const FLIP_MS = 90
/** Extra flips per position, so tiles further right settle a little later. */
const STAGGER = 0.8
const BASE_FLIPS = 4

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
  /**
   * `cycle` runs each tile through the alphabet before settling (results, rare);
   * `single` drops one leaf straight to the new character (values that change often,
   * like the lookup counter).
   */
  mode?: 'cycle' | 'single'
}

interface Frame {
  prev: string
  cur: string
  /** Bumps on every flip so each tile's leaves remount and replay. */
  step: number
}

/** One split-flap tile: static halves plus, while flipping, the two moving leaves. */
function Tile({ prev, cur, step, flipping, toneClass }: {
  prev: string
  cur: string
  step: number
  flipping: boolean
  toneClass: string
}) {
  const glyph = (ch: string) => <span className="flap-glyph">{ch === ' ' ? ' ' : ch}</span>
  return (
    <span className={cx('flap', cur === ' ' && !flipping && 'flap-blank', toneClass)}>
      {/* The new character's top is already showing behind the falling leaf... */}
      <span className="flap-half flap-top">{glyph(cur)}</span>
      {/* ...and the old bottom stays until the new bottom leaf lands over it. */}
      <span className="flap-half flap-bottom">{glyph(flipping ? prev : cur)}</span>
      {flipping && (
        <>
          <span key={`t${step}`} className="flap-half flap-top flap-leaf flap-leaf-fall">
            {glyph(prev)}
          </span>
          <span key={`b${step}`} className="flap-half flap-bottom flap-leaf flap-leaf-land">
            {glyph(cur)}
          </span>
        </>
      )}
    </span>
  )
}

/**
 * Text drawn as split-flap tiles. When `text` changes the tiles flip to the new
 * value like a departures board: the top half of the old character falls forward,
 * the bottom half of the next one lands. Under prefers-reduced-motion the value
 * just switches. Screen readers get the plain text, the tiles are decoration.
 */
export function FlapText({ text, length, align = 'left', tone = 'ink', className, still = false, mode = 'cycle' }: Props) {
  const padded = length ? (align === 'right' ? text.padStart(length, ' ') : text.padEnd(length, ' ')) : text
  // A blank tile inside a figure reads as a missing digit, so "755 €" becomes "755€".
  const target = padded.replace(/ /g, ' ').replace(/(\d) (€|\$|£)/g, '$1$2').toUpperCase()
  const [frame, setFrame] = useState<Frame>({ prev: target, cur: target, step: 0 })
  const timer = useRef<number | null>(null)
  const shown = useRef(target)

  useEffect(() => {
    if (timer.current) window.clearInterval(timer.current)
    const from = shown.current.padEnd(target.length, ' ').slice(0, target.length)
    if (still || prefersReducedMotion() || from === target) {
      shown.current = target
      setFrame((f) => ({ prev: target, cur: target, step: f.step }))
      return
    }

    // How many flips each tile needs before it rests on its character.
    const settleAt = [...target].map((ch, i) =>
      mode === 'single' || ch === ' ' || !CHARSET.includes(ch) ? 1 : BASE_FLIPS + Math.round(i * STAGGER),
    )
    const charAt = (tick: number) =>
      [...target]
        .map((ch, i) => {
          if (tick >= settleAt[i]) return ch
          // Cycle backwards through the charset so the last flip lands on `ch`.
          return CHARSET[(CHARSET.indexOf(ch) + settleAt[i] - tick + CHARSET.length) % CHARSET.length]
        })
        .join('')

    let tick = 0
    let previous = from
    const advance = () => {
      tick++
      const next = charAt(tick)
      setFrame((f) => ({ prev: previous, cur: next, step: f.step + 1 }))
      previous = next
      shown.current = next
      if (tick >= Math.max(...settleAt)) {
        window.clearInterval(timer.current!)
        timer.current = null
        // Let the last leaves land, then drop them.
        window.setTimeout(() => setFrame((f) => ({ prev: next, cur: next, step: f.step })), FLIP_MS)
      }
    }
    advance()
    timer.current = window.setInterval(advance, FLIP_MS)
    return () => {
      if (timer.current) window.clearInterval(timer.current)
    }
  }, [target, still, mode])

  const toneClass = tone === 'signal' ? 'text-fg-brand-primary' : tone === 'dim' ? 'text-tertiary' : 'text-primary'
  const prev = frame.prev.padEnd(frame.cur.length, ' ')

  return (
    <span className={cx('inline-flex items-center gap-[0.08em]', className)}>
      <span className="sr-only">{text}</span>
      <span aria-hidden="true" className="inline-flex gap-[0.08em]">
        {[...frame.cur].map((ch, i) => (
          <Tile
            key={i}
            prev={prev[i]}
            cur={ch}
            step={frame.step}
            flipping={prev[i] !== ch}
            toneClass={toneClass}
          />
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
          <span className="flap-half flap-top" />
          <span className="flap-half flap-bottom" />
        </span>
      ))}
    </span>
  )
}
