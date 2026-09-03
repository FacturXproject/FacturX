import { NavLink, useNavigate } from 'react-router-dom';
import { ShieldCheck, Building2, FileCheck2, RefreshCw, ScanLine, LogOut, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/dashboard', icon: ShieldCheck, label: 'Tableau de bord' },
  { to: '/organisations', icon: Building2, label: 'Organisations' },
  { to: '/invitations', icon: Users, label: 'Invitations' },
  { to: '/verifier', icon: FileCheck2, label: 'Vérifier' },
  { to: '/convertir', icon: RefreshCw, label: 'Convertir' },
  { to: '/lecture-xml', icon: ScanLine, label: 'Lecture XML' },
];

export default function Layout({ children }) {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate('/login');
    }
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f8f9fa' }}>
      {/* Sidebar */}
      <aside style={{
        width: '240px',
        flexShrink: 0,
        background: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid #e5e7eb',
      }}>
        {/* Logo */}
        <div style={{ padding: '22px 16px 18px', borderBottom: '1px solid #f3f4f6' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
            <ShieldCheck size={24} color="#1a2744" strokeWidth={2.2} />
            <div>
              <div style={{ color: '#111827', fontWeight: 700, fontSize: '17px', letterSpacing: '-0.3px' }}>Factur-X</div>
              <div style={{ color: '#6b7280', fontSize: '12px' }}>Conformité & Conversion</div>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav style={{ flex: 1, padding: '10px 0' }}>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '11px',
                padding: '11px 16px',
                textDecoration: 'none',
                fontSize: '15.5px',
                color: isActive ? '#1a2744' : '#6b7280',
                background: isActive ? '#eff6ff' : 'transparent',
                borderLeft: isActive ? '3px solid #4a9eff' : '3px solid transparent',
                fontWeight: isActive ? 550 : 450,
                transition: 'all 0.15s',
              })}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div style={{ padding: '12px 0', borderTop: '1px solid #f3f4f6' }}>
          <button
            onClick={handleLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '11px',
              padding: '11px 16px',
              width: '100%',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#6b7280',
              fontSize: '15.5px',
              textAlign: 'left',
            }}
          >
            <LogOut size={18} />
            Déconnexion
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main style={{ flex: 1, overflow: 'auto' }}>
        {children}
      </main>
    </div>
  );
}