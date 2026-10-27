import { Request, Response, NextFunction } from 'express';
import mongoose from 'mongoose';
import { Court } from '../models/Court';
import { Venue } from '../models/Venue';
import { NotFoundError, BadRequestError } from '../utils/errors';

export class CourtController {
  public static async getCourts(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { sport, venueId, isIndoor, minPrice, maxPrice, search } = req.query;
      const query: any = { isActive: true };

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

      const court = new Court({
        venueId,
        name,
        sport: sport.toUpperCase(),
        isIndoor: isIndoor !== false,
        surfaceType: surfaceType || 'SYNTHETIC',
        capacity: capacity || 4,
        basePrice,
        pricing: pricing || { weekday: basePrice, weekend: basePrice + 100, peak: basePrice + 150 },
        images: images || [],
      });

      await court.save();
      res.status(201).json({ success: true, data: court });
    } catch (error) {
      next(error);
    }
  }
}
