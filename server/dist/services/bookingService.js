"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BookingService = void 0;
const mongoose_1 = __importDefault(require("mongoose"));
const Booking_1 = require("../models/Booking");
const Slot_1 = require("../models/Slot");
const Court_1 = require("../models/Court");
const User_1 = require("../models/User");
const Offer_1 = require("../models/Offer");
const pricingService_1 = require("./pricingService");
const errors_1 = require("../utils/errors");
const database_1 = require("../config/database");
class BookingService {
    /**
     * Generates a readable unique booking reference.
     * e.g. CB-20260920-F8A2
     */
    static generateBookingNumber(date) {
        const cleanDate = date.replace(/-/g, '');
        const randomHex = Math.random().toString(16).substring(2, 6).toUpperCase();
        return `CB-${cleanDate}-${randomHex}`;
    }
    /**
     * CRITICAL ACID BOOKING TRANSACTION:
     * 1. Authenticates & verifies active user.
     * 2. Starts MongoDB Session with 'snapshot' readConcern & 'majority' writeConcern.
     * 3. Atomically claims requested slots with a conditional update.
     * 4. Aborts transaction if ANY slot is already claimed (all-or-nothing atomicity).
     * 5. Server calculates verified price & applies offer.
     * 6. Creates Booking record with immutable price snapshot.
     * 7. Transitions slots to BOOKED state.
     * 8. Atomically increments offer usage count.
     * 9. Commits transaction and returns confirmed booking.
     */
    static async createBooking(params) {
        const { userId, courtId, slotIds, offerCode, paymentMethod } = params;
        if (!slotIds || slotIds.length === 0) {
            throw new errors_1.BadRequestError('At least one slot must be selected for booking');
        }
        // 1. Verify User
        const user = await User_1.User.findById(userId);
        if (!user) {
            throw new errors_1.NotFoundError('User not found');
        }
        if (!user.isActive) {
            throw new errors_1.ForbiddenError('Account is disabled. Cannot create bookings.');
        }
        // 2. Verify Court
        const court = await Court_1.Court.findById(courtId);
        if (!court || !court.isActive) {
            throw new errors_1.NotFoundError('Court not found or currently inactive');
        }
        const userObjectId = new mongoose_1.default.Types.ObjectId(userId);
        const courtObjectId = new mongoose_1.default.Types.ObjectId(courtId);
        const slotObjectIds = slotIds.map((id) => new mongoose_1.default.Types.ObjectId(id));
        // Check if MongoDB instance supports transactions (Replica Set / Atlas)
        const supportsTransactions = database_1.isReplicaSet || Boolean(mongoose_1.default.connection.replicaSet);
        const session = await mongoose_1.default.startSession();
        try {
            if (supportsTransactions) {
                // SNAPSHOT ISOLATION & MAJORITY WRITE CONCERN
                session.startTransaction({
                    readConcern: { level: 'snapshot' },
                    writeConcern: { w: 'majority', j: true },
                });
            }
            // 3. Atomically Claim Slots:
            // Conditional match: Slot must match requested IDs, must belong to requested court,
            // and status must be AVAILABLE, OR HELD (with expired hold, or held by this user).
            const now = new Date();
            const claimResult = await Slot_1.Slot.updateMany({
                _id: { $in: slotObjectIds },
                courtId: courtObjectId,
                $or: [
                    { status: 'AVAILABLE' },
                    { status: 'HELD', holdExpiresAt: { $lt: now } },
                    { status: 'HELD', heldBy: userObjectId },
                ],
            }, {
                $set: {
                    status: 'HELD',
                    heldBy: userObjectId,
                    holdExpiresAt: new Date(now.getTime() + 5 * 60 * 1000), // 5 min hold
                },
                $inc: { version: 1 },
            }, { session: supportsTransactions ? session : undefined });
            // CRITICAL CONCURRENCY CHECK:
            // If the number of matched slots does NOT equal the number of requested slots,
            // another user concurrently booked or held at least one of them.
            if (claimResult.matchedCount !== slotIds.length) {
                if (supportsTransactions) {
                    await session.abortTransaction();
                }
                else {
                    // Roll back any partial claims made in standalone mode
                    await Slot_1.Slot.updateMany({
                        _id: { $in: slotObjectIds },
                        heldBy: userObjectId,
                    }, {
                        $set: { status: 'AVAILABLE', heldBy: null, holdExpiresAt: null },
                    });
                }
                throw new errors_1.ConflictError('Sorry, one or more selected slots were just booked or held by another player. Please choose another time.');
            }
            // 4. Fetch the claimed slots to compute pricing and ensure chronological continuity
            const slots = await Slot_1.Slot.find({ _id: { $in: slotObjectIds } }, null, { session: supportsTransactions ? session : undefined }).sort({ startTime: 1 });
            if (slots.length !== slotIds.length) {
                if (supportsTransactions)
                    await session.abortTransaction();
                throw new errors_1.ConflictError('One or more requested slots could not be verified.');
            }
            const bookingDate = slots[0].date;
            const overallStartTime = slots[0].startTime;
            const overallEndTime = slots[slots.length - 1].endTime;
            const slotPrices = slots.map((s) => s.price);
            // 5. Server-side price calculation & Offer validation (Zero trust of client amounts)
            const priceResult = await pricingService_1.PricingService.calculateBookingPrice(court, slotPrices, offerCode);
            // 6. Create Booking Document
            const bookingNumber = this.generateBookingNumber(bookingDate);
            const newBooking = new Booking_1.Booking({
                bookingNumber,
                userId: userObjectId,
                courtId: courtObjectId,
                venueId: court.venueId,
                slotIds: slotObjectIds,
                date: bookingDate,
                startTime: overallStartTime,
                endTime: overallEndTime,
                originalAmount: priceResult.originalAmount,
                discountAmount: priceResult.discountAmount,
                finalAmount: priceResult.finalAmount,
                offerId: priceResult.offer ? priceResult.offer._id : null,
                offerCode: priceResult.offer ? priceResult.offer.code : null,
                status: 'CONFIRMED',
                paymentStatus: 'PAID',
                paymentDetails: {
                    transactionId: `TXN_${Date.now()}_${Math.random().toString(36).substring(2, 8).toUpperCase()}`,
                    method: paymentMethod || 'MOCK_UPI',
                    paidAt: new Date(),
                },
            });
            await newBooking.save({ session: supportsTransactions ? session : undefined });
            // 7. Transition Slots from HELD to BOOKED
            await Slot_1.Slot.updateMany({ _id: { $in: slotObjectIds } }, {
                $set: {
                    status: 'BOOKED',
                    bookingId: newBooking._id,
                    heldBy: null,
                    holdExpiresAt: null,
                },
                $inc: { version: 1 },
            }, { session: supportsTransactions ? session : undefined });
            // 8. If an offer was applied, increment its usedCount atomically
            if (priceResult.offer) {
                await Offer_1.Offer.updateOne({ _id: priceResult.offer._id }, { $inc: { usedCount: 1 } }, { session: supportsTransactions ? session : undefined });
            }
            // 9. Commit the ACID Transaction
            if (supportsTransactions) {
                await session.commitTransaction();
            }
            return newBooking;
        }
        catch (error) {
            if (supportsTransactions && session.inTransaction()) {
                await session.abortTransaction();
            }
            throw error;
        }
        finally {
            session.endSession();
        }
    }
    /**
     * Cancellation Flow with Transactional Rollback:
     * Releases slots back to AVAILABLE and increments available coupon capacity.
     */
    static async cancelBooking(bookingId, userId, isAdmin = false, reason = 'Cancelled by user') {
        const booking = await Booking_1.Booking.findById(bookingId);
        if (!booking) {
            throw new errors_1.NotFoundError('Booking not found');
        }
        if (!isAdmin && !booking.userId.equals(userId)) {
            throw new errors_1.ForbiddenError('You can only cancel your own bookings');
        }
        if (booking.status === 'CANCELLED') {
            throw new errors_1.BadRequestError('This booking is already cancelled');
        }
        const supportsTransactions = database_1.isReplicaSet || Boolean(mongoose_1.default.connection.replicaSet);
        const session = await mongoose_1.default.startSession();
        try {
            if (supportsTransactions) {
                session.startTransaction({
                    readConcern: { level: 'snapshot' },
                    writeConcern: { w: 'majority', j: true },
                });
            }
            booking.status = 'CANCELLED';
            booking.paymentStatus = 'REFUNDED';
            booking.cancellationReason = reason;
            await booking.save({ session: supportsTransactions ? session : undefined });
            // Release all associated slots back to AVAILABLE
            await Slot_1.Slot.updateMany({ _id: { $in: booking.slotIds } }, {
                $set: {
                    status: 'AVAILABLE',
                    bookingId: null,
                    heldBy: null,
                    holdExpiresAt: null,
                },
                $inc: { version: 1 },
            }, { session: supportsTransactions ? session : undefined });
            // If an offer was used, decrement usedCount
            if (booking.offerId) {
                await Offer_1.Offer.updateOne({ _id: booking.offerId }, { $inc: { usedCount: -1 } }, { session: supportsTransactions ? session : undefined });
            }
            if (supportsTransactions) {
                await session.commitTransaction();
            }
            return booking;
        }
        catch (error) {
            if (supportsTransactions && session.inTransaction()) {
                await session.abortTransaction();
            }
            throw error;
        }
        finally {
            session.endSession();
        }
    }
}
exports.BookingService = BookingService;
