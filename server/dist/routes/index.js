"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const authRoutes_1 = __importDefault(require("./authRoutes"));
const venueRoutes_1 = __importDefault(require("./venueRoutes"));
const courtRoutes_1 = __importDefault(require("./courtRoutes"));
const slotRoutes_1 = __importDefault(require("./slotRoutes"));
const bookingRoutes_1 = __importDefault(require("./bookingRoutes"));
const offerRoutes_1 = __importDefault(require("./offerRoutes"));
const reviewRoutes_1 = __importDefault(require("./reviewRoutes"));
const adminRoutes_1 = __importDefault(require("./adminRoutes"));
const router = (0, express_1.Router)();
router.use('/auth', authRoutes_1.default);
router.use('/venues', venueRoutes_1.default);
router.use('/courts', courtRoutes_1.default);
router.use('/slots', slotRoutes_1.default);
router.use('/bookings', bookingRoutes_1.default);
router.use('/offers', offerRoutes_1.default);
router.use('/reviews', reviewRoutes_1.default);
router.use('/admin', adminRoutes_1.default);
// API root index
router.get('/', (req, res) => {
    res.status(200).json({
        status: 'OK',
        message: 'CourtBook API is live and operational',
        version: '1.0.0',
        endpoints: {
            health: '/api/health',
            auth: '/api/auth',
            venues: '/api/venues',
            courts: '/api/courts',
            slots: '/api/slots',
            bookings: '/api/bookings',
            admin: '/api/admin',
        },
    });
});
// Health check endpoint
router.get('/health', (req, res) => {
    res.status(200).json({
        status: 'OK',
        timestamp: new Date(),
        service: 'CourtBook API',
    });
});
exports.default = router;
