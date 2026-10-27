import { Request, Response, NextFunction } from 'express';
import bcrypt from 'bcryptjs';
import { User, IUser } from '../models/User';
import { generateToken, AuthRequest } from '../middleware/auth';
import { BadRequestError, UnauthorizedError } from '../utils/errors';

export class AuthController {
  public static async register(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { name, email, phone, password, role } = req.body;

      if (!name || !email || !phone || !password) {
        throw new BadRequestError('Name, email, phone, and password are all required');
      }

      const existingUser = await User.findOne({ email: email.toLowerCase() });
      if (existingUser) {
        throw new BadRequestError('An account with this email address already exists');
      }

      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash(password, salt);

      const user = new User({
        name,
        email: email.toLowerCase(),
        phone,
        passwordHash,
        role: role === 'ADMIN' ? 'ADMIN' : 'PLAYER',
        isActive: true,
      });

      await user.save();
      const token = generateToken(user);

      res.status(201).json({
        success: true,
        message: 'Account registered successfully',
        data: {
          token,
          user: {
            id: user._id,
            name: user.name,
            email: user.email,
            phone: user.phone,
            role: user.role,
          },
        },
      });
    } catch (error) {
      next(error);
    }
  }

  public static async login(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { email, password } = req.body;

      if (!email || !password) {
        throw new BadRequestError('Email and password are required');
      }

      const user = await User.findOne({ email: email.toLowerCase() });
      if (!user) {
        throw new UnauthorizedError('Invalid email or password');
      }

      if (!user.isActive) {
        throw new UnauthorizedError('Account is disabled. Please contact support.');
      }

      const isMatch = await user.comparePassword(password);
      if (!isMatch) {
        throw new UnauthorizedError('Invalid email or password');
      }

      const token = generateToken(user);

      res.status(200).json({
        success: true,
        message: 'Logged in successfully',
        data: {
          token,
          user: {
            id: user._id,
            name: user.name,
            email: user.email,
            phone: user.phone,
            role: user.role,
          },
        },
      });
    } catch (error) {
      next(error);
    }
  }

  public static async me(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const user = req.user!;
      res.status(200).json({
        success: true,
        data: {
          user: {
            id: user._id,
            name: user.name,
            email: user.email,
            phone: user.phone,
            role: user.role,
            isActive: user.isActive,
            createdAt: user.createdAt,
          },
        },
      });
    } catch (error) {
      next(error);
    }
  }
}
