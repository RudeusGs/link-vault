export interface AuthUser {
  id: string;
  username: string;
  email: string;
  displayName?: string | null;
  avatarUrl?: string | null;
  createdAt?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

export interface AuthResponse {
  accessToken?: string;
  token?: string;
  tokenType?: string;
  expiresAt?: string;
  user?: AuthUser;
}

export interface AvailabilityResponse {
  usernameAvailable?: boolean;
  emailAvailable?: boolean;
  usernameExists?: boolean;
  emailExists?: boolean;
}
