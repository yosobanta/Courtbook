import { Router } from 'express';
import { VenueController } from '../controllers/venueController';
import { authenticate, authorize } from '../middleware/auth';

const router = Router();

router.get('/', VenueController.getVenues);
router.get('/:id', VenueController.getVenueById);
router.post('/', authenticate, authorize('ADMIN'), VenueController.createVenue);

export default router;
