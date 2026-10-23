import Profile from './pages/Profile';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ComplianceReport from './pages/ComplianceReport';
import Conversion from './pages/Conversion';
import XmlReader from './pages/XmlReader';
import UploadPage from './pages/UploadPage';
import HealthCheck from './pages/HealthCheck';
import OrganizationsPage from './pages/OrganizationsPage';
import Users from './pages/Users';
import Layout from './components/Layout';
import Invitations from './pages/Invitations';
import OrganizationMembersPage from './pages/OrganizationMembersPage';
import NewInvitation from './pages/NewInvitation';
import InvitationCheck from './pages/InvitationCheck';
import PrivacyPolicy from './pages/PrivacyPolicy';
import Terms from './pages/Terms';
import LandingPage from './pages/LandingPage';
import DocumentDetail from './pages/DocumentDetail';
import Documents from './pages/Documents';
import InvoiceView from './pages/InvoiceView';
import ApiKeysPage from './pages/ApiKeysPage';
import { DocumentWebSocketProvider } from './context/DocumentWebSocketContext';

function Protected({ children }) {
  return (
    <ProtectedRoute>
      <DocumentWebSocketProvider>
        <Layout>{children}</Layout>
      </DocumentWebSocketProvider>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/healthcheck" element={<HealthCheck />} />
          <Route path="/users" element={<Users />} />
          <Route path="/privacy" element={<PrivacyPolicy />} />
          <Route path="/terms" element={<Terms />} />

          <Route path="/organisations/:id" element={<Protected><OrganizationMembersPage /></Protected>} />
          <Route path="/" element={<LandingPage />} />
          <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
          <Route path="/documents" element={<Protected><Documents /></Protected>} />
          <Route path="/documents/:id" element={<Protected><DocumentDetail /></Protected>} />
          <Route path="/invitations/new" element={<Protected><NewInvitation /></Protected>} />
          <Route path="/invitations" element={<Protected><Invitations /></Protected>} />
          <Route path="/invitations/:token" element={<InvitationCheck />} />
          <Route path="/documents/:id/invoice" element={<Protected><InvoiceView /></Protected>} />
          <Route path="/rapport" element={<Protected><ComplianceReport /></Protected>} />
          <Route path="/conversion" element={<Protected><Conversion /></Protected>} />
          <Route path="/profile" element={<Protected><Profile /></Protected>} />
          <Route path="/lecture-xml" element={<Protected><XmlReader /></Protected>} />
          <Route path="/verifier" element={<Protected><UploadPage mode="verifier" /></Protected>} />
          <Route path="/convertir" element={<Protected><UploadPage mode="convertir" /></Protected>} />
          <Route path="/organisations" element={<Protected><OrganizationsPage /></Protected>} />
          <Route path="/cles-api" element={<Protected><ApiKeysPage /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
