export const checkHealth = async () => {
    const response = await fetch('http://localhost:8080/api/healthcheck');

    if (!response.ok) {
        throw new Error(`Health check failed: ${response.status}`);
    }

    return await response.text();
};