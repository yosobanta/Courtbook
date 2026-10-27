import mongoose, { Document, Schema, Model } from 'mongoose';

export type SportType = 'BADMINTON' | 'TENNIS' | 'FOOTBALL' | 'BASKETBALL' | 'CRICKET' | 'TABLE_TENNIS';

export interface ICourtPricing {
  weekday: number;
  weekend: number;
  peak: number;
}

export interface ICourt extends Document {
  _id: mongoose.Types.ObjectId;
  venueId: mongoose.Types.ObjectId;
  name: string;
  sport: SportType;
  isIndoor: boolean;
  surfaceType: string;
  capacity: number;
  basePrice: number;
  pricing: ICourtPricing;
  images: string[];
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const courtSchema = new Schema<ICourt>(
  {
    venueId: {
      type: Schema.Types.ObjectId,
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
  },
  {
    timestamps: true,
  }
);

// Compound index for sport filtering by venue
courtSchema.index({ venueId: 1, sport: 1, isActive: 1 });

export const Court: Model<ICourt> = mongoose.model<ICourt>('Court', courtSchema);
