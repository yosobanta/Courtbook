import { Router } from 'express';
import { ReviewController } from '../controllers/reviewController';
import { authenticate } from '../middleware/auth';

const router = Router();

router.get('/:id/reviews', ReviewController.getCourtReviews);
router.post('/:id/reviews', authenticate, ReviewController.addReview);

export default router;
