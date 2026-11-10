import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  Building2,
  FileCheck2,
  RefreshCw,
  FileCode2,
  LogOut,
  Users,
  User,
  Search,
  KeyRound,
  Menu,
  X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Footer from './Footer';

const navItems = [
  { to: '/dashboard', icon: ShieldCheck, label: 'Tableau de bord' },
  { to: '/documents', icon: Search, label: 'Documents' },
  { to: '/organisations', icon: Building2, label: 'Organisations' },
  { to: '/invitations', icon: Users, label: 'Invitations' },
  { to: '/verifier', icon: FileCheck2, label: 'Vérifier' },
  { to: '/convertir', icon: RefreshCw, label: 'Convertir' },
  { to: '/lecture-xml', icon: FileCode2, label: 'Lecture XML' },
  { to: '/cles-api', icon: KeyRound, label: 'Clés API' },
  { to: '/profile', icon: User, label: 'Profil' },
];

// Sous cette largeur (mobile, tablette portrait) la sidebar devient un tiroir
const MOBILE_QUERY = '(max-width: 900px)';

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(
    () => typeof window !== 'undefined' && !!window.matchMedia?.(MOBILE_QUERY).matches
  );

  useEffect(() => {
    const media = window.matchMedia?.(MOBILE_QUERY);
    if (!media) return undefined;
    const onChange = (e) => setIsMobile(e.matches);
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, []);

  return isMobile;
}

export default function Layout({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const isMobile = useIsMobile();
  // Le tiroir est ouvert pour une page donnee : changer de page le referme
  const [menuOpenOn, setMenuOpenOn] = useState(null);
  const menuOpen = menuOpenOn === location.pathname;
  const setMenuOpen = (open) => setMenuOpenOn(open ? location.pathname : null);

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate('/');
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: isMobile ? 'column' : 'row',
      minHeight: '100vh',
      background: '#f8f9fa',
    }}>
      {/* Barre du haut (mobile / tablette) */}
      {isMobile && (
        <header style={{
          position: 'sticky',
          top: 0,
          zIndex: 30,
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          padding: '10px 16px',
          background: '#ffffff',
          borderBottom: '1px solid #e5e7eb',
        }}>
          <button
            type="button"
            onClick={() => setMenuOpen(true)}
            aria-label="Ouvrir le menu"
            style={{ display: 'flex', background: 'none', border: 'none', cursor: 'pointer', padding: '4px', color: '#1a2744' }}
          >
            <Menu size={22} />
          </button>
          <ShieldCheck size={22} color="#1a2744" strokeWidth={2.2} />
          <span style={{ color: '#111827', fontWeight: 700, fontSize: '16px' }}>Factur-X</span>
        </header>
      )}

      {/* Fond assombri derriere le tiroir */}
      {isMobile && menuOpen && (
        <div
          onClick={() => setMenuOpen(false)}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', zIndex: 40 }}
        />
      )}

      {/* Sidebar */}
      <aside style={{
        width: '240px',
        flexShrink: 0,
        background: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid #e5e7eb',
        ...(isMobile && {
          position: 'fixed',
          top: 0,
          bottom: 0,
          left: 0,
          zIndex: 50,
          overflowY: 'auto',
          transform: menuOpen ? 'translateX(0)' : 'translateX(-100%)',
          transition: 'transform 0.2s ease',
          visibility: menuOpen ? 'visible' : 'hidden',
        }),
      }}>
        {/* Logo */}
        <div style={{ padding: '22px 16px 18px', borderBottom: '1px solid #f3f4f6' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
            <ShieldCheck size={24} color="#1a2744" strokeWidth={2.2} />
            <div style={{ flex: 1 }}>
              <div style={{ color: '#111827', fontWeight: 700, fontSize: '17px', letterSpacing: '-0.3px' }}>Factur-X</div>
              <div style={{ color: '#6b7280', fontSize: '12px' }}>Conformité & Conversion</div>
            </div>
            {isMobile && (
              <button
                type="button"
                onClick={() => setMenuOpen(false)}
                aria-label="Fermer le menu"
                style={{ display: 'flex', background: 'none', border: 'none', cursor: 'pointer', padding: '4px', color: '#6b7280' }}
              >
                <X size={20} />
              </button>
            )}
          </div>
        </div>
	  	{/* User profile */}
		<div
		onClick={() => navigate('/profile')}
		style={{
			display: 'flex',
			alignItems: 'center',
			gap: '10px',
			padding: '14px 16px',
			borderBottom: '1px solid #f3f4f6',
			cursor: 'pointer',
		}}
		>
		<div
			style={{
			width: '36px',
			height: '36px',
			borderRadius: '50%',
			background: '#eff6ff',
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'center',
			flexShrink: 0,
			}}
		>
			<User size={19} color="#1a2744" />
		</div>

		<div style={{ minWidth: 0 }}>
			<div
			style={{
				color: '#1a1a2e',
				fontSize: '14px',
				fontWeight: 600,
				whiteSpace: 'nowrap',
				overflow: 'hidden',
				textOverflow: 'ellipsis',
			}}
			>
			{user?.firstName} {user?.lastName}
			</div>

			<div
			style={{
				color: '#6b7280',
				fontSize: '11.5px',
				whiteSpace: 'nowrap',
				overflow: 'hidden',
				textOverflow: 'ellipsis',
			}}
			>
			{user?.email}
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
      <main
		style={{
			flex: 1,
			minWidth: 0,
			overflow: 'auto',
			display: 'flex',
			flexDirection: 'column',
		}}
		>
		<div style={{ flex: 1 }}>
			{children}
		</div>

		<Footer />
	  </main>
    </div>
  );
}