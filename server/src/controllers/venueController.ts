import { Request, Response, NextFunction } from 'express';
import { Venue } from '../models/Venue';
import { Court } from '../models/Court';
import { NotFoundError, BadRequestError } from '../utils/errors';

export class VenueController {
  public static async getVenues(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { city, search } = req.query;
      const query: any = { isActive: true };

      if (city) {
        query.city = new RegExp(String(city), 'i');
      }

      if (search) {
        query.$or = [
          { name: new RegExp(String(search), 'i') },
          { address: new RegExp(String(search), 'i') },
          { city: new RegExp(String(search), 'i') },
        ];
      }

      const venues = await Venue.find(query).sort({ 'rating.average': -1, name: 1 });
      res.status(200).json({ success: true, count: venues.length, data: venues });
    } catch (error) {
      next(error);
    }
  }

  public static async getVenueById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const venue = await Venue.findById(id);

      if (!venue) {
        throw new NotFoundError('Venue not found');
      }

      const courts = await Court.find({ venueId: venue._id, isActive: true });

      res.status(200).json({
        success: true,
        data: {
          ...venue.toObject(),
          courts,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  public static async createVenue(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { name, address, city, location, amenities, operatingHours, contact, images } = req.body;

      if (!name || !address || !city) {
        throw new BadRequestError('Venue name, address, and city are required');
      }

      const venue = new Venue({
        name,
        address,
        city,
        location: location || { type: 'Point', coordinates: [77.5946, 12.9716] },
        amenities: amenities || ['PARKING', 'DRINKING_WATER'],
        operatingHours: operatingHours || { open: '06:00', close: '23:00' },
        contact: contact || {},
        images: images || [],
      });

      await venue.save();
      res.status(201).json({ success: true, data: venue });
    } catch (error) {
      next(error);
    }
  }
}
