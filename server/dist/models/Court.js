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
exports.Court = void 0;
const mongoose_1 = __importStar(require("mongoose"));
const courtSchema = new mongoose_1.Schema({
    venueId: {
        type: mongoose_1.Schema.Types.ObjectId,
        ref: 'Venue',
        required: [true, 'Venue reference is required'],
        index: true,
    },
    name: {
        type: String,
        required: [true, 'Court name is required'],
        trim: true,
    },
    sport: {
        type: String,
        enum: ['BADMINTON', 'TENNIS', 'FOOTBALL', 'BASKETBALL', 'CRICKET', 'TABLE_TENNIS'],
        required: [true, 'Sport category is required'],
        index: true,
    },
    isIndoor: {
        type: Boolean,
        default: true,
    },
    surfaceType: {
        type: String,
        default: 'SYNTHETIC',
    },
    capacity: {
        type: Number,
        default: 4,
    },
    basePrice: {
        type: Number,
        required: [true, 'Base price per hour is required'],
        min: 0,
    },
    pricing: {
        weekday: { type: Number, default: 400 },
        weekend: { type: Number, default: 500 },
        peak: { type: Number, default: 550 },
    },
    images: {
        type: [String],
        default: [],
    },
    isActive: {
        type: Boolean,
        default: true,
    },
}, {
    timestamps: true,
});
// Compound index for sport filtering by venue
courtSchema.index({ venueId: 1, sport: 1, isActive: 1 });
exports.Court = mongoose_1.default.model('Court', courtSchema);
