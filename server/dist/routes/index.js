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
const imageUtils_1 = require("../utils/imageUtils");
const router = (0, express_1.Router)();
router.use('/auth', authRoutes_1.default);
router.use('/venues', venueRoutes_1.default);
router.use('/courts', courtRoutes_1.default);
router.use('/slots', slotRoutes_1.default);
router.use('/bookings', bookingRoutes_1.default);
router.use('/offers', offerRoutes_1.default);
router.use('/reviews', reviewRoutes_1.default);
router.use('/admin', adminRoutes_1.default);
// Image upload and Google Photos / Drive URL resolution endpoint
router.post('/upload', async (req, res, next) => {
    try {
        const { image, url, filename } = req.body;
        if (image) {
            const savedPath = await (0, imageUtils_1.saveBase64Image)(image, filename || 'turf');
            const host = req.get('host') || 'localhost:5000';
            const fullUrl = `${req.protocol}://${host}${savedPath}`;
            return res.status(200).json({
                success: true,
                data: {
                    url: fullUrl,
                    path: savedPath,
                },
            });
        }
        if (url) {
            const resolvedUrl = await (0, imageUtils_1.normalizeImageUrl)(url);
            return res.status(200).json({
                success: true,
                data: {
                    url: resolvedUrl,
                },
            });
        }
        return res.status(400).json({
            success: false,
            message: 'Either base64 "image" or image "url" must be provided',
        });
    }
    catch (error) {
        next(error);
    }
});
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
