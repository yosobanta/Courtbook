import { Router } from 'express';
import { AdminController } from '../controllers/adminController';
import { authenticate, authorize } from '../middleware/auth';

const router = Router();

// Protect ALL admin routes with JWT authentication & role-based access control
router.use(authenticate, authorize('ADMIN'));

router.get('/dashboard', AdminController.getDashboard);
router.get('/bookings', AdminController.getAllBookings);
router.post('/courts/:id/generate-slots', AdminController.generateSlots);
router.post('/slots/block', AdminController.blockSlots);
router.post('/slots/unblock', AdminController.unblockSlots);
router.post('/offers', AdminController.createOffer);
router.get('/users', AdminController.getAllUsers);
router.patch('/users/:id/status', AdminController.toggleUserStatus);

// Aggregation Pipeline Analytics endpoints
router.get('/analytics/revenue', AdminController.getRevenueAnalytics);
router.get('/analytics/popular-sports', AdminController.getPopularSportsAnalytics);
router.get('/analytics/court-utilization', AdminController.getCourtUtilizationAnalytics);

export default router;
