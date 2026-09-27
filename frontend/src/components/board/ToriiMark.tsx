/** The torii gate mark: kasagi beam, nuki beam, centre strut and two splayed posts. */
export function ToriiMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={className}>
      <path d="M1.5 5.1c5 1.1 16 1.1 21 0l-.7 2.6c-4.6.8-15 .8-19.6 0z" />
      <path d="M3.6 10.6h16.8v1.9H3.6z" />
      <path d="M11.2 7.9h1.6v2.7h-1.6z" />
      <path d="M6.3 7.9h1.9l-.4 13.6H5.7z" />
      <path d="M15.8 7.9h1.9l.6 13.6h-2.1z" />
    </svg>
  )
}
