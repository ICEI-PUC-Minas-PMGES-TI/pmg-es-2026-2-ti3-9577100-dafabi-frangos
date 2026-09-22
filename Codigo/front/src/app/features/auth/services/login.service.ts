import { Injectable } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginCredentials } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class LoginService {
  constructor(private readonly auth: AuthService) {}
  login(credentials: LoginCredentials) { return this.auth.login(credentials.identifier, credentials.password); }
}
