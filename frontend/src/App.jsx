import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Processing from './pages/Processing';
import ComplianceReport from './pages/ComplianceReport';
import Conversion from './pages/Conversion';
import Success from './pages/Success';
import XmlReader from './pages/XmlReader';
import UploadPage from './pages/UploadPage';
import HealthCheck from './pages/HealthCheck';
import OrganizationsPage from './pages/OrganizationsPage';
import Users from './pages/Users';
import Layout from './components/Layout';
import Invitations from './pages/Invitations';
import OrganizationMembersPage from './pages/OrganizationMembersPage';


function Protected({ children }) {
  return (
    <ProtectedRoute>
      <Layout>{children}</Layout>
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
          <Route path="/organisations/:id" element={<Protected><OrganizationMembersPage /></Protected>} />
          <Route path="/" element={<Protected><Dashboard /></Protected>} />
          <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
          <Route path="/invitations" element={<Protected><Invitations /></Protected>} />
          <Route path="/traitement" element={<Protected><Processing /></Protected>} />
          <Route path="/rapport" element={<Protected><ComplianceReport /></Protected>} />
          <Route path="/conversion" element={<Protected><Conversion /></Protected>} />
          <Route path="/succes" element={<Protected><Success /></Protected>} />
          <Route path="/lecture-xml" element={<Protected><XmlReader /></Protected>} />
          <Route path="/verifier" element={<Protected><UploadPage mode="verifier" /></Protected>} />
          <Route path="/convertir" element={<Protected><UploadPage mode="convertir" /></Protected>} />
          <Route path="/organisations" element={<Protected><OrganizationsPage /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
