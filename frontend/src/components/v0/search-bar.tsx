'use client'

import { Search, X } from 'lucide-react'
import { cn } from '@/lib/utils'

export function SearchBar({
  value,
  onChange,
}: {
  value: string
  onChange: (value: string) => void
}) {
  return (
    <div
      className={cn(
        'flex h-14 items-center gap-3 rounded-full border border-border bg-card/90 px-4 backdrop-blur-md elevation-2',
        'transition-shadow duration-150 focus-within:elevation-3',
      )}
    >
      <Search size={20} className="shrink-0 text-muted-foreground" aria-hidden="true" />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Search for parking near you"
        aria-label="Search for parking locations"
        className="min-w-0 flex-1 bg-transparent text-[15px] text-foreground outline-none placeholder:text-muted-foreground"
      />
      {value ? (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Clear search"
          className="grid size-8 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
        >
          <X size={16} aria-hidden="true" />
        </button>
      ) : (
        <button
          type="button"
          aria-label="Search"
          className="grid size-8 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
        >
          <Search size={18} aria-hidden="true" />
        </button>
      )}
    </div>
  )
}
