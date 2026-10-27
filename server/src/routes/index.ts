import { Router } from 'express';
import authRoutes from './authRoutes';
import venueRoutes from './venueRoutes';
import courtRoutes from './courtRoutes';
import slotRoutes from './slotRoutes';
import bookingRoutes from './bookingRoutes';
import offerRoutes from './offerRoutes';
import reviewRoutes from './reviewRoutes';
import adminRoutes from './adminRoutes';

const router = Router();

router.use('/auth', authRoutes);
router.use('/venues', venueRoutes);
router.use('/courts', courtRoutes);
router.use('/slots', slotRoutes);
router.use('/bookings', bookingRoutes);
router.use('/offers', offerRoutes);
router.use('/reviews', reviewRoutes);
router.use('/admin', adminRoutes);

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

export default router;
