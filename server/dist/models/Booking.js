"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.Booking = void 0;
const mongoose_1 = __importStar(require("mongoose"));
const bookingSchema = new mongoose_1.Schema({
    bookingNumber: {
        type: String,
        required: true,
        unique: true,
        uppercase: true,
        index: true,
    },
    userId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'User',
        required: [true, 'User reference is required'],
        index: true,
    },
    courtId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'Court',
        required: [true, 'Court reference is required'],
        index: true,
    },
    venueId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'Venue',
        required: [true, 'Venue reference is required'],
        index: true,
    },
    slotIds: [
        {
            type: mongoose_1.Schema.Types.ObjectId,
            ref: 'Slot',
            required: true,
        },
    ],
    date: {
        type: String,
        required: true,
    },
    startTime: {
        type: String,
        required: true,
    },
    endTime: {
        type: String,
        required: true,
    },
    // CRITICAL: Historical Price Immutability.
    // Stores the exact amounts charged at transaction time.
    originalAmount: {
        type: Number,
        required: true,
        min: 0,
    },
    discountAmount: {
        type: Number,
        default: 0,
        min: 0,
    },
    finalAmount: {
        type: Number,
        required: true,
        min: 0,
    },
    offerId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'Offer',
        default: null,
    },
    offerCode: {
        type: String,
        default: null,
    },
    status: {
        type: String,
        enum: ['PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'FAILED'],
        default: 'CONFIRMED',
        index: true,
    },
    paymentStatus: {
        type: String,
        enum: ['PENDING', 'PAID', 'REFUNDED', 'FAILED'],
        default: 'PAID',
        index: true,
    },
    paymentDetails: {
        transactionId: { type: String, default: '' },
        method: { type: String, default: 'MOCK_UPI' },
        paidAt: { type: Date, default: Date.now },
    },
    cancellationReason: {
        type: String,
        default: null,
    },
}, {
    timestamps: true,
});
// COMPOUND INDEXES FOR REAL QUERY PATTERNS:
// 1. Player "My Bookings" page sorted chronologically
bookingSchema.index({ userId: 1, createdAt: -1 });
// 2. Schedule conflict validation and court schedule views
bookingSchema.index({ courtId: 1, date: 1 });
// 3. Admin dashboard and analytics aggregation pipelines (date + status)
bookingSchema.index({ status: 1, date: 1 });
exports.Booking = mongoose_1.default.model('Booking', bookingSchema);
