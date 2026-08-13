import { useState, useEffect } from 'react';
import { checkHealth } from '../services/healthcheck';

export default function HealthCheck() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchHealth = async () => {
      try {
        const result = await checkHealth();
        setData(result);
        setError(null);
      } catch (err) {
        setError(err.message);
        setData(null);
      } finally {
        setLoading(false);
      }
    };

    fetchHealth();
  }, []);

  if (loading) return <div className="p-8">Loading...</div>;

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">Backend Health Check</h1>
      
      {error ? (
        <div className="bg-red-100 p-4 rounded text-red-800">
          ❌ Error: {error}
        </div>
      ) : (
        <div className="bg-green-100 p-4 rounded text-green-800">
          <div className="font-bold">✓ Backend is WORKING!</div>
          <pre className="mt-4 bg-white text-black p-4 rounded overflow-auto">
            {JSON.stringify(data, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
