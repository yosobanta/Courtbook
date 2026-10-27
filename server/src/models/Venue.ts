import mongoose, { Document, Schema, Model } from 'mongoose';

export interface IOperatingHours {
  open: string;  // e.g. "06:00"
  close: string; // e.g. "23:00"
}

export interface IContact {
  phone: string;
  email: string;
}

export interface IGeoPoint {
  type: 'Point';
  coordinates: [number, number]; // [longitude, latitude]
}

export interface IVenue extends Document {
  _id: mongoose.Types.ObjectId;
  name: string;
  description: string;
  address: string;
  city: string;
  location: IGeoPoint;
  amenities: string[];
  operatingHours: IOperatingHours;
  contact: IContact;
  images: string[];
  rating: {
    average: number;
    count: number;
  };
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const venueSchema = new Schema<IVenue>(
  {
    name: {
      type: String,
      required: [true, 'Venue name is required'],
      trim: true,
    },
    description: {
      type: String,
      default: '',
    },
    address: {
      type: String,
      required: [true, 'Address is required'],
    },
    city: {
      type: String,
      required: [true, 'City is required'],
      trim: true,
      index: true,
    },
    location: {
      type: {
        type: String,
        enum: ['Point'],
        required: true,
        default: 'Point',
      },
      coordinates: {
        type: [Number], // [longitude, latitude]
        required: true,
      },
    },
    amenities: {
      type: [String],
      default: ['PARKING', 'DRINKING_WATER', 'RESTROOM'],
    },
    operatingHours: {
      open: { type: String, default: '06:00' },
      close: { type: String, default: '23:00' },
    },
    contact: {
      phone: { type: String, default: '' },
      email: { type: String, default: '' },
    },
    images: {
      type: [String],
      default: [],
    },
    rating: {
      average: { type: Number, default: 4.5, min: 0, max: 5 },
      count: { type: Number, default: 0 },
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

// 2dsphere index for geospatial queries ($near, $geoNear)
venueSchema.index({ location: '2dsphere' });
// Compound index for city filtering
venueSchema.index({ city: 1, isActive: 1 });

export const Venue: Model<IVenue> = mongoose.model<IVenue>('Venue', venueSchema);
