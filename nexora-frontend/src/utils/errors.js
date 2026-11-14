export function getErrorMessage(error) {
  const status = error?.response?.status;
  const data = error?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  if (data?.message) return data.message;
  if (data?.detail) return data.detail;
  if (status === 401) {
    return "Authentication failed (401). The request may be missing a valid Bearer token. Your page has been kept open; sign out and sign in again, then retry.";
  }
  if (status === 429) {
    return "The AI provider quota or rate limit has been reached. Wait for the provider reset or check your Gemini usage tier.";
  }
  if (status === 404) return "The requested resource was not found. Check the ID and confirm it belongs to your account.";
  if (status === 403) return "Access denied (403). Check the account permissions for this resource.";
  return error?.message || "Something went wrong. Please try again.";
}
