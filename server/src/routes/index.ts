import { Router } from 'express';
import authRoutes from './authRoutes';
import venueRoutes from './venueRoutes';
import courtRoutes from './courtRoutes';
import slotRoutes from './slotRoutes';
import bookingRoutes from './bookingRoutes';
import offerRoutes from './offerRoutes';
import reviewRoutes from './reviewRoutes';
import adminRoutes from './adminRoutes';
import { saveBase64Image, normalizeImageUrl } from '../utils/imageUtils';

const router = Router();

router.use('/auth', authRoutes);
router.use('/venues', venueRoutes);
router.use('/courts', courtRoutes);
router.use('/slots', slotRoutes);
router.use('/bookings', bookingRoutes);
router.use('/offers', offerRoutes);
router.use('/reviews', reviewRoutes);
router.use('/admin', adminRoutes);

// Image upload and Google Photos / Drive URL resolution endpoint
router.post('/upload', async (req, res, next) => {
  try {
    const { image, url, filename } = req.body;

    if (image) {
      const savedPath = await saveBase64Image(image, filename || 'turf');
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
      const resolvedUrl = await normalizeImageUrl(url);
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
  } catch (error) {
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

export default router;
