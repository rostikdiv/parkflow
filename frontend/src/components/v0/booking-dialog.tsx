'use client'

import { useEffect, useMemo, useState } from 'react'
import { Dialog } from '@base-ui/react/dialog'
import { Loader2, CircleParking, Minus, Plus } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ParkingLot, Spot } from '@/lib/parking'

const PLATE_RE = /^[A-Z]{2}\d{4}[A-Z]{2}$/

export function BookingDialog({
  open,
  lot,
  spot,
  onOpenChange,
  onConfirm,
}: {
  open: boolean
  lot: ParkingLot | null
  spot: Spot | null
  onOpenChange: (open: boolean) => void
  onConfirm: (details: { plate: string; hours: number }) => Promise<void>
}) {
  const [plate, setPlate] = useState('')
  const [hours, setHours] = useState(2)
  const [submitting, setSubmitting] = useState(false)
  const [touched, setTouched] = useState(false)

  // Reset form whenever a new spot is opened.
  useEffect(() => {
    if (open) {
      setPlate('')
      setHours(2)
      setSubmitting(false)
      setTouched(false)
    }
  }, [open, spot?.id])

  const rate = lot?.hourlyRate ?? 0
  const total = useMemo(() => (rate * hours).toFixed(2), [rate, hours])
  const plateValid = PLATE_RE.test(plate)

  async function handleConfirm() {
    if (!plateValid || submitting) {
      setTouched(true)
      return
    }
    setSubmitting(true)
    await onConfirm({ plate, hours })
    // Parent closes the dialog; guard against unmount race.
    setSubmitting(false)
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-[90] bg-black/60 backdrop-blur-sm transition-opacity duration-200 data-[ending-style]:opacity-0 data-[starting-style]:opacity-0" />
        <Dialog.Popup
          className={cn(
            'fixed left-1/2 top-1/2 z-[95] w-[calc(100vw-2rem)] max-w-sm -translate-x-1/2 -translate-y-1/2',
            'rounded-3xl border border-border bg-popover p-6 text-popover-foreground elevation-5 outline-none',
            'transition-all duration-200',
            'data-[starting-style]:scale-95 data-[starting-style]:opacity-0',
            'data-[ending-style]:scale-95 data-[ending-style]:opacity-0',
          )}
        >
          <div className="mb-5 flex items-center gap-3">
            <div className="grid size-12 shrink-0 place-items-center rounded-2xl bg-primary/15 text-primary">
              <CircleParking size={24} aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <Dialog.Title className="text-lg font-semibold leading-tight">
                Book spot {spot?.label}
              </Dialog.Title>
              <Dialog.Description className="truncate text-sm text-muted-foreground">
                {lot?.name}
              </Dialog.Description>
            </div>
          </div>

          {/* License plate */}
          <div className="mb-4">
            <label htmlFor="plate" className="mb-1.5 block text-sm font-medium">
              License plate
            </label>
            <input
              id="plate"
              value={plate}
              onChange={(e) => setPlate(e.target.value.toUpperCase().replace(/\s/g, ''))}
              onBlur={() => setTouched(true)}
              placeholder="KA1234BB"
              maxLength={8}
              autoComplete="off"
              spellCheck={false}
              aria-invalid={touched && !plateValid}
              className={cn(
                'w-full rounded-xl border bg-secondary px-4 py-3 font-mono text-base uppercase tracking-widest text-foreground outline-none transition-colors duration-150',
                'placeholder:tracking-normal placeholder:text-muted-foreground/60',
                touched && !plateValid
                  ? 'border-destructive focus:border-destructive'
                  : 'border-border focus:border-primary',
              )}
            />
            {touched && !plateValid && (
              <p className="mt-1.5 text-xs text-destructive">
                Enter a valid license plate (e.g., KA1234BB).
              </p>
            )}
          </div>

          {/* Duration selector */}
          <div className="mb-5">
            <div className="mb-1.5 flex items-center justify-between">
              <label className="text-sm font-medium">Duration</label>
              <span className="font-mono text-sm text-primary">
                {hours} {hours === 1 ? 'hour' : 'hours'}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <StepButton
                label="Decrease duration"
                onClick={() => setHours((h) => Math.max(1, h - 1))}
                disabled={hours <= 1}
              >
                <Minus size={18} aria-hidden="true" />
              </StepButton>
              <input
                type="range"
                min={1}
                max={12}
                step={1}
                value={hours}
                onChange={(e) => setHours(Number(e.target.value))}
                aria-label="Parking duration in hours"
                className="h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-secondary accent-primary"
                style={{
                  background: `linear-gradient(to right, var(--primary) ${((hours - 1) / 11) * 100}%, var(--secondary) ${((hours - 1) / 11) * 100}%)`,
                }}
              />
              <StepButton
                label="Increase duration"
                onClick={() => setHours((h) => Math.min(12, h + 1))}
                disabled={hours >= 12}
              >
                <Plus size={18} aria-hidden="true" />
              </StepButton>
            </div>
          </div>

          {/* Total price */}
          <div className="mb-6 flex items-center justify-between rounded-2xl bg-primary/10 px-4 py-3.5">
            <span className="text-sm text-muted-foreground">Total</span>
            <span className="text-2xl font-bold tabular-nums text-primary">${total}</span>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2">
            <Dialog.Close
              disabled={submitting}
              className="rounded-full px-5 py-2.5 text-sm font-medium text-foreground transition-colors duration-150 hover:bg-accent disabled:opacity-50"
            >
              Cancel
            </Dialog.Close>
            <button
              type="button"
              onClick={handleConfirm}
              disabled={submitting || !plateValid}
              className={cn(
                'flex min-w-[9.5rem] items-center justify-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground',
                'transition-all duration-150 hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50',
              )}
            >
              {submitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" aria-hidden="true" />
                  Booking…
                </>
              ) : (
                'Confirm Booking'
              )}
            </button>
          </div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

function StepButton({
  children,
  label,
  onClick,
  disabled,
}: {
  children: React.ReactNode
  label: string
  onClick: () => void
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      disabled={disabled}
      className="grid size-9 shrink-0 place-items-center rounded-full border border-border bg-secondary text-foreground transition-colors duration-150 hover:bg-accent disabled:opacity-40"
    >
      {children}
    </button>
  )
}
