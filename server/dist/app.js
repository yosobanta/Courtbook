"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.app = void 0;
exports.startServer = startServer;
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const env_1 = require("./config/env");
const database_1 = require("./config/database");
const routes_1 = __importDefault(require("./routes"));
const errorHandler_1 = require("./middleware/errorHandler");
const app = (0, express_1.default)();
exports.app = app;
// Standard middleware
app.use((0, cors_1.default)());
app.use(express_1.default.json());
app.use(express_1.default.urlencoded({ extended: true }));
// Minimal development request logger
if (env_1.ENV.NODE_ENV !== 'production') {
    app.use((req, res, next) => {
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
        next();
    });
}
// API Routes
app.use('/api', routes_1.default);
// Fallback 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        status: 404,
        message: `API endpoint ${req.method} ${req.originalUrl} not found`,
    });
});
// Centralized error handling
app.use(errorHandler_1.errorHandler);
// Connect to MongoDB and start HTTP server
async function startServer() {
    await (0, database_1.connectDatabase)();
    const server = app.listen(env_1.ENV.PORT, () => {
        console.log(`====================================================`);
        console.log(`🚀 CourtBook API Server running on port ${env_1.ENV.PORT}`);
        console.log(`📡 Base URL: http://localhost:${env_1.ENV.PORT}/api`);
        console.log(`🏥 Health check: http://localhost:${env_1.ENV.PORT}/api/health`);
        console.log(`====================================================`);
    });
    return server;
}
if (process.env.NODE_ENV !== 'test') {
    startServer();
}
