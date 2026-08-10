import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Processing from './pages/Processing';
import ComplianceReport from './pages/ComplianceReport';
import Conversion from './pages/Conversion';
import Success from './pages/Success';
import XmlReader from './pages/XmlReader';
import UploadPage from './pages/UploadPage';
import Layout from './components/Layout';

function ProtectedRoute({ loggedIn, children }) {
  if (!loggedIn) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(false);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login onLogin={() => setLoggedIn(true)} />} />

        <Route path="/dashboard" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <Dashboard />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/traitement" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <Processing />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/rapport" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <ComplianceReport />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/conversion" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <Conversion />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/succes" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <Success />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/lecture-xml" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <XmlReader />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/verifier" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <UploadPage mode="verifier" />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/convertir" element={
          <ProtectedRoute loggedIn={loggedIn}>
            <Layout onLogout={() => setLoggedIn(false)}>
              <UploadPage mode="convertir" />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
