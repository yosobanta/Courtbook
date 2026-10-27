import mongoose, { Document, Schema, Model } from 'mongoose';

export interface IReview extends Document {
  _id: mongoose.Types.ObjectId;
  userId: mongoose.Types.ObjectId;
  userName: string;
  courtId: mongoose.Types.ObjectId;
  venueId: mongoose.Types.ObjectId;
  bookingId: mongoose.Types.ObjectId;
  rating: number; // 1 to 5
  comment: string;
  isModerated: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const reviewSchema = new Schema<IReview>(
  {
    userId: {
      type: Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    userName: {
      type: String,
      required: true,
      trim: true,
    },
    courtId: {
      type: Schema.Types.ObjectId,
      ref: 'Court',
      required: true,
      index: true,
    },
    venueId: {
      type: Schema.Types.ObjectId,
      ref: 'Venue',
      required: true,
      index: true,
    },
    bookingId: {
      type: Schema.Types.ObjectId,
      ref: 'Booking',
      required: true,
    },
    rating: {
      type: Number,
      required: true,
      min: 1,
      max: 5,
    },
    comment: {
      type: String,
      required: true,
      trim: true,
      maxlength: 1000,
    },
    isModerated: {
      type: Boolean,
      default: false,
    },
  },
  {
    timestamps: true,
  }
);

// COMPOUND UNIQUE INDEX: User can only review a specific booking once
reviewSchema.index({ userId: 1, bookingId: 1 }, { unique: true });

// COMPOUND INDEX for retrieving court reviews in reverse chronological order
reviewSchema.index({ courtId: 1, createdAt: -1 });

export const Review: Model<IReview> = mongoose.model<IReview>('Review', reviewSchema);
