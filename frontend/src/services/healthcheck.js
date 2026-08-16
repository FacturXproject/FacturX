const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
export const checkHealth = async () => {
    const response = await fetch(`${API_URL}/api/healthcheck`);

    if (!response.ok) {
        throw new Error(`Health check failed: ${response.status}`);
    }

    return await response.text();
};