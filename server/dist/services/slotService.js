"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SlotService = void 0;
const mongoose_1 = __importDefault(require("mongoose"));
const Slot_1 = require("../models/Slot");
const Court_1 = require("../models/Court");
const errors_1 = require("../utils/errors");
const env_1 = require("../config/env");
class SlotService {
    /**
     * Generates time slots for a court over a date range.
     * Utilizes MongoDB bulkWrite with upsert to demonstrate bulk operations
     * and maintain compound index idempotency.
     */
    static async generateSlots(params) {
        const court = await Court_1.Court.findById(params.courtId);
        if (!court) {
            throw new errors_1.NotFoundError('Court not found');
        }
        const startHour = params.startHour ?? 6;
        const endHour = params.endHour ?? 22;
        const duration = params.slotDurationMinutes ?? 60;
        const price = params.price ?? court.basePrice;
        const start = new Date(params.startDate);
        const end = new Date(params.endDate);
        if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) {
            throw new errors_1.BadRequestError('Invalid date range');
        }
        const bulkOps = [];
        // Loop through each day in the date range
        const current = new Date(start);
        while (current <= end) {
            const dateStr = current.toISOString().split('T')[0];
            let currentMinute = startHour * 60;
            const endMinute = endHour * 60;
            while (currentMinute + duration <= endMinute) {
                const startH = Math.floor(currentMinute / 60).toString().padStart(2, '0');
                const startM = (currentMinute % 60).toString().padStart(2, '0');
                const endH = Math.floor((currentMinute + duration) / 60).toString().padStart(2, '0');
                const endM = ((currentMinute + duration) % 60).toString().padStart(2, '0');
                const startTime = `${startH}:${startM}`;
                const endTime = `${endH}:${endM}`;
                bulkOps.push({
                    updateOne: {
                        filter: {
                            courtId: court._id,
                            date: dateStr,
                            startTime: startTime,
                        },
                        update: {
                            $setOnInsert: {
                                courtId: court._id,
                                venueId: court.venueId,
                                date: dateStr,
                                startTime: startTime,
                                endTime: endTime,
                                price: price,
                                status: 'AVAILABLE',
                                heldBy: null,
                                holdExpiresAt: null,
                                bookingId: null,
                                blockReason: null,
                                version: 0,
                            },
                        },
                        upsert: true,
                    },
                });
                currentMinute += duration;
            }
            current.setDate(current.getDate() + 1);
        }
        if (bulkOps.length === 0) {
            return { createdOrUpdated: 0 };
        }
        const result = await Slot_1.Slot.bulkWrite(bulkOps, { ordered: false });
        return { createdOrUpdated: result.upsertedCount + result.modifiedCount };
    }
    /**
     * Retrieves slots for a specific court and date.
     * Performs lazy expiration check on any expired HELD slots.
     */
    static async getCourtSlotsByDate(courtId, date) {
        const now = new Date();
        // Lazy cleanup: Release expired holds back to AVAILABLE
        await Slot_1.Slot.updateMany({
            courtId: new mongoose_1.default.Types.ObjectId(courtId),
            date,
            status: 'HELD',
            holdExpiresAt: { $lt: now },
        }, {
            $set: {
                status: 'AVAILABLE',
                heldBy: null,
                holdExpiresAt: null,
            },
        });
        // Fetch updated slots sorted by startTime
        return Slot_1.Slot.find({
            courtId: new mongoose_1.default.Types.ObjectId(courtId),
            date,
        }).sort({ startTime: 1 });
    }
    /**
     * Logical Slot Hold (Two-Phase reservation mechanism).
     * Atomically transitions selected slots from AVAILABLE (or expired HELD) to HELD.
     */
    static async holdSlots(userId, slotIds, holdMinutes = env_1.ENV.HOLD_DURATION_MINUTES) {
        if (!slotIds || slotIds.length === 0) {
            throw new errors_1.BadRequestError('At least one slot ID is required');
        }
        const now = new Date();
        const holdExpiresAt = new Date(now.getTime() + holdMinutes * 60 * 1000);
        const userObjectId = new mongoose_1.default.Types.ObjectId(userId);
        const slotObjectIds = slotIds.map((id) => new mongoose_1.default.Types.ObjectId(id));
        // Conditional atomic update on all requested slots
        const result = await Slot_1.Slot.updateMany({
            _id: { $in: slotObjectIds },
            $or: [
                { status: 'AVAILABLE' },
                { status: 'HELD', holdExpiresAt: { $lt: now } },
                { status: 'HELD', heldBy: userObjectId },
            ],
        }, {
            $set: {
                status: 'HELD',
                heldBy: userObjectId,
                holdExpiresAt: holdExpiresAt,
            },
            $inc: { version: 1 },
        });
        // All-or-nothing check: If not all requested slots could be held, roll back
        if (result.matchedCount !== slotIds.length) {
            // Revert any slots this user just acquired in this partial attempt
            await Slot_1.Slot.updateMany({
                _id: { $in: slotObjectIds },
                heldBy: userObjectId,
                holdExpiresAt: holdExpiresAt,
            }, {
                $set: {
                    status: 'AVAILABLE',
                    heldBy: null,
                    holdExpiresAt: null,
                },
            });
            throw new errors_1.ConflictError('One or more selected slots are currently unavailable or held by another player.');
        }
        const heldSlots = await Slot_1.Slot.find({ _id: { $in: slotObjectIds } });
        return { holdExpiresAt, heldSlots };
    }
    /**
     * Admin can block slots for maintenance, holidays, or private events.
     */
    static async blockSlots(slotIds, blockReason = 'Maintenance') {
        const slotObjectIds = slotIds.map((id) => new mongoose_1.default.Types.ObjectId(id));
        // Can only block slots that are not already BOOKED
        const result = await Slot_1.Slot.updateMany({
            _id: { $in: slotObjectIds },
            status: { $ne: 'BOOKED' },
        }, {
            $set: {
                status: 'BLOCKED',
                blockReason: blockReason,
                heldBy: null,
                holdExpiresAt: null,
            },
            $inc: { version: 1 },
        });
        return { modifiedCount: result.modifiedCount };
    }
    /**
     * Admin can unblock slots back to AVAILABLE.
     */
    static async unblockSlots(slotIds) {
        const slotObjectIds = slotIds.map((id) => new mongoose_1.default.Types.ObjectId(id));
        const result = await Slot_1.Slot.updateMany({
            _id: { $in: slotObjectIds },
            status: 'BLOCKED',
        }, {
            $set: {
                status: 'AVAILABLE',
                blockReason: null,
            },
            $inc: { version: 1 },
        });
        return { modifiedCount: result.modifiedCount };
    }
}
exports.SlotService = SlotService;
