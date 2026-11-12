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
    <div className={`flex min-h-screen bg-[#f8f9fa] ${isMobile ? 'flex-col' : 'flex-row'}`}>
      {/* Barre du haut (mobile / tablette) */}
      {isMobile && (
        <header className="sticky top-0 z-30 flex items-center gap-[10px] px-4 py-[10px] bg-white border-b border-[#e5e7eb]">
          <button
            type="button"
            onClick={() => setMenuOpen(true)}
            aria-label="Ouvrir le menu"
            className="flex bg-transparent border-0 cursor-pointer p-1 text-[#1a2744]"
          >
            <Menu size={22} />
          </button>
          <ShieldCheck size={22} color="#1a2744" strokeWidth={2.2} />
          <span className="text-[#111827] font-bold text-[16px]">Factur-X</span>
        </header>
      )}

      {/* Fond assombri derriere le tiroir */}
      {isMobile && menuOpen && (
        <div
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 bg-black/35 z-40"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`w-[240px] shrink-0 bg-white flex flex-col border-r border-[#e5e7eb] ${
          isMobile
            ? `fixed top-0 bottom-0 left-0 z-50 overflow-y-auto transition-transform duration-200 ease-[ease] ${
              menuOpen ? 'translate-x-0 visible' : '-translate-x-full invisible'
            }`
            : ''
        }`}
      >
        {/* Logo */}
        <div className="pt-[22px] px-4 pb-[18px] border-b border-[#f3f4f6]">
          <div className="flex items-center gap-[9px]">
            <ShieldCheck size={24} color="#1a2744" strokeWidth={2.2} />
            <div className="flex-1">
              <div className="text-[#111827] font-bold text-[17px] tracking-[-0.3px]">Factur-X</div>
              <div className="text-[#6b7280] text-[12px]">Conformité & Conversion</div>
            </div>
            {isMobile && (
              <button
                type="button"
                onClick={() => setMenuOpen(false)}
                aria-label="Fermer le menu"
                className="flex bg-transparent border-0 cursor-pointer p-1 text-[#6b7280]"
              >
                <X size={20} />
              </button>
            )}
          </div>
        </div>

        {/* User profile */}
        <div
          onClick={() => navigate('/profile')}
          className="flex items-center gap-[10px] px-4 py-[14px] border-b border-[#f3f4f6] cursor-pointer"
        >
          <div className="w-9 h-9 rounded-full bg-[#eff6ff] flex items-center justify-center shrink-0">
            <User size={19} color="#1a2744" />
          </div>

          <div className="min-w-0">
            <div className="text-[#1a1a2e] text-[14px] font-semibold truncate">
              {user?.firstName} {user?.lastName}
            </div>
            <div className="text-[#6b7280] text-[11.5px] truncate">
              {user?.email}
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 py-[10px]">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => `flex items-center gap-[11px] px-4 py-[11px] no-underline text-[15.5px] border-l-[3px] transition-all duration-150 ease-[ease] ${
                isActive
                  ? 'text-[#1a2744] bg-[#eff6ff] border-l-[#4a9eff] font-[550]'
                  : 'text-[#6b7280] bg-transparent border-l-transparent font-[450]'
              }`}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div className="py-3 border-t border-[#f3f4f6]">
          <button
            onClick={handleLogout}
            className="flex items-center gap-[11px] px-4 py-[11px] w-full bg-transparent border-0 cursor-pointer text-[#6b7280] text-[15.5px] text-left"
          >
            <LogOut size={18} />
            Déconnexion
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 min-w-0 overflow-auto flex flex-col">
        <div className="flex-1">
          {children}
        </div>

        <Footer />
      </main>
    </div>
  );
}
