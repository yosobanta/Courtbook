"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.VenueController = void 0;
const Venue_1 = require("../models/Venue");
const Court_1 = require("../models/Court");
const errors_1 = require("../utils/errors");
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
            const { name, address, city, location, amenities, operatingHours, contact, images } = req.body;
            if (!name || !address || !city) {
                throw new errors_1.BadRequestError('Venue name, address, and city are required');
            }
            const venue = new Venue_1.Venue({
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
        }
        catch (error) {
            next(error);
        }
    }
}
exports.VenueController = VenueController;
