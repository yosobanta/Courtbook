"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.VenueController = void 0;
const Venue_1 = require("../models/Venue");
const Court_1 = require("../models/Court");
const Slot_1 = require("../models/Slot");
const errors_1 = require("../utils/errors");
const imageUtils_1 = require("../utils/imageUtils");
class VenueController {
    static async getVenues(req, res, next) {
        try {
            const { city, search } = req.query;
            const query = { isActive: true };
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
            const venues = await Venue_1.Venue.find(query).sort({ 'rating.average': -1, name: 1 });
            res.status(200).json({ success: true, count: venues.length, data: venues });
        }
        catch (error) {
            next(error);
        }
    }
    static async getVenueById(req, res, next) {
        try {
            const { id } = req.params;
            const venue = await Venue_1.Venue.findById(id);
            if (!venue) {
                throw new errors_1.NotFoundError('Venue not found');
            }
            const courts = await Court_1.Court.find({ venueId: venue._id, isActive: true });
            res.status(200).json({
                success: true,
                data: {
                    ...venue.toObject(),
                    courts,
                },
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async createVenue(req, res, next) {
        try {
            const { name, address, city, location, amenities, operatingHours, contact, images, sport, basePrice } = req.body;
            if (!name || !address || !city) {
                throw new errors_1.BadRequestError('Venue name, address, and city are required');
            }
            // Normalize images (e.g. resolve Google Photos / Google Drive share links to direct CDN URLs)
            const normalizedImages = [];
            if (images && Array.isArray(images)) {
                for (const img of images) {
                    if (img && typeof img === 'string') {
                        const resolved = await (0, imageUtils_1.normalizeImageUrl)(img);
                        if (resolved)
                            normalizedImages.push(resolved);
                    }
                }
            }
            const venue = new Venue_1.Venue({
                name,
                address,
                city,
                location: location || { type: 'Point', coordinates: [77.5946, 12.9716] },
                amenities: amenities || ['PARKING', 'DRINKING_WATER'],
                operatingHours: operatingHours || { open: '06:00', close: '23:00' },
                contact: contact || {},
                images: normalizedImages,
            });
            await venue.save();
            // Automatically create a main court so the turf is immediately visible and playable by players
            const courtSport = (sport || 'FOOTBALL').toUpperCase();
            const courtPrice = basePrice ? Number(basePrice) : 800;
            const defaultCourt = new Court_1.Court({
                venueId: venue._id,
                name: `${name} Main Turf`,
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
                await Slot_1.Slot.insertMany(slotsToInsert);
            }
            res.status(201).json({ success: true, data: venue });
        }
        catch (error) {
            next(error);
        }
    }
    static async updateVenue(req, res, next) {
        try {
            const { id } = req.params;
            const { name, description, address, city, amenities, operatingHours, contact, images, isActive } = req.body;
            const venue = await Venue_1.Venue.findById(id);
            if (!venue) {
                throw new errors_1.NotFoundError('Venue not found');
            }
            if (name !== undefined)
                venue.name = name;
            if (description !== undefined)
                venue.description = description;
            if (address !== undefined)
                venue.address = address;
            if (city !== undefined)
                venue.city = city;
            if (amenities !== undefined)
                venue.amenities = amenities;
            if (operatingHours !== undefined)
                venue.operatingHours = { ...venue.operatingHours, ...operatingHours };
            if (contact !== undefined)
                venue.contact = { ...venue.contact, ...contact };
            if (images !== undefined)
                venue.images = images;
            if (isActive !== undefined)
                venue.isActive = isActive;
            await venue.save();
            res.status(200).json({ success: true, message: 'Venue updated successfully', data: venue });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.VenueController = VenueController;
