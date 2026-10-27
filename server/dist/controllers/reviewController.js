"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ReviewController = void 0;
const Review_1 = require("../models/Review");
const Court_1 = require("../models/Court");
const Booking_1 = require("../models/Booking");
const errors_1 = require("../utils/errors");
class ReviewController {
    static async getCourtReviews(req, res, next) {
        try {
            const { id } = req.params;
            const reviews = await Review_1.Review.find({ courtId: id, isModerated: false })
                .sort({ createdAt: -1 })
                .limit(50);
            res.status(200).json({ success: true, count: reviews.length, data: reviews });
        }
        catch (error) {
            next(error);
        }
    }
    static async addReview(req, res, next) {
        try {
            const courtId = req.params.id;
            const userId = req.user._id;
            const userName = req.user.name;
            const { bookingId, rating, comment } = req.body;
            if (!bookingId || !rating || !comment) {
                throw new errors_1.BadRequestError('bookingId, rating, and comment are required');
            }
            if (rating < 1 || rating > 5) {
                throw new errors_1.BadRequestError('Rating must be between 1 and 5');
            }
            const court = await Court_1.Court.findById(courtId);
            if (!court) {
                throw new errors_1.NotFoundError('Court not found');
            }
            // Verify that user actually booked this court
            const booking = await Booking_1.Booking.findOne({
                _id: bookingId,
                courtId: court._id,
                userId: userId,
                status: { $in: ['CONFIRMED', 'COMPLETED'] },
            });
            if (!booking) {
                throw new errors_1.BadRequestError('You can only review courts you have booked and played on');
            }
            const existingReview = await Review_1.Review.findOne({ userId, bookingId });
            if (existingReview) {
                throw new errors_1.BadRequestError('You have already submitted a review for this booking');
            }
            const review = new Review_1.Review({
                userId,
                userName,
                courtId: court._id,
                venueId: court.venueId,
                bookingId: booking._id,
                rating,
                comment,
            });
            await review.save();
            res.status(201).json({
                success: true,
                message: 'Review posted successfully',
                data: review,
            });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.ReviewController = ReviewController;
