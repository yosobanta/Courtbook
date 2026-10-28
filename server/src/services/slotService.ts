import mongoose from 'mongoose';
import { Slot, ISlot, SlotStatus } from '../models/Slot';
import { Court } from '../models/Court';
import { BadRequestError, NotFoundError, ConflictError } from '../utils/errors';
import { ENV } from '../config/env';

export class SlotService {
  /**
   * Generates time slots for a court over a date range.
   * Utilizes MongoDB bulkWrite with upsert to demonstrate bulk operations
   * and maintain compound index idempotency.
   */
  public static async generateSlots(params: {
    courtId: string;
    startDate: string; // YYYY-MM-DD
    endDate: string;   // YYYY-MM-DD
    startHour?: number; // default 6 (06:00)
    endHour?: number;   // default 22 (22:00)
    slotDurationMinutes?: number; // default 60
    price?: number;
  }): Promise<{ createdOrUpdated: number }> {
    const court = await Court.findById(params.courtId);
    if (!court) {
      throw new NotFoundError('Court not found');
    }

    const startHour = params.startHour ?? 6;
    const endHour = params.endHour ?? 22;
    const duration = params.slotDurationMinutes ?? 60;
    const price = params.price ?? court.basePrice;

    const start = new Date(params.startDate);
    const end = new Date(params.endDate);

    if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) {
      throw new BadRequestError('Invalid date range');
    }

    const bulkOps: any[] = [];

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

    const result = await Slot.bulkWrite(bulkOps, { ordered: false });
    return { createdOrUpdated: result.upsertedCount + result.modifiedCount };
  }

  /**
   * Releases all expired HELD slots across the system back to AVAILABLE.
   */
  public static async releaseExpiredHolds(): Promise<number> {
    const now = new Date();
    const result = await Slot.updateMany(
      {
        status: 'HELD',
        holdExpiresAt: { $lt: now },
      },
      {
        $set: {
          status: 'AVAILABLE',
          heldBy: null,
          holdExpiresAt: null,
        },
      }
    );
    return result.modifiedCount;
  }

  /**
   * Explicitly releases held slots for a given user back to AVAILABLE.
   */
  public static async releaseHold(
    userId: string,
    slotIds?: string[]
  ): Promise<{ releasedCount: number }> {
    const userObjectId = new mongoose.Types.ObjectId(userId);
    const filter: any = {
      status: 'HELD',
      heldBy: userObjectId,
    };

    if (slotIds && Array.isArray(slotIds) && slotIds.length > 0) {
      filter._id = { $in: slotIds.map((id) => new mongoose.Types.ObjectId(id)) };
    }

    const result = await Slot.updateMany(filter, {
      $set: {
        status: 'AVAILABLE',
        heldBy: null,
        holdExpiresAt: null,
      },
    });

    return { releasedCount: result.modifiedCount };
  }

  /**
   * Retrieves slots for a specific court and date.
   * Performs lazy expiration check on any expired HELD slots.
   */
  public static async getCourtSlotsByDate(courtId: string, date: string): Promise<ISlot[]> {
    const now = new Date();

    // Lazy cleanup: Release expired holds across the system or for this court/date
    await Slot.updateMany(
      {
        $or: [
          { status: 'HELD', holdExpiresAt: { $lt: now } },
          {
            courtId: new mongoose.Types.ObjectId(courtId),
            date,
            status: 'HELD',
            holdExpiresAt: { $lt: now },
          },
        ],
      },
      {
        $set: {
          status: 'AVAILABLE',
          heldBy: null,
          holdExpiresAt: null,
        },
      }
    );

    // Fetch updated slots sorted by startTime
    return Slot.find({
      courtId: new mongoose.Types.ObjectId(courtId),
      date,
    }).sort({ startTime: 1 });
  }

  /**
   * Logical Slot Hold (Two-Phase reservation mechanism).
   * Atomically transitions selected slots from AVAILABLE (or expired HELD) to HELD.
   */
  public static async holdSlots(
    userId: string,
    slotIds: string[],
    holdMinutes = ENV.HOLD_DURATION_MINUTES
  ): Promise<{ holdExpiresAt: Date; heldSlots: ISlot[] }> {
    if (!slotIds || slotIds.length === 0) {
      throw new BadRequestError('At least one slot ID is required');
    }

    const now = new Date();
    const holdExpiresAt = new Date(now.getTime() + holdMinutes * 60 * 1000);
    const userObjectId = new mongoose.Types.ObjectId(userId);
    const slotObjectIds = slotIds.map((id) => new mongoose.Types.ObjectId(id));

    // Conditional atomic update on all requested slots
    const result = await Slot.updateMany(
      {
        _id: { $in: slotObjectIds },
        $or: [
          { status: 'AVAILABLE' },
          { status: 'HELD', holdExpiresAt: { $lt: now } },
          { status: 'HELD', heldBy: userObjectId },
        ],
      },
      {
        $set: {
          status: 'HELD',
          heldBy: userObjectId,
          holdExpiresAt: holdExpiresAt,
        },
        $inc: { version: 1 },
      }
    );

    // All-or-nothing check: If not all requested slots could be held, roll back
    if (result.matchedCount !== slotIds.length) {
      // Revert any slots this user just acquired in this partial attempt
      await Slot.updateMany(
        {
          _id: { $in: slotObjectIds },
          heldBy: userObjectId,
          holdExpiresAt: holdExpiresAt,
        },
        {
          $set: {
            status: 'AVAILABLE',
            heldBy: null,
            holdExpiresAt: null,
          },
        }
      );
      throw new ConflictError('One or more selected slots are currently unavailable or held by another player.');
    }

    const heldSlots = await Slot.find({ _id: { $in: slotObjectIds } });
    return { holdExpiresAt, heldSlots };
  }

  /**
   * Admin can block slots for maintenance, holidays, or private events.
   */
  public static async blockSlots(
    slotIds: string[],
    blockReason = 'Maintenance'
  ): Promise<{ modifiedCount: number }> {
    const slotObjectIds = slotIds.map((id) => new mongoose.Types.ObjectId(id));

    // Can only block slots that are not already BOOKED
    const result = await Slot.updateMany(
      {
        _id: { $in: slotObjectIds },
        status: { $ne: 'BOOKED' },
      },
      {
        $set: {
          status: 'BLOCKED',
          blockReason: blockReason,
          heldBy: null,
          holdExpiresAt: null,
        },
        $inc: { version: 1 },
      }
    );

    return { modifiedCount: result.modifiedCount };
  }

  /**
   * Admin can unblock slots back to AVAILABLE.
   */
  public static async unblockSlots(slotIds: string[]): Promise<{ modifiedCount: number }> {
    const slotObjectIds = slotIds.map((id) => new mongoose.Types.ObjectId(id));

    const result = await Slot.updateMany(
      {
        _id: { $in: slotObjectIds },
        status: 'BLOCKED',
      },
      {
        $set: {
          status: 'AVAILABLE',
          blockReason: null,
        },
        $inc: { version: 1 },
      }
    );

    return { modifiedCount: result.modifiedCount };
  }
}
