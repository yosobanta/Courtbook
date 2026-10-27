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
exports.Offer = void 0;
const mongoose_1 = __importStar(require("mongoose"));
const offerSchema = new mongoose_1.Schema({
    code: {
        type: String,
        required: [true, 'Coupon code is required'],
        unique: true,
        uppercase: true,
        trim: true,
    },
    title: {
        type: String,
        required: true,
        trim: true,
    },
    description: {
        type: String,
        default: '',
    },
    type: {
        type: String,
        enum: ['PERCENTAGE', 'FLAT'],
        required: true,
    },
    value: {
        type: Number,
        required: true,
        min: 1,
    },
    maxDiscount: {
        type: Number,
        default: 0, // 0 = no cap
    },
    minBookingAmount: {
        type: Number,
        default: 0,
    },
    validFrom: {
        type: Date,
        required: true,
    },
    validUntil: {
        type: Date,
        required: true,
    },
    usageLimit: {
        type: Number,
        default: 1000,
    },
    usedCount: {
        type: Number,
        default: 0,
        min: 0,
    },
    applicableSports: {
        type: [String],
        default: [],
    },
    applicableVenues: [
        {
            type: mongoose_1.Schema.Types.ObjectId,
            ref: 'Venue',
        },
    ],
    applicableCourts: [
        {
            type: mongoose_1.Schema.Types.ObjectId,
            ref: 'Court',
        },
    ],
    active: {
        type: Boolean,
        default: true,
    },
}, {
    timestamps: true,
});
// Unique index on coupon code for O(1) coupon validation
offerSchema.index({ code: 1 }, { unique: true });
exports.Offer = mongoose_1.default.model('Offer', offerSchema);
