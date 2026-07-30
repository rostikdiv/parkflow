'use client'

export function ProfileAvatar() {
  return (
    <button
      type="button"
      aria-label="Open profile and settings"
      className="grid size-14 shrink-0 place-items-center rounded-full border border-border bg-card/90 backdrop-blur-md elevation-2 transition-transform duration-150 hover:scale-[1.03] active:scale-95"
    >
      <span className="grid size-9 place-items-center rounded-full bg-primary text-sm font-medium text-primary-foreground">
        JD
      </span>
    </button>
  )
}
