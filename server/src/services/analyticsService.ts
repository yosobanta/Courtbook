import mongoose from 'mongoose';
import { Booking } from '../models/Booking';
import { Slot } from '../models/Slot';
import { User } from '../models/User';
import { Court } from '../models/Court';
import { Venue } from '../models/Venue';

export class AnalyticsService {
  /**
   * Aggregation Pipeline 1: Revenue by Venue
   * Groups confirmed bookings by venueId, calculates total and average revenue,
   * joins venue details with $lookup, and projects formatted metrics.
   */
  public static async getRevenueByVenue(): Promise<any[]> {
    return Booking.aggregate([
      { $match: { status: 'CONFIRMED', paymentStatus: 'PAID' } },
      {
        $group: {
          _id: '$venueId',
          totalRevenue: { $sum: '$finalAmount' },
          totalBookings: { $sum: 1 },
          averageBookingValue: { $avg: '$finalAmount' },
        },
      },
      {
        $lookup: {
          from: 'venues',
          localField: '_id',
          foreignField: '_id',
          as: 'venue',
        },
      },
      { $unwind: '$venue' },
      {
        $project: {
          _id: 0,
          venueId: '$_id',
          venueName: '$venue.name',
          city: '$venue.city',
          totalRevenue: 1,
          totalBookings: 1,
          averageBookingValue: { $round: ['$averageBookingValue', 2] },
        },
      },
      { $sort: { totalRevenue: -1 } },
    ]);
  }

  /**
   * Aggregation Pipeline 2: Most Popular Sports
   * Matches confirmed bookings, joins with court documents to get the sport,
   * groups by sport category, and calculates total bookings and revenue per sport.
   */
  public static async getPopularSports(): Promise<any[]> {
    return Booking.aggregate([
      { $match: { status: 'CONFIRMED' } },
      {
        $lookup: {
          from: 'courts',
          localField: 'courtId',
          foreignField: '_id',
          as: 'court',
        },
      },
      { $unwind: '$court' },
      {
        $group: {
          _id: '$court.sport',
          bookingCount: { $sum: 1 },
          totalRevenue: { $sum: '$finalAmount' },
        },
      },
      {
        $project: {
          _id: 0,
          sport: '$_id',
          bookingCount: 1,
          totalRevenue: 1,
        },
      },
      { $sort: { bookingCount: -1 } },
    ]);
  }

  /**
   * Aggregation Pipeline 3: Court Utilization Rate for a specific date
   * Groups slots by courtId and uses conditional $sum with $cond to calculate
   * utilization: (bookedSlots / (totalSlots - blockedSlots)) * 100.
   */
  public static async getCourtUtilization(targetDate: string): Promise<any[]> {
    return Slot.aggregate([
      { $match: { date: targetDate } },
      {
        $group: {
          _id: '$courtId',
          totalSlots: { $sum: 1 },
          bookedSlots: {
            $sum: { $cond: [{ $eq: ['$status', 'BOOKED'] }, 1, 0] },
          },
          blockedSlots: {
            $sum: { $cond: [{ $eq: ['$status', 'BLOCKED'] }, 1, 0] },
          },
        },
      },
      {
        $lookup: {
          from: 'courts',
          localField: '_id',
          foreignField: '_id',
          as: 'court',
        },
      },
      { $unwind: '$court' },
      {
        $project: {
          _id: 0,
          courtId: '$_id',
          courtName: '$court.name',
          sport: '$court.sport',
          totalSlots: 1,
          bookedSlots: 1,
          blockedSlots: 1,
          utilizationPercentage: {
            $cond: [
              { $gt: [{ $subtract: ['$totalSlots', '$blockedSlots'] }, 0] },
              {
                $min: [
                  100,
                  {
                    $max: [
                      0,
                      {
                        $round: [
                          {
                            $multiply: [
                              {
                                $divide: [
                                  '$bookedSlots',
                                  { $subtract: ['$totalSlots', '$blockedSlots'] },
                                ],
                              },
                              100,
                            ],
                          },
                          1,
                        ],
                      },
                    ],
                  },
                ],
              },
              0,
            ],
          },
        },
      },
      { $sort: { utilizationPercentage: -1 } },
    ]);
  }

  /**
   * Aggregation Pipeline 4: Admin Dashboard KPI Summary
   * Computes high-level platform stats including today's bookings, monthly revenue,
   * active users, and total courts.
   */
  public static async getAdminDashboardSummary(): Promise<any> {
    const todayStr = new Date().toISOString().split('T')[0];

    const [totalUsers, totalVenues, totalCourts] = await Promise.all([
      User.countDocuments({ role: 'PLAYER' }),
      Venue.countDocuments({ isActive: true }),
      Court.countDocuments({ isActive: true }),
    ]);

    // Aggregate today's bookings and today's revenue
    const todayStats = await Booking.aggregate([
      { $match: { date: todayStr, status: 'CONFIRMED' } },
      {
        $group: {
          _id: null,
          todayBookings: { $sum: 1 },
          todayRevenue: { $sum: '$finalAmount' },
        },
      },
    ]);

    // Aggregate monthly revenue
    const currentMonthPrefix = todayStr.substring(0, 7); // e.g. "2026-09"
    const monthlyStats = await Booking.aggregate([
      {
        $match: {
          date: { $regex: `^${currentMonthPrefix}` },
          status: 'CONFIRMED',
          paymentStatus: 'PAID',
        },
      },
      {
        $group: {
          _id: null,
          monthlyRevenue: { $sum: '$finalAmount' },
          monthlyBookings: { $sum: 1 },
        },
      },
    ]);

    // Recent 5 bookings
    const recentBookings = await Booking.find()
      .sort({ createdAt: -1 })
      .limit(5)
      .populate('courtId', 'name sport')
      .populate('userId', 'name email');

    return {
      kpi: {
        totalUsers,
        totalVenues,
        totalCourts,
        todayBookings: todayStats[0]?.todayBookings || 0,
        todayRevenue: todayStats[0]?.todayRevenue || 0,
        monthlyRevenue: monthlyStats[0]?.monthlyRevenue || 0,
        monthlyBookings: monthlyStats[0]?.monthlyBookings || 0,
      },
      recentBookings,
    };
  }
}
