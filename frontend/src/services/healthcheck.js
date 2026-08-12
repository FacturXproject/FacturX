const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

export const checkHealth = async () => {
  try {
    const response = await fetch(`${API_URL}/healthcheck`);
    if (!response.ok) {
      throw new Error(`Health check failed: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Health check error:', error);
    throw error;
  }
};
