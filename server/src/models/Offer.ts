import mongoose, { Document, Schema, Model } from 'mongoose';

export type OfferType = 'PERCENTAGE' | 'FLAT';

export interface IOffer extends Document {
  _id: mongoose.Types.ObjectId;
  code: string;
  title: string;
  description: string;
  type: OfferType;
  value: number; // e.g. 50 (for 50%) or 100 (for ₹100 flat)
  maxDiscount: number; // e.g. 200 (max cap for percentage discount)
  minBookingAmount: number; // minimum amount required before coupon applies
  validFrom: Date;
  validUntil: Date;
  usageLimit: number; // maximum times this offer can be claimed globally
  usedCount: number;  // current claimed count
  applicableSports: string[]; // empty array applies to all sports
  applicableVenues: mongoose.Types.ObjectId[];
  applicableCourts: mongoose.Types.ObjectId[];
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const offerSchema = new Schema<IOffer>(
  {
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
        type: Schema.Types.ObjectId,
        ref: 'Venue',
      },
    ],
    applicableCourts: [
      {
        type: Schema.Types.ObjectId,
        ref: 'Court',
      },
    ],
    active: {
      type: Boolean,
      default: true,
    },
  },
  {
    timestamps: true,
  }
);

// Unique index on coupon code for O(1) coupon validation
offerSchema.index({ code: 1 }, { unique: true });

export const Offer: Model<IOffer> = mongoose.model<IOffer>('Offer', offerSchema);
