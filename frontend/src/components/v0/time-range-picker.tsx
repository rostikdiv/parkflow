'use client'

import { Clock, ArrowRight } from 'lucide-react'

interface TimeRangePickerProps {
  /** ISO 8601 date string for range start */
  fromIso: string
  /** ISO 8601 date string for range end */
  toIso: string
  onFromChange: (iso: string) => void
  onToChange: (iso: string) => void
}

function isoToDateLocal(iso: string): { date: string; time: string } {
  // Parse ISO string to local date+time inputs
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return { date, time };
}

function dateTimeToIso(date: string, time: string): string {
  // Combine local date+time to ISO
  return new Date(`${date}T${time}:00`).toISOString();
}

export function TimeRangePicker({ fromIso, toIso, onFromChange, onToChange }: TimeRangePickerProps) {
  const from = isoToDateLocal(fromIso);
  const to   = isoToDateLocal(toIso);

  function handleFromDate(date: string) {
    onFromChange(dateTimeToIso(date, from.time));
  }
  function handleFromTime(time: string) {
    onFromChange(dateTimeToIso(from.date, time));
  }
  function handleToDate(date: string) {
    onToChange(dateTimeToIso(date, to.time));
  }
  function handleToTime(time: string) {
    onToChange(dateTimeToIso(to.date, time));
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-3">
      <div className="mb-2 flex items-center justify-between px-1">
        <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
          <Clock size={14} aria-hidden="true" />
          Check availability for a time range
        </div>
        <button
          type="button"
          onClick={() => {
            const n = new Date()
            const t = new Date(n.getTime() + 2 * 60 * 60 * 1000)
            onFromChange(n.toISOString())
            onToChange(t.toISOString())
          }}
          className="rounded-full bg-secondary px-2 py-0.5 text-[10px] font-semibold tracking-wide text-foreground hover:bg-accent transition-colors"
        >
          Now
        </button>
      </div>
      <div className="flex items-start gap-2">
        <DateTimeField
          label="From"
          date={from.date}
          time={from.time}
          onDateChange={handleFromDate}
          onTimeChange={handleFromTime}
        />
        <ArrowRight size={16} className="mt-6 shrink-0 text-muted-foreground" aria-hidden="true" />
        <DateTimeField
          label="To"
          date={to.date}
          time={to.time}
          onDateChange={handleToDate}
          onTimeChange={handleToTime}
        />
      </div>
    </div>
  )
}

function DateTimeField({
  label,
  date,
  time,
  onDateChange,
  onTimeChange,
}: {
  label: string
  date: string
  time: string
  onDateChange: (v: string) => void
  onTimeChange: (v: string) => void
}) {
  return (
    <label className="flex flex-1 flex-col gap-1.5 rounded-xl bg-secondary px-3 py-2 transition-colors duration-150 focus-within:bg-accent">
      <span className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </span>
      <input
        type="date"
        value={date}
        onChange={(e) => onDateChange(e.target.value)}
        aria-label={`${label} date`}
        className="w-full bg-transparent text-xs text-foreground outline-none [color-scheme:dark]"
      />
      <input
        type="time"
        value={time}
        onChange={(e) => onTimeChange(e.target.value)}
        aria-label={`${label} time`}
        className="w-full bg-transparent font-mono text-sm text-foreground outline-none [color-scheme:dark]"
      />
    </label>
  )
}
