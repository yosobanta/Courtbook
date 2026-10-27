import { Request, Response, NextFunction } from 'express';
import { Review } from '../models/Review';
import { Court } from '../models/Court';
import { Booking } from '../models/Booking';
import { AuthRequest } from '../middleware/auth';
import { BadRequestError, NotFoundError } from '../utils/errors';

export class ReviewController {
  public static async getCourtReviews(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const reviews = await Review.find({ courtId: id, isModerated: false })
        .sort({ createdAt: -1 })
        .limit(50);

      res.status(200).json({ success: true, count: reviews.length, data: reviews });
    } catch (error) {
      next(error);
    }
  }

  public static async addReview(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const courtId = req.params.id;
      const userId = req.user!._id;
      const userName = req.user!.name;
      const { bookingId, rating, comment } = req.body;

      if (!bookingId || !rating || !comment) {
        throw new BadRequestError('bookingId, rating, and comment are required');
      }

      if (rating < 1 || rating > 5) {
        throw new BadRequestError('Rating must be between 1 and 5');
      }

      const court = await Court.findById(courtId);
      if (!court) {
        throw new NotFoundError('Court not found');
      }

      // Verify that user actually booked this court
      const booking = await Booking.findOne({
        _id: bookingId,
        courtId: court._id,
        userId: userId,
        status: { $in: ['CONFIRMED', 'COMPLETED'] },
      });

      if (!booking) {
        throw new BadRequestError('You can only review courts you have booked and played on');
      }

      const existingReview = await Review.findOne({ userId, bookingId });
      if (existingReview) {
        throw new BadRequestError('You have already submitted a review for this booking');
      }

      const review = new Review({
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
    } catch (error) {
      next(error);
    }
  }
}
