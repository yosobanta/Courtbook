"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const adminController_1 = require("../controllers/adminController");
const auth_1 = require("../middleware/auth");
const router = (0, express_1.Router)();
// Protect ALL admin routes with JWT authentication & role-based access control
router.use(auth_1.authenticate, (0, auth_1.authorize)('ADMIN'));
router.get('/dashboard', adminController_1.AdminController.getDashboard);
router.get('/bookings', adminController_1.AdminController.getAllBookings);
router.post('/courts/:id/generate-slots', adminController_1.AdminController.generateSlots);
router.post('/slots/block', adminController_1.AdminController.blockSlots);
router.post('/slots/unblock', adminController_1.AdminController.unblockSlots);
router.post('/offers', adminController_1.AdminController.createOffer);
router.get('/users', adminController_1.AdminController.getAllUsers);
router.patch('/users/:id/status', adminController_1.AdminController.toggleUserStatus);
// Aggregation Pipeline Analytics endpoints
router.get('/analytics/revenue', adminController_1.AdminController.getRevenueAnalytics);
router.get('/analytics/popular-sports', adminController_1.AdminController.getPopularSportsAnalytics);
router.get('/analytics/court-utilization', adminController_1.AdminController.getCourtUtilizationAnalytics);
exports.default = router;
