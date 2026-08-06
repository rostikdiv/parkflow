'use client'

import { useState, useEffect, useMemo, useRef } from 'react'
import { X, CalendarDays, AlertTriangle, ShieldCheck, CheckCircle2, ChevronDown, Building2, Filter, CheckCircle, AlertCircle, RefreshCw, Play, Square, Search } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '../../lib/auth'
import { TimeRangePicker } from './time-range-picker'
import type { AdminReservationResponse, SpotAnomalyResponse, Page } from '@/types'

interface AdminPanelProps {
  onClose: () => void
}

export function AdminPanel({ onClose }: AdminPanelProps) {
  const { token } = useAuth()
  const [activeTab, setActiveTab] = useState<'reservations' | 'anomalies'>('reservations')
  
  // Data state
  const [allReservations, setAllReservations] = useState<AdminReservationResponse[]>([])
  const [anomalies, setAnomalies] = useState<SpotAnomalyResponse[]>([])
  const [loading, setLoading] = useState(true)

  // Emulator state
  const [isEmulatorRunning, setIsEmulatorRunning] = useState(false)
  const [emulatorStatusLoading, setEmulatorStatusLoading] = useState(false)

  // Pagination state
  const [resPage, setResPage] = useState(0)
  const [anomPage, setAnomPage] = useState(0)
  const size = 10

  // Filter state
  const [selectedLot, setSelectedLot] = useState<string>('ALL')
  const [anomalyStatus, setAnomalyStatus] = useState<'ALL' | 'UNRESOLVED' | 'RESOLVED'>('UNRESOLVED')
  const [resStatus, setResStatus] = useState<'ALL' | 'ACTIVE_ONLY'>('ACTIVE_ONLY')
  const [resSort, setResSort] = useState<'NEWEST' | 'OLDEST'>('NEWEST')
  const [userSearch, setUserSearch] = useState('')
  const [filtersExpanded, setFiltersExpanded] = useState(false)

  // Time filter state
  const [fromIso, setFromIso] = useState(() => {
    const from = new Date();
    from.setSeconds(0, 0);
    return from.toISOString();
  })
  const [toIso, setToIso] = useState(() => {
    const from = new Date();
    from.setSeconds(0, 0);
    const to = new Date(from.getTime() + 2 * 60 * 60 * 1000);
    return to.toISOString();
  })

  const uniqueLots = useMemo(() => {
    const lots = new Set<string>()
    allReservations.forEach(r => lots.add(r.lotName))
    anomalies.forEach(a => lots.add(a.lotName))
    return Array.from(lots).sort()
  }, [allReservations, anomalies])

  const filteredReservations = selectedLot === 'ALL' 
    ? allReservations 
    : allReservations.filter(r => r.lotName === selectedLot)

  const filteredAnomalies = selectedLot === 'ALL'
    ? anomalies
    : anomalies.filter(a => a.lotName === selectedLot)

  const fromTime = new Date(fromIso).getTime();
  const toTime = new Date(toIso).getTime();

  const finalReservations = filteredReservations.filter(r => {
    const rStart = new Date(r.startTime).getTime();
    const rEnd = new Date(r.endTime).getTime();
    const matchesTime = rStart <= toTime && rEnd >= fromTime;
    const matchesStatus = resStatus === 'ALL' ? true : (r.status === 'ACTIVE' || r.status === 'CONFIRMED' || r.status === 'PENDING');
    
    const search = userSearch.toLowerCase().trim();
    const matchesSearch = search === '' 
      || r.userFullName?.toLowerCase().includes(search)
      || r.userEmail?.toLowerCase().includes(search)
      || r.licensePlate?.toLowerCase().includes(search);

    return matchesTime && matchesStatus && matchesSearch;
  }).sort((a, b) => {
    const tA = new Date(a.startTime).getTime();
    const tB = new Date(b.startTime).getTime();
    return resSort === 'NEWEST' ? tB - tA : tA - tB;
  });

  const finalAnomalies = filteredAnomalies.filter(a => {
    const dTime = new Date(a.detectedAt).getTime();
    const matchesTime = dTime >= fromTime && dTime <= toTime;
    const matchesStatus = anomalyStatus === 'ALL' ? true 
                        : anomalyStatus === 'RESOLVED' ? a.resolvedAt !== null 
                        : a.resolvedAt === null;
    return matchesTime && matchesStatus;
  });

  useEffect(() => {
    setResPage(0)
    setAnomPage(0)
  }, [selectedLot, fromIso, toIso, anomalyStatus, userSearch, resStatus, resSort])

  // Paged data
  const pagedReservations = finalReservations.slice(resPage * size, (resPage + 1) * size);
  const pagedAnomalies = finalAnomalies.slice(anomPage * size, (anomPage + 1) * size);
  const totalResPages = Math.ceil(finalReservations.length / size) || 1;
  const totalAnomPages = Math.ceil(finalAnomalies.length / size) || 1;

  const fetchData = async () => {
    setLoading(true)
    try {
      const [resResp, anomResp] = await Promise.all([
        fetch(`/api/admin/v1/reservations?page=0&size=1000&sort=startTime,desc`, {
          headers: { Authorization: `Bearer ${token}` }
        }),
        fetch('/api/admin/v1/anomalies?resolved=true', {
          headers: { Authorization: `Bearer ${token}` }
        })
      ])

      if (resResp.ok) {
        const resData: Page<AdminReservationResponse> = await resResp.json()
        setAllReservations(resData.content)
      }
      if (anomResp.ok) {
        const anomData: SpotAnomalyResponse[] = await anomResp.json()
        setAnomalies(anomData)
      }
    } catch (e) {
      console.error('Failed to fetch admin data', e)
    } finally {
      setLoading(false)
    }
  }

  const fetchEmulatorStatus = async () => {
    try {
      // Proxy through backend
      const resp = await fetch(`/api/v1/emulator/status`);
      if (resp.ok) {
        const data = await resp.json();
        setIsEmulatorRunning(data.isRunning);
      }
    } catch (e) {
      console.warn('Emulator not reachable', e);
    }
  }

  const toggleEmulator = async () => {
    setEmulatorStatusLoading(true);
    try {
      const endpoint = isEmulatorRunning ? '/api/v1/emulator/stop' : '/api/v1/emulator/start';
      const resp = await fetch(endpoint, { 
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (resp.ok) {
        setIsEmulatorRunning(!isEmulatorRunning);
      }
    } catch (e) {
      console.error('Failed to toggle emulator', e);
    } finally {
      setEmulatorStatusLoading(false);
    }
  }

  useEffect(() => {
    if (token) {
      fetchData()
      fetchEmulatorStatus()

      // Poll emulator status periodically
      const intervalId = setInterval(() => {
        fetchEmulatorStatus()
      }, 5000);

      return () => clearInterval(intervalId);
    }
  }, [token])

  const resolveAnomaly = async (id: string) => {
    try {
      const resp = await fetch(`/api/admin/v1/anomalies/${id}/resolve`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (resp.ok) {
        setAnomalies(prev => prev.filter(a => a.id !== id))
      }
    } catch (e) {
      console.error('Failed to resolve anomaly', e)
    }
  }



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
          'md:inset-y-0 md:left-0 md:right-auto md:max-h-none md:w-[450px] md:rounded-none md:rounded-r-3xl md:border-r md:border-t-0',
          'md:animate-in md:slide-in-from-left md:fade-in',
        )}
      >
        <div className="flex justify-center pt-3 md:hidden">
          <span className="h-1.5 w-10 rounded-full bg-border" aria-hidden="true" />
        </div>

        <header className="flex items-start gap-3 px-5 pb-4 pt-4 md:pt-6">
          <div className="grid size-10 shrink-0 place-items-center rounded-full bg-primary/20 text-primary">
            <ShieldCheck size={20} />
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-pretty text-lg font-semibold leading-tight tracking-tight">
              Admin Dashboard
            </h2>
            <p className="text-sm text-muted-foreground">Manage lots and anomalies</p>
          </div>
          <button
            type="button"
            onClick={() => token && fetchData()}
            className="grid size-9 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
            title="Refresh"
          >
            <RefreshCw size={18} className={cn(loading && "animate-spin")} />
          </button>
          <button
            type="button"
            onClick={onClose}
            className="grid size-9 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
            title="Close"
          >
            <X size={18} />
          </button>
        </header>

        <div className="px-5 pb-3">
          <div className="flex items-center justify-between rounded-xl border border-border bg-secondary/20 p-3">
            <div>
              <p className="text-sm font-semibold">Sensor Emulator</p>
              <p className="text-xs text-muted-foreground">Generate fake bookings and sensor events</p>
            </div>
            <button
              onClick={toggleEmulator}
              disabled={emulatorStatusLoading}
              className={cn(
                "flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors disabled:opacity-50",
                isEmulatorRunning 
                  ? "bg-red-500/20 text-red-600 dark:text-red-500 hover:bg-red-500/30" 
                  : "bg-green-500/20 text-green-600 dark:text-green-500 hover:bg-green-500/30"
              )}
            >
              {isEmulatorRunning ? <Square size={14} /> : <Play size={14} />}
              {isEmulatorRunning ? "Stop" : "Start"}
            </button>
          </div>
        </div>

        <div className="px-5 pb-2">
          <button 
            onClick={() => setFiltersExpanded(!filtersExpanded)}
            className="flex items-center justify-between w-full py-2 px-3 mb-2 text-sm font-semibold rounded-lg border border-border bg-secondary/20 hover:bg-secondary/40 transition-colors"
          >
            <span className="flex items-center gap-2">
              <Filter size={16} /> Filters & Search
            </span>
            <ChevronDown size={16} className={cn("transition-transform duration-200 text-muted-foreground", filtersExpanded && "rotate-180")} />
          </button>
          
          {filtersExpanded && (
            <div className="flex flex-col gap-3 animate-in slide-in-from-top-2 fade-in duration-200 border border-border rounded-xl p-3 bg-card/50">
              <div>
            <label htmlFor="lot-filter" className="block text-xs font-medium text-muted-foreground mb-1">
              Filter by Parking Lot
            </label>
            <CustomSelect
              value={selectedLot}
              onChange={setSelectedLot}
              options={[
                { value: 'ALL', label: 'All Lots', icon: <Filter size={14} /> },
                ...uniqueLots.map(lot => ({ 
                  value: lot, 
                  label: lot, 
                  icon: <Building2 size={14} /> 
                }))
              ]}
            />
          </div>
          {activeTab === 'anomalies' && (
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1">
                Anomaly Status
              </label>
              <CustomSelect
                value={anomalyStatus}
                onChange={(val) => setAnomalyStatus(val as any)}
                options={[
                  { value: 'UNRESOLVED', label: 'Unresolved', icon: <AlertCircle size={14} /> },
                  { value: 'RESOLVED', label: 'Resolved', icon: <CheckCircle size={14} /> },
                  { value: 'ALL', label: 'All Statuses', icon: <Filter size={14} /> }
                ]}
              />
            </div>
          )}
          {activeTab === 'reservations' && (
            <div>
              <label className="block text-xs font-medium text-muted-foreground mb-1">
                Reservation Status
              </label>
              <CustomSelect
                value={resStatus}
                onChange={(val) => setResStatus(val as any)}
                options={[
                  { value: 'ACTIVE_ONLY', label: 'Active & Confirmed', icon: <CheckCircle2 size={14} /> },
                  { value: 'ALL', label: 'All (Inc. Expired/Cancelled)', icon: <Filter size={14} /> }
                ]}
              />
              
              <div className="mt-3">
                <label className="block text-xs font-medium text-muted-foreground mb-1">
                  Sort By
                </label>
                <CustomSelect
                  value={resSort}
                  onChange={(val) => setResSort(val as any)}
                  options={[
                    { value: 'NEWEST', label: 'Start Time: Newest First', icon: <CalendarDays size={14} /> },
                    { value: 'OLDEST', label: 'Start Time: Oldest First', icon: <CalendarDays size={14} /> }
                  ]}
                />
              </div>
              
              <div className="mt-3">
                <label className="block text-xs font-medium text-muted-foreground mb-1">
                  Search by User or Plate
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-muted-foreground">
                    <Search size={14} />
                  </div>
                  <input
                    type="text"
                    name="search-user-plate-admin-panel-field"
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                    placeholder="Email, name or plate..."
                    autoComplete="nope"
                    autoCorrect="off"
                    spellCheck={false}
                    className="w-full h-[38px] pl-9 pr-3 rounded-lg border border-border bg-background text-sm outline-none transition-colors focus:border-primary placeholder:text-muted-foreground/60"
                  />
                  {userSearch && (
                    <button
                      type="button"
                      onClick={() => setUserSearch('')}
                      className="absolute inset-y-0 right-0 pr-3 flex items-center text-muted-foreground hover:text-foreground"
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}
          
          <TimeRangePicker 
                fromIso={fromIso}
                toIso={toIso}
                onFromChange={setFromIso}
                onToChange={setToIso}
              />
            </div>
          )}
        </div>

        <div className="flex gap-4 border-b border-border px-5 mt-2">
          <TabButton 
            active={activeTab === 'reservations'} 
            onClick={() => setActiveTab('reservations')}
            icon={<CalendarDays size={16} />}
            label={`Reservations (${finalReservations.length})`}
          />
          <TabButton 
            active={activeTab === 'anomalies'} 
            onClick={() => setActiveTab('anomalies')}
            icon={<AlertTriangle size={16} />}
            label={`Anomalies (${finalAnomalies.length})`}
          />
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-6 scrollbar-slim">
          {loading ? (
            <div className="flex justify-center p-8">
              <div className="animate-pulse text-muted-foreground">Loading...</div>
            </div>
          ) : activeTab === 'reservations' ? (
            finalReservations.length === 0 ? (
              <p className="text-center text-muted-foreground">No reservations found for this time range.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {pagedReservations.map(res => (
                  <ReservationCard key={res.id} reservation={res} />
                ))}

                {totalResPages > 1 && (
                  <div className="flex items-center justify-between py-4 mt-2 border-t border-border/50">
                    <button
                      onClick={() => setResPage(p => Math.max(0, p - 1))}
                      disabled={resPage === 0}
                      className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
                    >
                      Previous
                    </button>
                    <span className="text-sm text-muted-foreground">
                      Page {resPage + 1} of {totalResPages}
                    </span>
                    <button
                      onClick={() => setResPage(p => Math.min(totalResPages - 1, p + 1))}
                      disabled={resPage >= totalResPages - 1}
                      className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
                    >
                      Next
                    </button>
                  </div>
                )}
              </div>
            )
          ) : (
            finalAnomalies.length === 0 ? (
              <div className="flex flex-col items-center gap-2 p-8 text-muted-foreground">
                <CheckCircle2 size={32} className="text-primary" />
                <p>All clear! No anomalies detected for this time range.</p>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                {pagedAnomalies.map(anom => (
                  <AnomalyCard 
                    key={anom.id} 
                    anomaly={anom} 
                    onResolve={() => resolveAnomaly(anom.id)} 
                  />
                ))}

                {totalAnomPages > 1 && (
                  <div className="flex items-center justify-between py-4 mt-2 border-t border-border/50">
                    <button
                      onClick={() => setAnomPage(p => Math.max(0, p - 1))}
                      disabled={anomPage === 0}
                      className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
                    >
                      Previous
                    </button>
                    <span className="text-sm text-muted-foreground">
                      Page {anomPage + 1} of {totalAnomPages}
                    </span>
                    <button
                      onClick={() => setAnomPage(p => Math.min(totalAnomPages - 1, p + 1))}
                      disabled={anomPage >= totalAnomPages - 1}
                      className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
                    >
                      Next
                    </button>
                  </div>
                )}
              </div>
            )
          )}
        </div>
      </aside>
    </>
  )
}

function TabButton({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'flex items-center gap-2 border-b-2 pb-3 pt-2 text-sm font-medium transition-colors',
        active ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
      )}
    >
      {icon}
      {label}
    </button>
  )
}

function ReservationCard({ reservation }: { reservation: AdminReservationResponse }) {
  const from = new Date(reservation.startTime)
  const to = new Date(reservation.endTime)
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-border bg-secondary/30 p-4 text-sm">
      <div className="flex items-center justify-between">
        <div>
          <span className="font-semibold text-primary">Spot {reservation.spotCode}</span>
          <p className="text-xs text-muted-foreground truncate max-w-[200px]">{reservation.lotName}</p>
        </div>
        <span className={cn('rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide', 
          reservation.status === 'CONFIRMED' || reservation.status === 'ACTIVE' ? 'bg-primary/20 text-primary' : 'bg-muted text-muted-foreground'
        )}>
          {reservation.status}
        </span>
      </div>
      <div>
        <p className="font-medium">{reservation.userFullName}</p>
        <p className="text-xs text-muted-foreground">{reservation.userEmail}</p>
      </div>
      <div className="flex justify-between items-end mt-1">
        <div className="text-xs text-muted-foreground">
          {from.toLocaleString()} - {to.toLocaleTimeString()}
        </div>
        <div className="font-mono bg-background px-2 py-1 rounded border text-xs">
          {reservation.licensePlate}
        </div>
      </div>
    </div>
  )
}

function AnomalyCard({ anomaly, onResolve }: { anomaly: SpotAnomalyResponse, onResolve: () => void }) {
  const detected = new Date(anomaly.detectedAt)
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-yellow-500/30 bg-yellow-500/10 p-4 text-sm">
      <div className="flex items-start justify-between">
        <div>
          <span className="font-bold text-yellow-600 dark:text-yellow-500">Spot {anomaly.spotCode}</span>
          <p className="text-xs text-yellow-600/80 dark:text-yellow-500/80">{anomaly.type}</p>
        </div>
        <div className="text-xs text-muted-foreground">
          {detected.toLocaleTimeString()}
        </div>
      </div>
      <div className="flex justify-end mt-2">
        {anomaly.resolvedAt ? (
          <span className="rounded-lg bg-green-500/20 text-green-600 dark:text-green-500 px-3 py-1.5 text-xs font-semibold">
            Resolved
          </span>
        ) : (
          <button
            onClick={onResolve}
            className="rounded-lg bg-yellow-500 px-3 py-1.5 text-xs font-semibold text-yellow-950 transition-colors hover:bg-yellow-400"
          >
            Resolve Anomaly
          </button>
        )}
      </div>
    </div>
  )
}

function CustomSelect({ 
  value, 
  onChange, 
  options, 
  placeholder = "Select..." 
}: { 
  value: string; 
  onChange: (val: string) => void; 
  options: { value: string; label: string; icon?: React.ReactNode }[];
  placeholder?: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedOption = options.find(o => o.value === value);

  return (
    <div className="relative w-full dropdown-container" ref={containerRef}>
      <button 
        type="button"
        onClick={() => setIsOpen(!isOpen)} 
        className="w-full px-3 py-2 bg-background border border-border hover:border-muted-foreground/50 text-foreground text-sm rounded-xl flex items-center justify-between transition-all"
      >
        <span className="truncate flex items-center gap-2">
          {selectedOption?.icon && <span className="opacity-70">{selectedOption.icon}</span>}
          {selectedOption ? selectedOption.label : placeholder}
        </span>
        <ChevronDown size={16} className={cn("text-muted-foreground transition-transform", isOpen && "rotate-180")} />
      </button>
      
      {isOpen && (
        <div className="absolute left-0 right-0 mt-1 bg-background border border-border rounded-xl shadow-xl z-[2010] overflow-hidden">
          <div className="p-1 space-y-0.5 max-h-60 overflow-y-auto scrollbar-slim">
            {options.map((opt) => (
              <div 
                key={opt.value}
                onClick={() => {
                  onChange(opt.value);
                  setIsOpen(false);
                }} 
                className={cn(
                  "px-3 py-2 text-sm rounded-lg cursor-pointer flex items-center gap-2 transition-colors",
                  value === opt.value 
                    ? "bg-primary text-primary-foreground" 
                    : "text-foreground hover:bg-secondary"
                )}
              >
                {opt.icon && <span className={cn("opacity-70", value === opt.value && "opacity-100")}>{opt.icon}</span>}
                <span className="flex-1 truncate">{opt.label}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
