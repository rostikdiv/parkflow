'use client'

import { X, User, Mail, Phone, Shield } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '../../lib/auth'

interface ProfilePanelProps {
  onClose: () => void
}

export function ProfilePanel({ onClose }: ProfilePanelProps) {
  const { user } = useAuth()

  const initials = user?.fullName?.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() ?? 'U'

  return (
    <>
      <button
        type="button"
        aria-label="Close panel"
        onClick={onClose}
        className="fixed inset-0 z-[1990] bg-black/50 backdrop-blur-[2px] md:hidden"
      />

      <aside
        className={cn(
          'fixed z-[2000] flex flex-col border-border bg-card elevation-5',
          'inset-x-0 bottom-0 max-h-[86vh] rounded-t-3xl border-t',
          'animate-in slide-in-from-bottom duration-300 ease-out',
          'md:inset-y-0 md:left-0 md:right-auto md:max-h-none md:w-[400px] md:rounded-none md:rounded-r-3xl md:border-r md:border-t-0',
          'md:animate-in md:slide-in-from-left md:fade-in',
        )}
        aria-label="User Profile"
      >
        <div className="flex justify-center pt-3 md:hidden">
          <span className="h-1.5 w-10 rounded-full bg-border" aria-hidden="true" />
        </div>

        <header className="flex items-start gap-3 px-5 pb-4 pt-4 md:pt-6">
          <div className="min-w-0 flex-1">
            <h2 className="text-pretty text-lg font-semibold leading-tight tracking-tight">
              User Profile
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close profile"
            className="grid size-9 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <div className="flex flex-col gap-6 overflow-y-auto px-5 pb-6">
          <div className="flex flex-col items-center gap-4 text-center">
            <div className="grid size-24 place-items-center rounded-full bg-primary/20 text-3xl font-bold text-primary ring-4 ring-primary/10">
              {initials}
            </div>
            <div>
              <h3 className="text-xl font-semibold">{user?.fullName ?? 'Unknown User'}</h3>
              <p className="text-sm text-muted-foreground">{user?.email ?? 'No email'}</p>
            </div>
          </div>

          <div className="flex flex-col gap-3 rounded-2xl border border-border bg-secondary/30 p-4">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Account Details
            </h4>
            
            <div className="flex items-center gap-3 text-sm">
              <User size={16} className="text-muted-foreground" />
              <div className="flex-1">
                <p className="font-medium">Full Name</p>
                <p className="text-muted-foreground">{user?.fullName ?? '—'}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 text-sm">
              <Mail size={16} className="text-muted-foreground" />
              <div className="flex-1">
                <p className="font-medium">Email Address</p>
                <p className="text-muted-foreground">{user?.email ?? '—'}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 text-sm">
              <Phone size={16} className="text-muted-foreground" />
              <div className="flex-1">
                <p className="font-medium">Phone Number</p>
                <p className="text-muted-foreground">+380 99 123 4567</p>
              </div>
            </div>

            <div className="flex items-center gap-3 text-sm">
              <Shield size={16} className="text-muted-foreground" />
              <div className="flex-1">
                <p className="font-medium">Role</p>
                <p className="text-muted-foreground capitalize">{user?.role?.toLowerCase() ?? 'Driver'}</p>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}
