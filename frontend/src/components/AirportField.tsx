import { useMemo, useState } from 'react'
import {
  ComboBox as AriaComboBox,
  Group as AriaGroup,
  Input as AriaInput,
  Label as AriaLabel,
  ListBox as AriaListBox,
  ListBoxItem as AriaListBoxItem,
  Text as AriaText,
} from 'react-aria-components'
import { Popover } from '@/components/base/select/popover'
import { cx } from '@/utils/cx'
import { countryName, lookupAirport, searchAirports, useAirports, type Airport } from '../api/airports'

interface Props {
  label: string
  /** IATA code, or whatever the user is typing while it isn't one yet. */
  value: string
  onChange: (value: string) => void
  placeholder: string
  className?: string
}

/**
 * Airport picker: type a code ("AGP") or a city ("mála") and pick from the list.
 * Any three letters are still accepted, even if the code isn't in our list: the
 * flight APIs know more airports than the map does. The hint below confirms which
 * airport the code is.
 */
export function AirportField({ label, value, onChange, placeholder, className }: Props) {
  // Only fetched once someone touches an airport field (or the map needs it).
  const [wanted, setWanted] = useState(false)
  const airports = useAirports(wanted)
  const [query, setQuery] = useState<string | null>(null)

  const text = query ?? value
  const items = useMemo(
    () => (airports.data && query ? searchAirports(airports.data, query) : []),
    [airports.data, query],
  )
  const known = lookupAirport(airports.data, value)
  const isCode = /^[A-Z]{3}$/.test(value)
  // Just the city: the field is one board column wide. The list shows the country.
  const hint = known ? known.city : isCode || !value ? 'Código IATA' : 'Elige de la lista'

  function pick(airport: Airport) {
    onChange(airport.iata)
    setQuery(null)
  }

  return (
    <AriaComboBox
      aria-label={`${label}: aeropuerto o ciudad`}
      className={cx('flex flex-col gap-1.5', className)}
      items={items}
      inputValue={text}
      allowsCustomValue
      allowsEmptyCollection={false}
      menuTrigger="input"
      onInputChange={(typed) => {
        setWanted(true)
        setQuery(typed)
        // A bare code counts straight away; anything else waits for a pick.
        onChange(/^[a-z]{3}$/i.test(typed) ? typed.toUpperCase() : typed)
      }}
      onSelectionChange={(key) => {
        const airport = key ? airports.data?.get(String(key)) : undefined
        if (airport) pick(airport)
      }}
      onFocus={() => setWanted(true)}
      onBlur={() => setQuery(null)}
    >
      <AriaLabel className="text-sm font-medium text-secondary">{label}</AriaLabel>
      <AriaGroup className="relative flex w-full rounded-lg bg-primary shadow-xs ring-1 ring-primary transition-shadow duration-100 ease-linear ring-inset focus-within:ring-2 focus-within:ring-brand">
        <AriaInput
          placeholder={placeholder}
          className="m-0 w-full bg-transparent px-3 py-2 text-md text-primary outline-hidden placeholder:text-placeholder"
          autoComplete="off"
          spellCheck={false}
        />
      </AriaGroup>
      <AriaText
        slot="description"
        className="truncate text-sm text-tertiary"
        title={known ? `${known.name}, ${countryName(known.country)}` : undefined}
      >
        {hint}
      </AriaText>

      <Popover size="lg" placement="bottom start" className="w-80! max-w-[calc(100vw-2rem)]">
        <AriaListBox className="outline-hidden">
          {(airport: Airport) => (
            <AriaListBoxItem
              id={airport.iata}
              // React Aria writes the picked item's textValue into the input: keep it the bare code.
              textValue={airport.iata}
              className={({ isFocused }) =>
                cx(
                  'mx-1 flex cursor-pointer items-baseline gap-3 rounded-md px-2.5 py-2 outline-hidden',
                  isFocused && 'bg-primary_hover',
                )
              }
            >
              <span className="w-10 shrink-0 font-display text-md font-semibold tracking-wide text-fg-brand-primary">
                {airport.iata}
              </span>
              <span className="flex min-w-0 flex-col">
                <span className="truncate text-sm font-medium text-primary">
                  {airport.city}, {countryName(airport.country)}
                </span>
                <span className="truncate text-xs text-tertiary">{airport.name}</span>
              </span>
            </AriaListBoxItem>
          )}
        </AriaListBox>
      </Popover>
    </AriaComboBox>
  )
}
