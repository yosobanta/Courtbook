"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const reviewController_1 = require("../controllers/reviewController");
const auth_1 = require("../middleware/auth");
const router = (0, express_1.Router)();
router.get('/:id/reviews', reviewController_1.ReviewController.getCourtReviews);
router.post('/:id/reviews', auth_1.authenticate, reviewController_1.ReviewController.addReview);
exports.default = router;
