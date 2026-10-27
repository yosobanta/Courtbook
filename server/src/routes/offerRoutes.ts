import { Router } from 'express';
import { OfferController } from '../controllers/offerController';

const router = Router();

router.get('/active', OfferController.getActiveOffers);
router.post('/validate', OfferController.validateOffer);

export default router;
