"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OfferController = void 0;
const Offer_1 = require("../models/Offer");
const Court_1 = require("../models/Court");
const pricingService_1 = require("../services/pricingService");
const errors_1 = require("../utils/errors");
class OfferController {
    static async getActiveOffers(req, res, next) {
        try {
            const now = new Date();
            const { sport } = req.query;
            const query = {
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
            const offers = await Offer_1.Offer.find(query).sort({ value: -1 });
            res.status(200).json({ success: true, count: offers.length, data: offers });
        }
        catch (error) {
            next(error);
        }
    }
    static async validateOffer(req, res, next) {
        try {
            const { code, courtId, bookingAmount } = req.body;
            if (!code || !courtId || bookingAmount === undefined) {
                throw new errors_1.BadRequestError('code, courtId, and bookingAmount are required');
            }
            const court = await Court_1.Court.findById(courtId);
            if (!court) {
                throw new errors_1.NotFoundError('Court not found');
            }
            const calculation = await pricingService_1.PricingService.calculateBookingPrice(court, [Number(bookingAmount)], code);
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
        }
        catch (error) {
            next(error);
        }
    }
}
exports.OfferController = OfferController;
