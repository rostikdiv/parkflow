'use client'

import { Clock, ArrowRight } from 'lucide-react'

export function TimeRangePicker({
  from,
  to,
  onFromChange,
  onToChange,
}: {
  from: string
  to: string
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-3">
      <div className="mb-2 flex items-center gap-2 px-1 text-xs font-medium text-muted-foreground">
        <Clock size={14} aria-hidden="true" />
        Check availability for a time range
      </div>
      <div className="flex items-center gap-2">
        <TimeField label="From" value={from} onChange={onFromChange} />
        <ArrowRight size={16} className="shrink-0 text-muted-foreground" aria-hidden="true" />
        <TimeField label="To" value={to} onChange={onToChange} />
      </div>
    </div>
  )
}

function TimeField({
  label,
  value,
  onChange,
}: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label className="flex flex-1 flex-col gap-1 rounded-xl bg-secondary px-3 py-2 transition-colors duration-150 focus-within:bg-accent">
      <span className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </span>
      <input
        type="time"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={`${label} time`}
        className="w-full bg-transparent font-mono text-sm text-foreground outline-none [color-scheme:dark]"
      />
    </label>
  )
}
