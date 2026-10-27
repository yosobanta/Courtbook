"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PricingService = void 0;
const Offer_1 = require("../models/Offer");
const errors_1 = require("../utils/errors");
class PricingService {
    /**
     * Recalculates and validates the price and discount entirely on the server.
     * NEVER trust client-provided calculations or discounts.
     */
    static async calculateBookingPrice(court, slotPrices, offerCode) {
        const originalAmount = slotPrices.reduce((sum, price) => sum + price, 0);
        if (!offerCode || offerCode.trim() === '') {
            return {
                originalAmount,
                discountAmount: 0,
                finalAmount: originalAmount,
                offer: null,
            };
        }
        const code = offerCode.trim().toUpperCase();
        const offer = await Offer_1.Offer.findOne({ code });
        if (!offer) {
            throw new errors_1.BadRequestError(`Coupon code "${code}" is invalid or does not exist.`);
        }
        if (!offer.active) {
            throw new errors_1.BadRequestError(`Coupon code "${code}" is no longer active.`);
        }
        const now = new Date();
        if (now < offer.validFrom || now > offer.validUntil) {
            throw new errors_1.BadRequestError(`Coupon code "${code}" has expired or is not yet valid.`);
        }
        if (offer.usedCount >= offer.usageLimit) {
            throw new errors_1.BadRequestError(`Coupon code "${code}" has reached its global usage limit.`);
        }
        // Check minimum booking amount
        if (originalAmount < offer.minBookingAmount) {
            throw new errors_1.BadRequestError(`Minimum booking amount of ₹${offer.minBookingAmount} required to apply coupon "${code}".`);
        }
        // Check sport eligibility
        if (offer.applicableSports.length > 0 && !offer.applicableSports.includes(court.sport)) {
            throw new errors_1.BadRequestError(`Coupon "${code}" is not applicable for ${court.sport}.`);
        }
        // Check venue eligibility
        if (offer.applicableVenues.length > 0 &&
            !offer.applicableVenues.some((vId) => vId.equals(court.venueId))) {
            throw new errors_1.BadRequestError(`Coupon "${code}" is not applicable at this venue.`);
        }
        // Check court eligibility
        if (offer.applicableCourts.length > 0 &&
            !offer.applicableCourts.some((cId) => cId.equals(court._id))) {
            throw new errors_1.BadRequestError(`Coupon "${code}" is not applicable for this specific court.`);
        }
        // Calculate discount
        let discountAmount = 0;
        if (offer.type === 'PERCENTAGE') {
            discountAmount = Math.round((originalAmount * offer.value) / 100);
            if (offer.maxDiscount > 0 && discountAmount > offer.maxDiscount) {
                discountAmount = offer.maxDiscount;
            }
        }
        else if (offer.type === 'FLAT') {
            discountAmount = Math.min(offer.value, originalAmount);
        }
        const finalAmount = Math.max(0, originalAmount - discountAmount);
        return {
            originalAmount,
            discountAmount,
            finalAmount,
            offer,
        };
    }
}
exports.PricingService = PricingService;
