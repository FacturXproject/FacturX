import { useState, useEffect } from 'react';
import api from '../services/api';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const response = await api.get('/users');

        setUsers(response.data);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  return (
    <div className="p-8 max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold mb-2">Users</h1>
      <p className="text-gray-600 mb-6">
        Users retrieved from the FacturX backend
      </p>

      {loading && <p>Loading users...</p>}

      {error && (
        <div className="bg-red-100 p-4 rounded text-red-800 mb-6">
          Error: {error}
        </div>
      )}

      {!loading && !error && (
        <div className="bg-green-100 p-6 rounded text-green-800">
          <div className="font-bold mb-4">Backend connected</div>

          <div className="bg-white text-black p-4 rounded">
            <h2 className="font-bold mb-3">Users:</h2>

            <table className="w-full text-sm">
              <thead className="border-b">
                <tr>
                  <th className="text-left py-2">ID</th>
                  <th className="text-left py-2">Name</th>
                  <th className="text-left py-2">Email</th>
                  <th className="text-left py-2">Status</th>
                </tr>
              </thead>

              <tbody>
                {users.map((user) => (
                  <tr
                    key={user.id}
                    className="border-b hover:bg-gray-50"
                  >
                    <td className="py-2">{user.id}</td>

                    <td className="py-2">
                      {user.firstName} {user.lastName}
                    </td>

                    <td className="py-2">{user.email}</td>

                    <td className="py-2">
                      <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs">
                        {user.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
