import { Request, Response, NextFunction } from 'express';
import { AnalyticsService } from '../services/analyticsService';
import { SlotService } from '../services/slotService';
import { Booking } from '../models/Booking';
import { User } from '../models/User';
import { Offer } from '../models/Offer';
import { Court } from '../models/Court';
import { Venue } from '../models/Venue';
import { BadRequestError, NotFoundError } from '../utils/errors';

export class AdminController {
  public static async getDashboard(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const summary = await AnalyticsService.getAdminDashboardSummary();
      res.status(200).json({ success: true, data: summary });
    } catch (error) {
      next(error);
    }
  }

  public static async getAllBookings(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { date, status, courtId, venueId, limit, page } = req.query;
      const query: any = {};

      if (date) query.date = String(date);
      if (status) query.status = String(status).toUpperCase();
      if (courtId) query.courtId = courtId;
      if (venueId) query.venueId = venueId;

      const pageNum = parseInt(page as string, 10) || 1;
      const limitNum = parseInt(limit as string, 10) || 20;

      const [bookings, total] = await Promise.all([
        Booking.find(query)
          .sort({ createdAt: -1 })
          .skip((pageNum - 1) * limitNum)
          .limit(limitNum)
          .populate('userId', 'name email phone')
          .populate('courtId', 'name sport')
          .populate('venueId', 'name city'),
        Booking.countDocuments(query),
      ]);

      res.status(200).json({
        success: true,
        total,
        page: pageNum,
        totalPages: Math.ceil(total / limitNum),
        data: bookings,
      });
    } catch (error) {
      next(error);
    }
  }

  public static async generateSlots(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const courtId = req.params.id;
      const { startDate, endDate, startHour, endHour, slotDurationMinutes, price } = req.body;

      if (!startDate || !endDate) {
        throw new BadRequestError('startDate and endDate are required');
      }

      const result = await SlotService.generateSlots({
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
    } catch (error) {
      next(error);
    }
  }

  public static async blockSlots(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { slotIds, blockReason } = req.body;
      if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
        throw new BadRequestError('slotIds array is required');
      }

      const result = await SlotService.blockSlots(slotIds, blockReason || 'Maintenance');
      res.status(200).json({
        success: true,
        message: `Blocked ${result.modifiedCount} slot(s).`,
        data: result,
      });
    } catch (error) {
      next(error);
    }
  }

  public static async unblockSlots(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { slotIds } = req.body;
      if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
        throw new BadRequestError('slotIds array is required');
      }

      const result = await SlotService.unblockSlots(slotIds);
      res.status(200).json({
        success: true,
        message: `Unblocked ${result.modifiedCount} slot(s).`,
        data: result,
      });
    } catch (error) {
      next(error);
    }
  }

  public static async createOffer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const {
        code,
        title,
        description,
        type,
        value,
        maxDiscount,
        minBookingAmount,
        validFrom,
        validUntil,
        usageLimit,
        applicableSports,
        applicableVenues,
        applicableCourts,
      } = req.body;

      if (!code || !title || !type || value === undefined || !validFrom || !validUntil) {
        throw new BadRequestError('code, title, type, value, validFrom, and validUntil are required');
      }

      const offer = new Offer({
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
    } catch (error) {
      next(error);
    }
  }

  public static async getAllUsers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const users = await User.find().select('-passwordHash').sort({ createdAt: -1 });
      res.status(200).json({ success: true, count: users.length, data: users });
    } catch (error) {
      next(error);
    }
  }

  public static async toggleUserStatus(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      const { isActive } = req.body;

      if (typeof isActive !== 'boolean') {
        throw new BadRequestError('isActive boolean is required');
      }

      const user = await User.findByIdAndUpdate(
        id,
        { isActive },
        { new: true }
      ).select('-passwordHash');

      if (!user) {
        throw new NotFoundError('User not found');
      }

      res.status(200).json({
        success: true,
        message: `User status updated to ${isActive ? 'Active' : 'Disabled'}`,
        data: user,
      });
    } catch (error) {
      next(error);
    }
  }

  public static async getRevenueAnalytics(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = await AnalyticsService.getRevenueByVenue();
      res.status(200).json({ success: true, data });
    } catch (error) {
      next(error);
    }
  }

  public static async getPopularSportsAnalytics(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = await AnalyticsService.getPopularSports();
      res.status(200).json({ success: true, data });
    } catch (error) {
      next(error);
    }
  }

  public static async getCourtUtilizationAnalytics(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const date = (req.query.date as string) || new Date().toISOString().split('T')[0];
      const data = await AnalyticsService.getCourtUtilization(date);
      res.status(200).json({ success: true, date, data });
    } catch (error) {
      next(error);
    }
  }
}
