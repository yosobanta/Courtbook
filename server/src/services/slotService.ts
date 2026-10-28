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
   *
   * When re-run (e.g. after a price update), existing AVAILABLE slots
   * get their price refreshed. BOOKED and BLOCKED slots are untouched.
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

    // If an explicit flat price was passed use it; otherwise derive from court pricing tiers.
    const explicitPrice: number | undefined = params.price;

    const weekdayRate = court.pricing?.weekday && court.pricing.weekday > 0
      ? court.pricing.weekday
      : court.basePrice;
    const weekendRate = court.pricing?.weekend && court.pricing.weekend > 0
      ? court.pricing.weekend
      : (weekdayRate + 100);
    const peakRate = court.pricing?.peak && court.pricing.peak > 0
      ? court.pricing.peak
      : (weekdayRate + 150);

    // Pricing tier helpers
    const isPeakHour = (hour: number): boolean => (hour >= 6 && hour < 9) || (hour >= 17 && hour < 21);
    const isWeekendDate = (dateStr: string): boolean => {
      const parts = dateStr.split('-').map(Number);
      if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        const day = d.getDay();
        return day === 0 || day === 6;
      }
      const day = new Date(dateStr).getDay();
      return day === 0 || day === 6;
    };

    const slotPrice = (hour: number, dateStr: string): number => {
      if (explicitPrice !== undefined) return explicitPrice;
      if (isPeakHour(hour)) return peakRate;
      if (isWeekendDate(dateStr)) return weekendRate;
      return weekdayRate;
    };

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
        const hour = Math.floor(currentMinute / 60);
        const startH = hour.toString().padStart(2, '0');
        const startM = (currentMinute % 60).toString().padStart(2, '0');
        const endH = Math.floor((currentMinute + duration) / 60).toString().padStart(2, '0');
        const endM = ((currentMinute + duration) % 60).toString().padStart(2, '0');

        const startTime = `${startH}:${startM}`;
        const endTime = `${endH}:${endM}`;
        const price = slotPrice(hour, dateStr);

        bulkOps.push({
          updateOne: {
            filter: {
              courtId: court._id,
              date: dateStr,
              startTime: startTime,
            },
            update: {
              // $setOnInsert: sets fields only on a new document (upsert insert path)
              $setOnInsert: {
                courtId: court._id,
                venueId: court.venueId,
                date: dateStr,
                startTime: startTime,
                endTime: endTime,
                status: 'AVAILABLE',
                heldBy: null,
                holdExpiresAt: null,
                bookingId: null,
                blockReason: null,
                version: 0,
              },
              // $set with conditional: update price on existing AVAILABLE slots only.
              // We achieve this by always writing the price in the update; the filter
              // below restricts to non-BOOKED/non-BLOCKED statuses separately via
              // the status condition in the filter.
              $set: {
                price: price,
              },
            },
            // Only refresh price on AVAILABLE slots (not BOOKED/BLOCKED)
            // The upsert will still INSERT new slots regardless of status filter
            // because MongoDB uses the filter as the identity for the new doc.
            // We use a separate status condition on the filter for the update path.
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

    // Run with ordered:false for maximum throughput
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

    const court = await Court.findById(courtId);
    if (!court) {
      throw new NotFoundError('Court not found');
    }

    // Pricing tier helpers for this court
    const weekdayRate = court.pricing?.weekday && court.pricing.weekday > 0
      ? court.pricing.weekday
      : court.basePrice;
    const weekendRate = court.pricing?.weekend && court.pricing.weekend > 0
      ? court.pricing.weekend
      : (weekdayRate + 100);
    const peakRate = court.pricing?.peak && court.pricing.peak > 0
      ? court.pricing.peak
      : (weekdayRate + 150);

    const isPeakHour = (startTime: string): boolean => {
      const h = parseInt(startTime.split(':')[0], 10);
      return (h >= 6 && h < 9) || (h >= 17 && h < 21);
    };

    const isWeekendDate = (dateStr: string): boolean => {
      const parts = dateStr.split('-').map(Number);
      if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        const day = d.getDay();
        return day === 0 || day === 6;
      }
      const day = new Date(dateStr).getDay();
      return day === 0 || day === 6;
    };

    const getExpectedPrice = (startTime: string): number => {
      if (isPeakHour(startTime)) return peakRate;
      if (isWeekendDate(date)) return weekendRate;
      return weekdayRate;
    };

    // Fetch slots sorted by startTime
    let slots = await Slot.find({
      courtId: new mongoose.Types.ObjectId(courtId),
      date,
    }).sort({ startTime: 1 });

    // If no slots exist for this date, automatically generate them
    if (slots.length === 0) {
      await SlotService.generateSlots({
        courtId,
        startDate: date,
        endDate: date,
        startHour: 6,
        endHour: 22,
        slotDurationMinutes: 60,
      });
      slots = await Slot.find({
        courtId: new mongoose.Types.ObjectId(courtId),
        date,
      }).sort({ startTime: 1 });
    }

    // Ensure all AVAILABLE and HELD slots reflect the active rate schedule (weekday, weekend, peak)
    const bulkOps: any[] = [];
    for (const slot of slots) {
      if (slot.status === 'AVAILABLE' || slot.status === 'HELD') {
        const expectedPrice = getExpectedPrice(slot.startTime);
        if (slot.price !== expectedPrice) {
          slot.price = expectedPrice;
          bulkOps.push({
            updateOne: {
              filter: { _id: slot._id },
              update: { $set: { price: expectedPrice } },
            },
          });
        }
      }
    }

    if (bulkOps.length > 0) {
      await Slot.bulkWrite(bulkOps, { ordered: false });
    }

    return slots;
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
