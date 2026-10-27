"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CourtController = void 0;
const mongoose_1 = __importDefault(require("mongoose"));
const Court_1 = require("../models/Court");
const Venue_1 = require("../models/Venue");
const errors_1 = require("../utils/errors");
class CourtController {
    static async getCourts(req, res, next) {
        try {
            const { sport, venueId, isIndoor, minPrice, maxPrice, search } = req.query;
            const query = { isActive: true };
            if (sport) {
                query.sport = String(sport).toUpperCase();
            }
            if (venueId) {
                query.venueId = new mongoose_1.default.Types.ObjectId(String(venueId));
            }
            if (isIndoor !== undefined) {
                query.isIndoor = isIndoor === 'true';
            }
            if (minPrice || maxPrice) {
                query.basePrice = {};
                if (minPrice)
                    query.basePrice.$gte = Number(minPrice);
                if (maxPrice)
                    query.basePrice.$lte = Number(maxPrice);
            }
            if (search) {
                query.name = new RegExp(String(search), 'i');
            }
            const courts = await Court_1.Court.find(query)
                .populate('venueId', 'name address city rating location')
                .sort({ basePrice: 1 });
            res.status(200).json({ success: true, count: courts.length, data: courts });
        }
        catch (error) {
            next(error);
        }
    }
    static async getCourtById(req, res, next) {
        try {
            const { id } = req.params;
            const court = await Court_1.Court.findById(id).populate('venueId');
            if (!court) {
                throw new errors_1.NotFoundError('Court not found');
            }
            res.status(200).json({ success: true, data: court });
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * MongoDB Geospatial Query demonstration:
     * Uses $geoNear aggregation stage on venues with 2dsphere index,
     * then joins corresponding courts.
     */
    static async getNearbyCourts(req, res, next) {
        try {
            const lng = parseFloat(req.query.lng) || 77.5946; // Bangalore default
            const lat = parseFloat(req.query.lat) || 12.9716;
            const maxDistanceMeters = parseInt(req.query.maxDistanceMeters, 10) || 15000; // 15km default
            // Run $geoNear on venues collection
            const nearbyVenues = await Venue_1.Venue.aggregate([
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
        }
        catch (error) {
            next(error);
        }
    }
    static async createCourt(req, res, next) {
        try {
            const { venueId, name, sport, isIndoor, surfaceType, capacity, basePrice, pricing, images } = req.body;
            if (!venueId || !name || !sport || basePrice === undefined) {
                throw new errors_1.BadRequestError('venueId, name, sport, and basePrice are required');
            }
            const venue = await Venue_1.Venue.findById(venueId);
            if (!venue) {
                throw new errors_1.NotFoundError('Referenced venue not found');
            }
            const court = new Court_1.Court({
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
        }
        catch (error) {
            next(error);
        }
    }
}
exports.CourtController = CourtController;
