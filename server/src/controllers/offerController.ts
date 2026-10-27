import { Request, Response, NextFunction } from 'express';
import { Offer } from '../models/Offer';
import { Court } from '../models/Court';
import { PricingService } from '../services/pricingService';
import { BadRequestError, NotFoundError } from '../utils/errors';

export class OfferController {
  public static async getActiveOffers(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const now = new Date();
      const { sport } = req.query;

      const query: any = {
        active: true,
        validFrom: { $lte: now },
        validUntil: { $gte: now },
        $expr: { $lt: ['$usedCount', '$usageLimit'] },
      };

      if (sport) {
        query.$or = [
          { applicableSports: { $size: 0 } },
          { applicableSports: String(sport).toUpperCase() },
        ];
      }

      const offers = await Offer.find(query).sort({ value: -1 });
      res.status(200).json({ success: true, count: offers.length, data: offers });
    } catch (error) {
      next(error);
    }
  }

  public static async validateOffer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { code, courtId, bookingAmount } = req.body;

      if (!code || !courtId || bookingAmount === undefined) {
        throw new BadRequestError('code, courtId, and bookingAmount are required');
      }

      const court = await Court.findById(courtId);
      if (!court) {
        throw new NotFoundError('Court not found');
      }

      const calculation = await PricingService.calculateBookingPrice(
        court,
        [Number(bookingAmount)],
        code
      );

      res.status(200).json({
        success: true,
        message: 'Coupon is valid',
        data: {
          code: calculation.offer?.code,
          title: calculation.offer?.title,
          originalAmount: calculation.originalAmount,
          discountAmount: calculation.discountAmount,
          finalAmount: calculation.finalAmount,
        },
      });
    } catch (error) {
      next(error);
    }
  }
}
