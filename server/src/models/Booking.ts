import mongoose, { Document, Schema, Model } from 'mongoose';

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'FAILED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'REFUNDED' | 'FAILED';

export interface IPaymentDetails {
  transactionId: string;
  method: string;
  paidAt: Date;
}

export interface IBooking extends Document {
  _id: mongoose.Types.ObjectId;
  bookingNumber: string;
  userId: mongoose.Types.ObjectId;
  courtId: mongoose.Types.ObjectId;
  venueId: mongoose.Types.ObjectId;
  slotIds: mongoose.Types.ObjectId[];
  date: string; // YYYY-MM-DD
  startTime: string; // e.g. "18:00"
  endTime: string;   // e.g. "20:00"
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
  offerId: mongoose.Types.ObjectId | null;
  offerCode: string | null;
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  paymentDetails: IPaymentDetails;
  cancellationReason: string | null;
  createdAt: Date;
  updatedAt: Date;
}

const bookingSchema = new Schema<IBooking>(
  {
    bookingNumber: {
      type: String,
      required: true,
      unique: true,
      uppercase: true,
      index: true,
    },
    userId: {
      type: Schema.Types.ObjectId,
      ref: 'User',
      required: [true, 'User reference is required'],
      index: true,
    },
    courtId: {
      type: Schema.Types.ObjectId,
      ref: 'Court',
      required: [true, 'Court reference is required'],
      index: true,
    },
    venueId: {
      type: Schema.Types.ObjectId,
      ref: 'Venue',
      required: [true, 'Venue reference is required'],
      index: true,
    },
    slotIds: [
      {
        type: Schema.Types.ObjectId,
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
      type: Schema.Types.ObjectId,
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
  },
  {
    timestamps: true,
  }
);

// COMPOUND INDEXES FOR REAL QUERY PATTERNS:
// 1. Player "My Bookings" page sorted chronologically
bookingSchema.index({ userId: 1, createdAt: -1 });

// 2. Schedule conflict validation and court schedule views
bookingSchema.index({ courtId: 1, date: 1 });

// 3. Admin dashboard and analytics aggregation pipelines (date + status)
bookingSchema.index({ status: 1, date: 1 });

export const Booking: Model<IBooking> = mongoose.model<IBooking>('Booking', bookingSchema);
