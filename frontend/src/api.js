export async function api(path, method = 'GET', body, signal) {
  const response = await fetch('/api' + path, {
    method, credentials: 'same-origin', signal,
    headers: { 'X-Contact-Book': '1', ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  if (!response.ok) {
    const result = await response.json().catch(() => ({}));
    const error = new Error(result.message || 'The server could not complete the request.');
    error.status = response.status;
    if (response.status === 401 && path !== '/account/login') window.dispatchEvent(new Event('session-ended'));
    throw error;
  }
  return response.status === 204 ? null : response.json();
}
