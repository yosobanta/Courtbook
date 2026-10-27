"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BookingController = void 0;
const bookingService_1 = require("../services/bookingService");
const Booking_1 = require("../models/Booking");
const errors_1 = require("../utils/errors");
class BookingController {
    /**
     * Primary booking endpoint executing the ACID transaction.
     */
    static async createBooking(req, res, next) {
        try {
            const userId = req.user._id.toString();
            const { courtId, slotIds, offerCode, paymentMethod } = req.body;
            if (!courtId || !slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
                throw new errors_1.BadRequestError('courtId and an array of slotIds are required');
            }
            const booking = await bookingService_1.BookingService.createBooking({
                userId,
                courtId,
                slotIds,
                offerCode,
                paymentMethod,
            });
            // Populate court and venue information for rich response
            const populatedBooking = await Booking_1.Booking.findById(booking._id)
                .populate('courtId', 'name sport isIndoor surfaceType')
                .populate('venueId', 'name address city contact')
                .populate('slotIds', 'date startTime endTime price');
            res.status(201).json({
                success: true,
                message: 'Court booking confirmed successfully',
                data: populatedBooking,
            });
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * Retrieves bookings belonging to the currently logged in player.
     */
    static async getMyBookings(req, res, next) {
        try {
            const userId = req.user._id;
            const bookings = await Booking_1.Booking.find({ userId })
                .sort({ createdAt: -1 })
                .populate('courtId', 'name sport')
                .populate('venueId', 'name address city images')
                .populate('slotIds', 'date startTime endTime price');
            res.status(200).json({
                success: true,
                count: bookings.length,
                data: bookings,
            });
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * Retrieves single booking details.
     */
    static async getBookingById(req, res, next) {
        try {
            const { id } = req.params;
            const user = req.user;
            const booking = await Booking_1.Booking.findById(id)
                .populate('courtId')
                .populate('venueId')
                .populate('slotIds')
                .populate('offerId', 'code title description discountAmount');
            if (!booking) {
                throw new errors_1.NotFoundError('Booking not found');
            }
            // Authorization check: only booking owner or admin can view
            if (user.role !== 'ADMIN' && !booking.userId.equals(user._id)) {
                throw new errors_1.ForbiddenError('You are not authorized to view this booking');
            }
            res.status(200).json({ success: true, data: booking });
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * Cancels a booking and releases its claimed slots.
     */
    static async cancelBooking(req, res, next) {
        try {
            const { id } = req.params;
            const { reason } = req.body;
            const user = req.user;
            const booking = await bookingService_1.BookingService.cancelBooking(id, user._id.toString(), user.role === 'ADMIN', reason);
            res.status(200).json({
                success: true,
                message: 'Booking cancelled successfully. Associated slots have been released.',
                data: booking,
            });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.BookingController = BookingController;
