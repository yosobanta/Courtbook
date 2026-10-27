"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateToken = generateToken;
exports.authenticate = authenticate;
exports.authorize = authorize;
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const env_1 = require("../config/env");
const User_1 = require("../models/User");
const errors_1 = require("../utils/errors");
function generateToken(user) {
    return jsonwebtoken_1.default.sign({
        userId: user._id.toString(),
        email: user.email,
        role: user.role,
    }, env_1.ENV.JWT_SECRET, { expiresIn: '7d' });
}
async function authenticate(req, res, next) {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            throw new errors_1.UnauthorizedError('Authentication token missing or invalid');
        }
        const token = authHeader.split(' ')[1];
        const decoded = jsonwebtoken_1.default.verify(token, env_1.ENV.JWT_SECRET);
        const user = await User_1.User.findById(decoded.userId);
        if (!user) {
            throw new errors_1.UnauthorizedError('User account not found');
        }
        if (!user.isActive) {
            throw new errors_1.ForbiddenError('Your account has been disabled by an administrator');
        }
        req.user = user;
        next();
    }
    catch (error) {
        if (error.name === 'JsonWebTokenError' || error.name === 'TokenExpiredError') {
            next(new errors_1.UnauthorizedError('Invalid or expired authentication token'));
        }
        else {
            next(error);
        }
    }
}
function authorize(...allowedRoles) {
    return (req, res, next) => {
        if (!req.user) {
            return next(new errors_1.UnauthorizedError('Authentication required'));
        }
        if (!allowedRoles.includes(req.user.role)) {
            return next(new errors_1.ForbiddenError('You do not have permission to perform this action'));
        }
        next();
    };
}
