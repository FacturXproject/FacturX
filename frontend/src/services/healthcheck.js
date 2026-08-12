export const checkHealth = async () => {
  try {
    const response = await fetch('http://localhost:5000/healthcheck');
    if (!response.ok) {
      throw new Error(`Health check failed: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Health check error:', error);
    throw error;
  }
};
