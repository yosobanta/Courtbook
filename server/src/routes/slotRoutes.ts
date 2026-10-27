import { Router } from 'express';
import { SlotController } from '../controllers/slotController';
import { authenticate } from '../middleware/auth';

const router = Router();

// Slot hold reservation endpoint (two-phase locking)
router.post('/hold', authenticate, SlotController.holdSlots);

export default router;
