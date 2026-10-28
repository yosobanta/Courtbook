import { Request, Response, NextFunction } from 'express';
import mongoose from 'mongoose';
import { Court } from '../models/Court';
import { Venue } from '../models/Venue';
import { Slot } from '../models/Slot';
import { NotFoundError, BadRequestError } from '../utils/errors';
import { normalizeImageUrl } from '../utils/imageUtils';

export class CourtController {
  public static async getCourts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { sport, venueId, isIndoor, minPrice, maxPrice, search, includeInactive } = req.query;
      const query: any = {};

      if (includeInactive !== 'true') {
        query.isActive = true;
      }

      if (sport) {
        query.sport = String(sport).toUpperCase();
      }

      if (venueId) {
        query.venueId = new mongoose.Types.ObjectId(String(venueId));
      }

      if (isIndoor !== undefined) {
        query.isIndoor = isIndoor === 'true';
      }

      if (minPrice || maxPrice) {
        query.basePrice = {};
        if (minPrice) query.basePrice.$gte = Number(minPrice);
        if (maxPrice) query.basePrice.$lte = Number(maxPrice);
      }

      if (search) {
        query.name = new RegExp(String(search), 'i');
      }

      const courts = await Court.find(query)
        .populate('venueId', 'name address city rating location')
        .sort({ basePrice: 1 });

      res.status(200).json({ success: true, count: courts.length, data: courts });
    } catch (error) {
      next(error);
    }
  }

  public static async getCourtById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const court = await Court.findById(id).populate('venueId');

      if (!court) {
        throw new NotFoundError('Court not found');
      }

      res.status(200).json({ success: true, data: court });
    } catch (error) {
      next(error);
    }
  }

  /**
   * MongoDB Geospatial Query demonstration:
   * Uses $geoNear aggregation stage on venues with 2dsphere index,
   * then joins corresponding courts.
   */
  public static async getNearbyCourts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const lng = parseFloat(req.query.lng as string) || 77.5946; // Bangalore default
      const lat = parseFloat(req.query.lat as string) || 12.9716;
      const maxDistanceMeters = parseInt(req.query.maxDistanceMeters as string, 10) || 15000; // 15km default

      // Run $geoNear on venues collection
      const nearbyVenues = await Venue.aggregate([
        {
          $geoNear: {
            near: { type: 'Point', coordinates: [lng, lat] },
            distanceField: 'distanceMeters',
            maxDistance: maxDistanceMeters,
            spherical: true,
            query: { isActive: true },
          },
        },
        {
          $lookup: {
            from: 'courts',
            localField: '_id',
            foreignField: 'venueId',
            as: 'courts',
          },
        },
        {
          $project: {
            name: 1,
            address: 1,
            city: 1,
            location: 1,
            rating: 1,
            amenities: 1,
            images: 1,
            distanceKm: { $round: [{ $divide: ['$distanceMeters', 1000] }, 1] },
            courts: 1,
          },
        },
        { $sort: { distanceMeters: 1 } },
      ]);

      res.status(200).json({ success: true, count: nearbyVenues.length, data: nearbyVenues });
    } catch (error) {
      next(error);
    }
  }

  public static async createCourt(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { venueId, name, sport, isIndoor, surfaceType, capacity, basePrice, pricing, images } = req.body;

      if (!venueId || !name || !sport || basePrice === undefined) {
        throw new BadRequestError('venueId, name, sport, and basePrice are required');
      }

      const venue = await Venue.findById(venueId);
      if (!venue) {
        throw new NotFoundError('Referenced venue not found');
      }

      // Normalize images
      let normalizedImages: string[] = [];
      if (images && Array.isArray(images)) {
        for (const img of images) {
          if (img && typeof img === 'string') {
            const resolved = await normalizeImageUrl(img);
            if (resolved) normalizedImages.push(resolved);
          }
        }
      }
      if (normalizedImages.length === 0 && venue.images && venue.images.length > 0) {
        normalizedImages = venue.images;
      }

      const finalPricing = pricing || { weekday: basePrice, weekend: basePrice + 100, peak: basePrice + 150 };

      const court = new Court({
        venueId,
        name,
        sport: sport.toUpperCase(),
        isIndoor: isIndoor !== false,
        surfaceType: surfaceType || 'SYNTHETIC',
        capacity: capacity || 4,
        basePrice,
        pricing: finalPricing,
        images: normalizedImages,
      });

      await court.save();

      // Automatically generate 7 days of bookable slots with correct per-tier pricing
      const isPeakHour = (hour: number): boolean => (hour >= 6 && hour < 9) || (hour >= 17 && hour < 21);
      const isWeekend = (dateStr: string): boolean => {
        const day = new Date(dateStr).getDay();
        return day === 0 || day === 6;
      };

      const now = new Date();
      const slotsToInsert = [];
      for (let day = 0; day < 7; day++) {
        const d = new Date(now);
        d.setDate(d.getDate() + day);
        const dateStr = d.toISOString().split('T')[0];
        for (let hour = 6; hour < 23; hour++) {
          const startTime = `${hour.toString().padStart(2, '0')}:00`;
          const endTime = `${(hour + 1).toString().padStart(2, '0')}:00`;
          let slotPrice: number;
          if (isPeakHour(hour)) {
            slotPrice = finalPricing.peak;
          } else if (isWeekend(dateStr)) {
            slotPrice = finalPricing.weekend;
          } else {
            slotPrice = finalPricing.weekday;
          }
          slotsToInsert.push({
            courtId: court._id,
            venueId: venue._id,
            date: dateStr,
            startTime,
            endTime,
            price: slotPrice,
            status: 'AVAILABLE',
          });
        }
      }
      if (slotsToInsert.length > 0) {
        await Slot.insertMany(slotsToInsert);
      }

      res.status(201).json({ success: true, data: court });
    } catch (error) {
      next(error);
    }
  }

  public static async updateCourt(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const { name, sport, isIndoor, surfaceType, capacity, basePrice, pricing, images, isActive } = req.body;

      const court = await Court.findById(id);
      if (!court) {
        throw new NotFoundError('Court not found');
      }

      const priceChanged = basePrice !== undefined || pricing !== undefined;

      if (name !== undefined) court.name = name;
      if (sport !== undefined) court.sport = sport.toUpperCase();
      if (isIndoor !== undefined) court.isIndoor = isIndoor;
      if (surfaceType !== undefined) court.surfaceType = surfaceType;
      if (capacity !== undefined) court.capacity = capacity;
      if (basePrice !== undefined) {
        court.basePrice = basePrice;
        if (!pricing) {
          // Keep pricing in sync if not explicitly overridden
          court.pricing = {
            weekday: basePrice,
            weekend: basePrice + 100,
            peak: basePrice + 150,
          };
        }
      }
      if (pricing !== undefined) court.pricing = { ...court.pricing, ...pricing };
      if (images !== undefined) court.images = images;
      if (isActive !== undefined) court.isActive = isActive;

      await court.save();

      // ── Propagate updated prices to all future AVAILABLE/HELD slots ─────
      // Runs only when basePrice or per-tier pricing actually changed.
      // BOOKED and BLOCKED slots are intentionally left untouched.
      if (priceChanged) {
        const today = new Date().toISOString().split('T')[0];
        const finalPricing = court.pricing;

        // PEAK hours: 06:00–09:00 and 17:00–21:00
        const isPeakHour = (startTime: string): boolean => {
          const h = parseInt(startTime.split(':')[0], 10);
          return (h >= 6 && h < 9) || (h >= 17 && h < 21);
        };

        // Weekend: Saturday (6) or Sunday (0)
        const isWeekend = (dateStr: string): boolean => {
          const parts = dateStr.split('-').map(Number);
          if (parts.length === 3) {
            const d = new Date(parts[0], parts[1] - 1, parts[2]);
            const day = d.getDay();
            return day === 0 || day === 6;
          }
          const day = new Date(dateStr).getDay();
          return day === 0 || day === 6;
        };

        const weekdayRate = court.pricing?.weekday && court.pricing.weekday > 0
          ? court.pricing.weekday
          : court.basePrice;
        const weekendRate = court.pricing?.weekend && court.pricing.weekend > 0
          ? court.pricing.weekend
          : (weekdayRate + 100);
        const peakRate = court.pricing?.peak && court.pricing.peak > 0
          ? court.pricing.peak
          : (weekdayRate + 150);

        // Fetch all future non-booked, non-blocked slots for this court
        const futureSlots = await Slot.find({
          courtId: court._id,
          date: { $gte: today },
          status: { $in: ['AVAILABLE', 'HELD'] },
        }).select('_id date startTime');

        if (futureSlots.length > 0) {
          const bulkOps = futureSlots.map((slot) => {
            let price: number;
            if (isPeakHour(slot.startTime)) {
              price = peakRate;
            } else if (isWeekend(slot.date)) {
              price = weekendRate;
            } else {
              price = weekdayRate;
            }

            return {
              updateOne: {
                filter: { _id: slot._id },
                update: { $set: { price } },
              },
            };
          });

          await Slot.bulkWrite(bulkOps, { ordered: false });
        }
      }
      // ────────────────────────────────────────────────────────────────────

      res.status(200).json({ success: true, message: 'Court updated successfully', data: court });
    } catch (error) {
      next(error);
    }
  }
}
