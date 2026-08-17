import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, CheckCircle, RefreshCw, FileCode2, LogOut, FileCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Tableau de bord' },
  { to: '/verifier', icon: CheckCircle, label: 'Vérifier' },
  { to: '/convertir', icon: RefreshCw, label: 'Convertir' },
  { to: '/lecture-xml', icon: FileCode2, label: 'Lecture XML' },
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
        width: '220px',
        flexShrink: 0,
        background: '#1a2744',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid #0f1a33',
      }}>
        {/* Logo */}
        <div style={{ padding: '20px 16px 16px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileCheck size={20} color="#4a9eff" />
            <div>
              <div style={{ color: '#fff', fontWeight: 700, fontSize: '15px', letterSpacing: '-0.3px' }}>Factur-X</div>
              <div style={{ color: 'rgba(255,255,255,0.45)', fontSize: '11px' }}>Conformité & Conversion</div>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav style={{ flex: 1, padding: '8px 0' }}>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '9px 16px',
                textDecoration: 'none',
                fontSize: '13.5px',
                color: isActive ? '#fff' : 'rgba(255,255,255,0.55)',
                background: isActive ? 'rgba(74,158,255,0.15)' : 'transparent',
                borderLeft: isActive ? '3px solid #4a9eff' : '3px solid transparent',
                fontWeight: isActive ? 500 : 400,
                transition: 'all 0.15s',
              })}
            >
              <Icon size={15} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div style={{ padding: '12px 0', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
          <button
            onClick={handleLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '9px 16px',
              width: '100%',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: 'rgba(255,255,255,0.45)',
              fontSize: '13.5px',
              textAlign: 'left',
            }}
          >
            <LogOut size={15} />
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
