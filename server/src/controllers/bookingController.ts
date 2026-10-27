import { Response, NextFunction } from 'express';
import { BookingService } from '../services/bookingService';
import { AuthRequest } from '../middleware/auth';
import { Booking } from '../models/Booking';
import { NotFoundError, ForbiddenError, BadRequestError } from '../utils/errors';

export class BookingController {
  /**
   * Primary booking endpoint executing the ACID transaction.
   */
  public static async createBooking(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!._id.toString();
      const { courtId, slotIds, offerCode, paymentMethod } = req.body;

      if (!courtId || !slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
        throw new BadRequestError('courtId and an array of slotIds are required');
      }

      const booking = await BookingService.createBooking({
        userId,
        courtId,
        slotIds,
        offerCode,
        paymentMethod,
      });

      // Populate court and venue information for rich response
      const populatedBooking = await Booking.findById(booking._id)
        .populate('courtId', 'name sport isIndoor surfaceType')
        .populate('venueId', 'name address city contact')
        .populate('slotIds', 'date startTime endTime price');

      res.status(201).json({
        success: true,
        message: 'Court booking confirmed successfully',
        data: populatedBooking,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * Retrieves bookings belonging to the currently logged in player.
   */
  public static async getMyBookings(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!._id;

      const bookings = await Booking.find({ userId })
        .sort({ createdAt: -1 })
        .populate('courtId', 'name sport')
        .populate('venueId', 'name address city images')
        .populate('slotIds', 'date startTime endTime price');

      res.status(200).json({
        success: true,
        count: bookings.length,
        data: bookings,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * Retrieves single booking details.
   */
  public static async getBookingById(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const user = req.user!;

      const booking = await Booking.findById(id)
        .populate('courtId')
        .populate('venueId')
        .populate('slotIds')
        .populate('offerId', 'code title description discountAmount');

      if (!booking) {
        throw new NotFoundError('Booking not found');
      }

      // Authorization check: only booking owner or admin can view
      if (user.role !== 'ADMIN' && !booking.userId.equals(user._id)) {
        throw new ForbiddenError('You are not authorized to view this booking');
      }

      res.status(200).json({ success: true, data: booking });
    } catch (error) {
      next(error);
    }
  }

  /**
   * Cancels a booking and releases its claimed slots.
   */
  public static async cancelBooking(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const { reason } = req.body;
      const user = req.user!;

      const booking = await BookingService.cancelBooking(
        id,
        user._id.toString(),
        user.role === 'ADMIN',
        reason
      );

      res.status(200).json({
        success: true,
        message: 'Booking cancelled successfully. Associated slots have been released.',
        data: booking,
      });
    } catch (error) {
      next(error);
    }
  }
}
