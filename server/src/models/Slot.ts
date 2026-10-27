import mongoose, { Document, Schema, Model } from 'mongoose';

export type SlotStatus = 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED';

export interface ISlot extends Document {
  _id: mongoose.Types.ObjectId;
  courtId: mongoose.Types.ObjectId;
  venueId: mongoose.Types.ObjectId;
  date: string; // YYYY-MM-DD format
  startTime: string; // HH:mm format e.g. "18:00"
  endTime: string;   // HH:mm format e.g. "19:00"
  price: number;
  status: SlotStatus;
  heldBy: mongoose.Types.ObjectId | null;
  holdExpiresAt: Date | null;
  bookingId: mongoose.Types.ObjectId | null;
  blockReason: string | null;
  version: number;
  createdAt: Date;
  updatedAt: Date;
}

const slotSchema = new Schema<ISlot>(
  {
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
      type: Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    holdExpiresAt: {
      type: Date,
      default: null,
    },
    bookingId: {
      type: Schema.Types.ObjectId,
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
  },
  {
    timestamps: true,
  }
);

// CRITICAL COMPOUND UNIQUE INDEX:
// Guarantees database-level consistency that two identical slot documents cannot exist.
slotSchema.index({ courtId: 1, date: 1, startTime: 1 }, { unique: true });

// COMPOUND INDEX FOR FAST AVAILABILITY QUERIES:
// Accelerates queries filtering available slots for a court on a selected date.
slotSchema.index({ courtId: 1, date: 1, status: 1 });

// COMPOUND INDEX FOR EXPIRED HOLD CLEANUP / REAPER:
slotSchema.index({ status: 1, holdExpiresAt: 1 });

export const Slot: Model<ISlot> = mongoose.model<ISlot>('Slot', slotSchema);
