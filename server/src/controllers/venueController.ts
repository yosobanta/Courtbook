import { Request, Response, NextFunction } from 'express';
import { Venue } from '../models/Venue';
import { Court } from '../models/Court';
import { Slot } from '../models/Slot';
import { NotFoundError, BadRequestError } from '../utils/errors';
import { normalizeImageUrl } from '../utils/imageUtils';

export class VenueController {
  public static async getVenues(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { city, search, includeInactive } = req.query;
      const query: any = {};

      if (includeInactive !== 'true') {
        query.isActive = true;
      }

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
      const { includeInactive } = req.query;
      const venue = await Venue.findById(id);

      if (!venue) {
        throw new NotFoundError('Venue not found');
      }

      const courtQuery: any = { venueId: venue._id };
      if (includeInactive !== 'true') {
        courtQuery.isActive = true;
      }
      const courts = await Court.find(courtQuery);

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
      const { name, address, city, location, amenities, operatingHours, contact, images, sport, basePrice } = req.body;

      if (!name || !address || !city) {
        throw new BadRequestError('Venue name, address, and city are required');
      }

      // Normalize images (e.g. resolve Google Photos / Google Drive share links to direct CDN URLs)
      const normalizedImages: string[] = [];
      if (images && Array.isArray(images)) {
        for (const img of images) {
          if (img && typeof img === 'string') {
            const resolved = await normalizeImageUrl(img);
            if (resolved) normalizedImages.push(resolved);
          }
        }
      }

      const trimmedName = String(name).trim();
      const venue = new Venue({
        name: trimmedName,
        address: String(address).trim(),
        city: String(city).trim(),
        location: location || { type: 'Point', coordinates: [77.5946, 12.9716] },
        amenities: amenities || ['PARKING', 'DRINKING_WATER'],
        operatingHours: operatingHours || { open: '06:00', close: '23:00' },
        contact: contact || {},
        images: normalizedImages,
      });

      await venue.save();

      // Automatically create a main court with the exact entered name so the turf is immediately visible and playable
      const courtSport = (sport || 'FOOTBALL').toUpperCase();
      const courtPrice = basePrice ? Number(basePrice) : 800;
      const defaultCourt = new Court({
        venueId: venue._id,
        name: trimmedName,
        sport: courtSport,
        isIndoor: false,
        surfaceType: 'TURF',
        capacity: 10,
        basePrice: courtPrice,
        pricing: {
          weekday: courtPrice,
          weekend: courtPrice + 100,
          peak: courtPrice + 200,
        },
        images: normalizedImages,
        isActive: true,
      });
      await defaultCourt.save();

      // Generate 7 days of bookable slots automatically
      const now = new Date();
      const slotsToInsert = [];
      for (let day = 0; day < 7; day++) {
        const d = new Date(now);
        d.setDate(d.getDate() + day);
        const dateStr = d.toISOString().split('T')[0];
        for (let hour = 6; hour < 23; hour++) {
          const startTime = `${hour.toString().padStart(2, '0')}:00`;
          const endTime = `${(hour + 1).toString().padStart(2, '0')}:00`;
          slotsToInsert.push({
            courtId: defaultCourt._id,
            venueId: venue._id,
            date: dateStr,
            startTime,
            endTime,
            price: courtPrice,
            status: 'AVAILABLE',
          });
        }
      }
      if (slotsToInsert.length > 0) {
        await Slot.insertMany(slotsToInsert);
      }

      res.status(201).json({ success: true, data: venue });
    } catch (error) {
      next(error);
    }
  }

  public static async updateVenue(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const { name, description, address, city, amenities, operatingHours, contact, images, isActive } = req.body;

      const venue = await Venue.findById(id);
      if (!venue) {
        throw new NotFoundError('Venue not found');
      }

      if (name !== undefined) venue.name = name;
      if (description !== undefined) venue.description = description;
      if (address !== undefined) venue.address = address;
      if (city !== undefined) venue.city = city;
      if (amenities !== undefined) venue.amenities = amenities;
      if (operatingHours !== undefined) venue.operatingHours = { ...venue.operatingHours, ...operatingHours };
      if (contact !== undefined) venue.contact = { ...venue.contact, ...contact };
      if (images !== undefined) venue.images = images;
      if (isActive !== undefined) venue.isActive = isActive;

      await venue.save();
      res.status(200).json({ success: true, message: 'Venue updated successfully', data: venue });
    } catch (error) {
      next(error);
    }
  }
}
