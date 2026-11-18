// What we SEND to POST /api/v1/login (matches LoginRequest.java)
export interface LoginRequest {
  email: string;
  password: string;
}

// What we SEND to POST /api/v1/users/register (matches UserDto.java, without id)
export interface RegisterRequest {
  fullname: string;
  email: string;
  password: string; // min 6 characters
}

// The user we keep in the browser (no password!)
export interface AuthUser {
  id: number;
  fullname: string;
  email: string;
}

// Backend UserDto also contains the BCrypt password hash – we drop it.
export type BackendUser = AuthUser & { password?: string };

// What we RECEIVE from POST /api/v1/login (matches LoginResponse.java)
export interface LoginResponse {
  token: string;
  type: string; // "Bearer"
  user: BackendUser;
}