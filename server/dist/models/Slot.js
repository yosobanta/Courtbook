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
exports.Slot = void 0;
const mongoose_1 = __importStar(require("mongoose"));
const slotSchema = new mongoose_1.Schema({
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
    date: {
        type: String,
        required: [true, 'Date string (YYYY-MM-DD) is required'],
        match: [/^\d{4}-\d{2}-\d{2}$/, 'Date must be formatted as YYYY-MM-DD'],
    },
    startTime: {
        type: String,
        required: [true, 'Start time is required'],
        match: [/^\d{2}:\d{2}$/, 'Start time must be formatted as HH:mm'],
    },
    endTime: {
        type: String,
        required: [true, 'End time is required'],
        match: [/^\d{2}:\d{2}$/, 'End time must be formatted as HH:mm'],
    },
    price: {
        type: Number,
        required: [true, 'Slot price is required'],
        min: 0,
    },
    status: {
        type: String,
        enum: ['AVAILABLE', 'HELD', 'BOOKED', 'BLOCKED'],
        default: 'AVAILABLE',
        index: true,
    },
    heldBy: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'User',
        default: null,
    },
    holdExpiresAt: {
        type: Date,
        default: null,
    },
    bookingId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'Booking',
        default: null,
    },
    blockReason: {
        type: String,
        default: null,
    },
    version: {
        type: Number,
        default: 0,
    },
}, {
    timestamps: true,
});
// CRITICAL COMPOUND UNIQUE INDEX:
// Guarantees database-level consistency that two identical slot documents cannot exist.
slotSchema.index({ courtId: 1, date: 1, startTime: 1 }, { unique: true });
// COMPOUND INDEX FOR FAST AVAILABILITY QUERIES:
// Accelerates queries filtering available slots for a court on a selected date.
slotSchema.index({ courtId: 1, date: 1, status: 1 });
// COMPOUND INDEX FOR EXPIRED HOLD CLEANUP / REAPER:
slotSchema.index({ status: 1, holdExpiresAt: 1 });
exports.Slot = mongoose_1.default.model('Slot', slotSchema);
