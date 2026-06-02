import { useEffect, useState } from 'react'
import { Users, UserPlus, Trash2, ChevronDown, Mail, CheckCircle2, XCircle, Clock } from 'lucide-react'
import {
  getMembers, updateMemberRole, removeMember,
  getPendingInvitations, sendInvitation, revokeInvitation,
} from '../../api/members'
import type { Member, Invitation } from '../../types/members'
import type { MemberRole } from '../../types/auth'
import { useAuthStore } from '../../stores/authStore'

const ROLES: MemberRole[] = ['OWNER', 'ADMIN', 'MEMBER']
const ROLE_COLORS: Record<MemberRole, string> = {
  OWNER:  'bg-indigo-100 text-indigo-700',
  ADMIN:  'bg-amber-100 text-amber-700',
  MEMBER: 'bg-slate-100 text-slate-600',
}

function RoleBadge({ role }: { role: MemberRole }) {
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${ROLE_COLORS[role]}`}>
      {role}
    </span>
  )
}

function RoleSelector({ current, onChange, disabled }: {
  current: MemberRole; onChange: (r: MemberRole) => void; disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  return (
    <div className="relative">
      <button
        onClick={() => setOpen(o => !o)}
        disabled={disabled}
        className="flex items-center gap-1 text-sm disabled:opacity-40 disabled:cursor-not-allowed"
      >
        <RoleBadge role={current} />
        {!disabled && <ChevronDown className="w-3 h-3 text-slate-400" />}
      </button>
      {open && (
        <div className="absolute left-0 mt-1 bg-white border border-slate-200 rounded-lg shadow-lg z-10 min-w-[110px]">
          {ROLES.filter(r => r !== 'OWNER').map(r => (
            <button
              key={r}
              onClick={() => { onChange(r); setOpen(false) }}
              className={`block w-full text-left px-3 py-2 text-sm hover:bg-slate-50 ${r === current ? 'font-semibold text-indigo-600' : 'text-slate-700'}`}
            >
              {r}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default function MembersPage() {
  const [members, setMembers]         = useState<Member[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [loading, setLoading]         = useState(true)
  const [showInvite, setShowInvite]   = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole]   = useState<MemberRole>('MEMBER')
  const [inviting, setInviting]       = useState(false)
  const [toast, setToast]             = useState<{ msg: string; ok: boolean } | null>(null)

  const currentUser = useAuthStore(s => s.user)
  const currentRole = useAuthStore(s => s.role)
  const canManage   = currentRole === 'OWNER' || currentRole === 'ADMIN'

  const showToast = (msg: string, ok = true) => {
    setToast({ msg, ok })
    setTimeout(() => setToast(null), 3500)
  }

  const load = () => Promise.all([
    getMembers().then(r => setMembers(r.data.data)),
    canManage ? getPendingInvitations().then(r => setInvitations(r.data.data)) : Promise.resolve(),
  ])

  useEffect(() => {
    load().catch(() => {}).finally(() => setLoading(false))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleInvite = async () => {
    if (!inviteEmail.trim()) return
    setInviting(true)
    try {
      const res = await sendInvitation({ email: inviteEmail, role: inviteRole })
      setInvitations(i => [res.data.data, ...i])
      setInviteEmail('')
      setShowInvite(false)
      showToast('Invitation sent')
    } catch (e: any) {
      showToast(e.response?.data?.detail ?? 'Failed to send invitation', false)
    } finally {
      setInviting(false)
    }
  }

  const handleRoleChange = async (member: Member, role: MemberRole) => {
    try {
      const res = await updateMemberRole(member.id, { role })
      setMembers(ms => ms.map(m => m.id === member.id ? res.data.data : m))
      showToast('Role updated')
    } catch (e: any) {
      showToast(e.response?.data?.detail ?? 'Failed to update role', false)
    }
  }

  const handleRemove = async (member: Member) => {
    if (!confirm(`Remove ${member.fullName} from the organisation?`)) return
    try {
      await removeMember(member.id)
      setMembers(ms => ms.filter(m => m.id !== member.id))
      showToast('Member removed')
    } catch (e: any) {
      showToast(e.response?.data?.detail ?? 'Failed to remove member', false)
    }
  }

  const handleRevoke = async (inv: Invitation) => {
    try {
      await revokeInvitation(inv.id)
      setInvitations(is => is.filter(i => i.id !== inv.id))
      showToast('Invitation revoked')
    } catch {
      showToast('Failed to revoke invitation', false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-40">
        <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="space-y-8">
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-xl shadow-lg text-sm font-medium text-white ${toast.ok ? 'bg-emerald-600' : 'bg-red-600'}`}>
          {toast.ok ? <CheckCircle2 className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
          {toast.msg}
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Team Members</h1>
          <p className="text-slate-500 mt-1 text-sm">{members.length} member{members.length !== 1 ? 's' : ''}</p>
        </div>
        {canManage && (
          <button
            onClick={() => setShowInvite(s => !s)}
            className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700"
          >
            <UserPlus className="w-4 h-4" /> Invite member
          </button>
        )}
      </div>

      {showInvite && (
        <div className="bg-slate-50 border border-slate-200 rounded-xl p-5">
          <p className="text-sm font-semibold text-slate-700 mb-3">Send invitation</p>
          <div className="flex gap-3 flex-wrap">
            <input
              type="email"
              value={inviteEmail}
              onChange={e => setInviteEmail(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleInvite()}
              placeholder="colleague@company.com"
              className="flex-1 min-w-[200px] px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <select
              value={inviteRole}
              onChange={e => setInviteRole(e.target.value as MemberRole)}
              className="px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Admin</option>
            </select>
            <button
              onClick={handleInvite}
              disabled={inviting || !inviteEmail.trim()}
              className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 disabled:opacity-50"
            >
              {inviting ? 'Sending…' : 'Send invite'}
            </button>
            <button onClick={() => setShowInvite(false)} className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700">
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="border border-slate-200 rounded-xl overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
            <tr>
              <th className="py-3 px-4">Member</th>
              <th className="py-3 px-4">Role</th>
              <th className="py-3 px-4">Joined</th>
              {canManage && <th className="py-3 px-4" />}
            </tr>
          </thead>
          <tbody>
            {members.map(m => {
              const isMe    = m.userId === currentUser?.id
              const isOwner = m.role === 'OWNER'
              return (
                <tr key={m.id} className="border-t border-slate-100 hover:bg-slate-50">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-semibold text-sm shrink-0">
                        {m.fullName.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-slate-900">
                          {m.fullName}{isMe && <span className="ml-1 text-xs text-slate-400">(you)</span>}
                        </p>
                        <p className="text-xs text-slate-400">{m.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-4">
                    {canManage && !isMe && !isOwner
                      ? <RoleSelector current={m.role} onChange={r => handleRoleChange(m, r)} />
                      : <RoleBadge role={m.role} />
                    }
                  </td>
                  <td className="py-3 px-4 text-sm text-slate-400">
                    {new Date(m.joinedAt).toLocaleDateString()}
                  </td>
                  {canManage && (
                    <td className="py-3 px-4 text-right">
                      {!isMe && !isOwner && (
                        <button onClick={() => handleRemove(m)}
                          className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {canManage && invitations.length > 0 && (
        <div>
          <div className="flex items-center gap-2 mb-3">
            <Clock className="w-4 h-4 text-slate-400" />
            <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Pending Invitations</h2>
          </div>
          <div className="border border-slate-200 rounded-xl overflow-hidden">
            <table className="w-full text-left">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Email</th>
                  <th className="py-3 px-4">Role</th>
                  <th className="py-3 px-4">Expires</th>
                  <th className="py-3 px-4" />
                </tr>
              </thead>
              <tbody>
                {invitations.map(inv => (
                  <tr key={inv.id} className="border-t border-slate-100 hover:bg-slate-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <Mail className="w-4 h-4 text-slate-400" />
                        <span className="text-sm text-slate-700">{inv.email}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4"><RoleBadge role={inv.role} /></td>
                    <td className="py-3 px-4 text-sm text-slate-400">
                      {new Date(inv.expiresAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button onClick={() => handleRevoke(inv)}
                        className="text-xs text-slate-400 hover:text-red-600 font-medium">
                        Revoke
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {members.length === 0 && (
        <div className="text-center py-16 border border-dashed border-slate-200 rounded-xl">
          <Users className="w-8 h-8 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 text-sm">No members yet.</p>
        </div>
      )}
    </div>
  )
}
