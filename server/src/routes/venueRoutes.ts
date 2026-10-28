import { Router } from 'express';
import { VenueController } from '../controllers/venueController';
import { authenticate, authorize } from '../middleware/auth';

const router = Router();

router.get('/', VenueController.getVenues);
router.get('/:id', VenueController.getVenueById);
router.post('/', authenticate, authorize('ADMIN'), VenueController.createVenue);
router.patch('/:id', authenticate, authorize('ADMIN'), VenueController.updateVenue);
router.put('/:id', authenticate, authorize('ADMIN'), VenueController.updateVenue);

export default router;
