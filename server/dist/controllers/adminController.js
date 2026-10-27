"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AdminController = void 0;
const analyticsService_1 = require("../services/analyticsService");
const slotService_1 = require("../services/slotService");
const Booking_1 = require("../models/Booking");
const User_1 = require("../models/User");
const Offer_1 = require("../models/Offer");
const errors_1 = require("../utils/errors");
class AdminController {
    static async getDashboard(req, res, next) {
        try {
            const summary = await analyticsService_1.AnalyticsService.getAdminDashboardSummary();
            res.status(200).json({ success: true, data: summary });
        }
        catch (error) {
            next(error);
        }
    }
    static async getAllBookings(req, res, next) {
        try {
            const { date, status, courtId, venueId, limit, page } = req.query;
            const query = {};
            if (date)
                query.date = String(date);
            if (status)
                query.status = String(status).toUpperCase();
            if (courtId)
                query.courtId = courtId;
            if (venueId)
                query.venueId = venueId;
            const pageNum = parseInt(page, 10) || 1;
            const limitNum = parseInt(limit, 10) || 20;
            const [bookings, total] = await Promise.all([
                Booking_1.Booking.find(query)
                    .sort({ createdAt: -1 })
                    .skip((pageNum - 1) * limitNum)
                    .limit(limitNum)
                    .populate('userId', 'name email phone')
                    .populate('courtId', 'name sport')
                    .populate('venueId', 'name city'),
                Booking_1.Booking.countDocuments(query),
            ]);
            res.status(200).json({
                success: true,
                total,
                page: pageNum,
                totalPages: Math.ceil(total / limitNum),
                data: bookings,
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async generateSlots(req, res, next) {
        try {
            const courtId = req.params.id;
            const { startDate, endDate, startHour, endHour, slotDurationMinutes, price } = req.body;
            if (!startDate || !endDate) {
                throw new errors_1.BadRequestError('startDate and endDate are required');
            }
            const result = await slotService_1.SlotService.generateSlots({
                courtId,
                startDate,
                endDate,
                startHour,
                endHour,
                slotDurationMinutes,
                price,
            });
            res.status(201).json({
                success: true,
                message: `Successfully generated ${result.createdOrUpdated} slots.`,
                data: result,
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async blockSlots(req, res, next) {
        try {
            const { slotIds, blockReason } = req.body;
            if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
                throw new errors_1.BadRequestError('slotIds array is required');
            }
            const result = await slotService_1.SlotService.blockSlots(slotIds, blockReason || 'Maintenance');
            res.status(200).json({
                success: true,
                message: `Blocked ${result.modifiedCount} slot(s).`,
                data: result,
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async unblockSlots(req, res, next) {
        try {
            const { slotIds } = req.body;
            if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
                throw new errors_1.BadRequestError('slotIds array is required');
            }
            const result = await slotService_1.SlotService.unblockSlots(slotIds);
            res.status(200).json({
                success: true,
                message: `Unblocked ${result.modifiedCount} slot(s).`,
                data: result,
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async createOffer(req, res, next) {
        try {
            const { code, title, description, type, value, maxDiscount, minBookingAmount, validFrom, validUntil, usageLimit, applicableSports, applicableVenues, applicableCourts, } = req.body;
            if (!code || !title || !type || value === undefined || !validFrom || !validUntil) {
                throw new errors_1.BadRequestError('code, title, type, value, validFrom, and validUntil are required');
            }
            const offer = new Offer_1.Offer({
                code: code.trim().toUpperCase(),
                title,
                description: description || '',
                type,
                value,
                maxDiscount: maxDiscount || 0,
                minBookingAmount: minBookingAmount || 0,
                validFrom: new Date(validFrom),
                validUntil: new Date(validUntil),
                usageLimit: usageLimit || 1000,
                applicableSports: applicableSports || [],
                applicableVenues: applicableVenues || [],
                applicableCourts: applicableCourts || [],
                active: true,
            });
            await offer.save();
            res.status(201).json({ success: true, message: 'Offer created successfully', data: offer });
        }
        catch (error) {
            next(error);
        }
    }
    static async getAllUsers(req, res, next) {
        try {
            const users = await User_1.User.find().select('-passwordHash').sort({ createdAt: -1 });
            res.status(200).json({ success: true, count: users.length, data: users });
        }
        catch (error) {
            next(error);
        }
    }
    static async toggleUserStatus(req, res, next) {
        try {
            const { id } = req.params;
            const { isActive } = req.body;
            if (typeof isActive !== 'boolean') {
                throw new errors_1.BadRequestError('isActive boolean is required');
            }
            const user = await User_1.User.findByIdAndUpdate(id, { isActive }, { new: true }).select('-passwordHash');
            if (!user) {
                throw new errors_1.NotFoundError('User not found');
            }
            res.status(200).json({
                success: true,
                message: `User status updated to ${isActive ? 'Active' : 'Disabled'}`,
                data: user,
            });
        }
        catch (error) {
            next(error);
        }
    }
    static async getRevenueAnalytics(req, res, next) {
        try {
            const data = await analyticsService_1.AnalyticsService.getRevenueByVenue();
            res.status(200).json({ success: true, data });
        }
        catch (error) {
            next(error);
        }
    }
    static async getPopularSportsAnalytics(req, res, next) {
        try {
            const data = await analyticsService_1.AnalyticsService.getPopularSports();
            res.status(200).json({ success: true, data });
        }
        catch (error) {
            next(error);
        }
    }
    static async getCourtUtilizationAnalytics(req, res, next) {
        try {
            const date = req.query.date || new Date().toISOString().split('T')[0];
            const data = await analyticsService_1.AnalyticsService.getCourtUtilization(date);
            res.status(200).json({ success: true, date, data });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.AdminController = AdminController;
