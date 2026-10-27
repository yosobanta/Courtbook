"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthController = void 0;
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const User_1 = require("../models/User");
const auth_1 = require("../middleware/auth");
const errors_1 = require("../utils/errors");
class AuthController {
    static async register(req, res, next) {
        try {
            const { name, email, phone, password, role } = req.body;
            if (!name || !email || !phone || !password) {
                throw new errors_1.BadRequestError('Name, email, phone, and password are all required');
            }
            const existingUser = await User_1.User.findOne({ email: email.toLowerCase() });
            if (existingUser) {
                throw new errors_1.BadRequestError('An account with this email address already exists');
            }
            const salt = await bcryptjs_1.default.genSalt(10);
            const passwordHash = await bcryptjs_1.default.hash(password, salt);
            const user = new User_1.User({
                name,
                email: email.toLowerCase(),
                phone,
                passwordHash,
                role: role === 'ADMIN' ? 'ADMIN' : 'PLAYER',
                isActive: true,
            });
            await user.save();
            const token = (0, auth_1.generateToken)(user);
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
        }
        catch (error) {
            next(error);
        }
    }
    static async login(req, res, next) {
        try {
            const { email, password } = req.body;
            if (!email || !password) {
                throw new errors_1.BadRequestError('Email and password are required');
            }
            const user = await User_1.User.findOne({ email: email.toLowerCase() });
            if (!user) {
                throw new errors_1.UnauthorizedError('Invalid email or password');
            }
            if (!user.isActive) {
                throw new errors_1.UnauthorizedError('Account is disabled. Please contact support.');
            }
            const isMatch = await user.comparePassword(password);
            if (!isMatch) {
                throw new errors_1.UnauthorizedError('Invalid email or password');
            }
            const token = (0, auth_1.generateToken)(user);
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
        }
        catch (error) {
            next(error);
        }
    }
    static async me(req, res, next) {
        try {
            const user = req.user;
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
        }
        catch (error) {
            next(error);
        }
    }
}
exports.AuthController = AuthController;
