import Profile from './pages/Profile';
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
import Users from './pages/Users';
import Layout from './components/Layout';
import PrivacyPolicy from './pages/PrivacyPolicy';
import Terms from './pages/Terms';

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
         <Route path="/privacy" element={<PrivacyPolicy />} />
          <Route path="/terms" element={<Terms />} />

          <Route path="/" element={<Protected><Dashboard /></Protected>} />
          <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
          <Route path="/traitement" element={<Protected><Processing /></Protected>} />
          <Route path="/rapport" element={<Protected><ComplianceReport /></Protected>} />
          <Route path="/conversion" element={<Protected><Conversion /></Protected>} />
		  <Route path="/profile" element={<Protected><Profile /></Protected>} />
          <Route path="/succes" element={<Protected><Success /></Protected>} />
          <Route path="/lecture-xml" element={<Protected><XmlReader /></Protected>} />
          <Route path="/verifier" element={<Protected><UploadPage mode="verifier" /></Protected>} />
          <Route path="/convertir" element={<Protected><UploadPage mode="convertir" /></Protected>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
