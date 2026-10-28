import { Router } from 'express';
import { CourtController } from '../controllers/courtController';
import { SlotController } from '../controllers/slotController';
import { authenticate, authorize } from '../middleware/auth';

const router = Router();

router.get('/nearby', CourtController.getNearbyCourts);
router.get('/', CourtController.getCourts);
router.get('/:id', CourtController.getCourtById);
router.get('/:id/slots', SlotController.getCourtSlots);
router.post('/', authenticate, authorize('ADMIN'), CourtController.createCourt);
router.patch('/:id', authenticate, authorize('ADMIN'), CourtController.updateCourt);
router.put('/:id', authenticate, authorize('ADMIN'), CourtController.updateCourt);

export default router;
