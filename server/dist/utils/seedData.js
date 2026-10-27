"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const database_1 = require("../config/database");
const User_1 = require("../models/User");
const Venue_1 = require("../models/Venue");
const Court_1 = require("../models/Court");
const Slot_1 = require("../models/Slot");
const Booking_1 = require("../models/Booking");
const Offer_1 = require("../models/Offer");
const Review_1 = require("../models/Review");
const slotService_1 = require("../services/slotService");
async function seedDatabase() {
    console.log('[Seed] Starting database population...');
    await (0, database_1.connectDatabase)();
    // Clear existing collections
    await Promise.all([
        User_1.User.deleteMany({}),
        Venue_1.Venue.deleteMany({}),
        Court_1.Court.deleteMany({}),
        Slot_1.Slot.deleteMany({}),
        Booking_1.Booking.deleteMany({}),
        Offer_1.Offer.deleteMany({}),
        Review_1.Review.deleteMany({}),
    ]);
    console.log('[Seed] Cleaned existing collections.');
    // 1. Create Users
    const salt = await bcryptjs_1.default.genSalt(10);
    const adminPassword = await bcryptjs_1.default.hash('Admin@123', salt);
    const playerPassword = await bcryptjs_1.default.hash('Player@123', salt);
    const admin = await User_1.User.create({
        name: 'Platform Admin',
        email: 'admin@courtbook.com',
        phone: '+919999900000',
        passwordHash: adminPassword,
        role: 'ADMIN',
        isActive: true,
    });
    const player1 = await User_1.User.create({
        name: 'Alex Johnson',
        email: 'alex@example.com',
        phone: '+919876543210',
        passwordHash: playerPassword,
        role: 'PLAYER',
        isActive: true,
    });
    const player2 = await User_1.User.create({
        name: 'Sarah Miller',
        email: 'sarah@example.com',
        phone: '+919876543211',
        passwordHash: playerPassword,
        role: 'PLAYER',
        isActive: true,
    });
    console.log('[Seed] Users created: Admin (admin@courtbook.com) and Players (alex@example.com, sarah@example.com)');
    // 2. Create Venues (with GeoJSON 2dsphere points)
    const venue1 = await Venue_1.Venue.create({
        name: 'Skyline Sports Arena',
        description: 'Premier sports destination featuring state-of-the-art synthetic wooden courts.',
        address: '80 Feet Road, 4th Block, Koramangala',
        city: 'Bengaluru',
        location: {
            type: 'Point',
            coordinates: [77.6245, 12.9352], // [lng, lat]
        },
        amenities: ['PARKING', 'LOCKER_ROOM', 'DRINKING_WATER', 'SHOWER', 'EQUIPMENT_RENTAL'],
        operatingHours: { open: '06:00', close: '23:00' },
        contact: { phone: '+918025531001', email: 'koramangala@skylinesports.in' },
        images: [
            'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80',
        ],
        rating: { average: 4.8, count: 56 },
        isActive: true,
    });
    const venue2 = await Venue_1.Venue.create({
        name: 'SmashZone Badminton Hub',
        description: 'Dedicated professional badminton facility with BWF-approved flooring.',
        address: '100 Feet Road, Indiranagar',
        city: 'Bengaluru',
        location: {
            type: 'Point',
            coordinates: [77.6412, 12.9784],
        },
        amenities: ['PARKING', 'DRINKING_WATER', 'AIR_CONDITIONED', 'RESTROOM'],
        operatingHours: { open: '05:00', close: '22:00' },
        contact: { phone: '+918025252002', email: 'support@smashzone.in' },
        images: [
            'https://images.unsplash.com/photo-1544919982-b61976f0ba43?auto=format&fit=crop&w=800&q=80',
        ],
        rating: { average: 4.6, count: 38 },
        isActive: true,
    });
    const venue3 = await Venue_1.Venue.create({
        name: 'Champions Turf & Arena',
        description: 'Multi-sport complex offering 5-a-side football turf and clay tennis courts.',
        address: 'Sector 2, HSR Layout',
        city: 'Bengaluru',
        location: {
            type: 'Point',
            coordinates: [77.6389, 12.9121],
        },
        amenities: ['PARKING', 'FLOODLIGHTS', 'CAFE', 'DRINKING_WATER'],
        operatingHours: { open: '06:00', close: '23:00' },
        contact: { phone: '+918025723003', email: 'info@championsturf.in' },
        images: [
            'https://images.unsplash.com/photo-1529900245534-47fbf8221565?auto=format&fit=crop&w=800&q=80',
        ],
        rating: { average: 4.5, count: 29 },
        isActive: true,
    });
    console.log('[Seed] 3 Venues created with GeoJSON coordinates.');
    // 3. Create Courts
    const courts = await Court_1.Court.insertMany([
        {
            venueId: venue1._id,
            name: 'Badminton Court 1 (Indoor Wooden)',
            sport: 'BADMINTON',
            isIndoor: true,
            surfaceType: 'SYNTHETIC_WOODEN',
            capacity: 4,
            basePrice: 400,
            pricing: { weekday: 400, weekend: 500, peak: 550 },
            images: ['https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
        {
            venueId: venue1._id,
            name: 'Badminton Court 2 (Synthetic)',
            sport: 'BADMINTON',
            isIndoor: true,
            surfaceType: 'SYNTHETIC_MAT',
            capacity: 4,
            basePrice: 350,
            pricing: { weekday: 350, weekend: 450, peak: 500 },
            images: ['https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
        {
            venueId: venue2._id,
            name: 'Smash Pro Court A',
            sport: 'BADMINTON',
            isIndoor: true,
            surfaceType: 'BWF_APPROVED_MAT',
            capacity: 4,
            basePrice: 450,
            pricing: { weekday: 450, weekend: 550, peak: 600 },
            images: ['https://images.unsplash.com/photo-1544919982-b61976f0ba43?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
        {
            venueId: venue3._id,
            name: '5-a-side Football Arena',
            sport: 'FOOTBALL',
            isIndoor: false,
            surfaceType: 'ARTIFICIAL_TURF',
            capacity: 10,
            basePrice: 1200,
            pricing: { weekday: 1200, weekend: 1500, peak: 1800 },
            images: ['https://images.unsplash.com/photo-1529900245534-47fbf8221565?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
        {
            venueId: venue3._id,
            name: 'Clay Tennis Court 1',
            sport: 'TENNIS',
            isIndoor: false,
            surfaceType: 'RED_CLAY',
            capacity: 4,
            basePrice: 600,
            pricing: { weekday: 600, weekend: 750, peak: 800 },
            images: ['https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
        {
            venueId: venue1._id,
            name: 'Table Tennis Arena 1',
            sport: 'TABLE_TENNIS',
            isIndoor: true,
            surfaceType: 'WOODEN',
            capacity: 4,
            basePrice: 250,
            pricing: { weekday: 250, weekend: 300, peak: 350 },
            images: ['https://images.unsplash.com/photo-1534158914592-062992fbe900?auto=format&fit=crop&w=800&q=80'],
            isActive: true,
        },
    ]);
    console.log(`[Seed] ${courts.length} Courts created across venues.`);
    // 4. Create Promotional Offers
    const today = new Date();
    const nextYear = new Date(today.getFullYear() + 1, today.getMonth(), today.getDate());
    const offers = await Offer_1.Offer.insertMany([
        {
            code: 'WELCOME50',
            title: '50% Welcome Discount',
            description: 'Get 50% discount up to ₹200 on your booking.',
            type: 'PERCENTAGE',
            value: 50,
            maxDiscount: 200,
            minBookingAmount: 300,
            validFrom: today,
            validUntil: nextYear,
            usageLimit: 1000,
            usedCount: 0,
            applicableSports: [],
            active: true,
        },
        {
            code: 'FLAT100',
            title: 'Flat ₹100 Off',
            description: 'Instant ₹100 discount on bookings above ₹400.',
            type: 'FLAT',
            value: 100,
            maxDiscount: 100,
            minBookingAmount: 400,
            validFrom: today,
            validUntil: nextYear,
            usageLimit: 500,
            usedCount: 0,
            applicableSports: [],
            active: true,
        },
        {
            code: 'SMASH20',
            title: '20% Badminton Special',
            description: '20% off exclusively for badminton enthusiasts.',
            type: 'PERCENTAGE',
            value: 20,
            maxDiscount: 150,
            minBookingAmount: 350,
            validFrom: today,
            validUntil: nextYear,
            usageLimit: 200,
            usedCount: 0,
            applicableSports: ['BADMINTON'],
            active: true,
        },
    ]);
    console.log(`[Seed] ${offers.length} Promotional Offers created.`);
    // 5. Generate Slots for each court for Today and Next 7 Days
    const startDateStr = today.toISOString().split('T')[0];
    const endDate = new Date(today);
    endDate.setDate(endDate.getDate() + 7);
    const endDateStr = endDate.toISOString().split('T')[0];
    let totalSlots = 0;
    for (const court of courts) {
        const res = await slotService_1.SlotService.generateSlots({
            courtId: court._id.toString(),
            startDate: startDateStr,
            endDate: endDateStr,
            startHour: 6,
            endHour: 22,
            slotDurationMinutes: 60,
            price: court.basePrice,
        });
        totalSlots += res.createdOrUpdated;
    }
    console.log(`[Seed] Generated ${totalSlots} bookable slots across all courts.`);
    // 6. Create Demo Bookings to populate Analytics pipelines
    const sampleSlot1 = await Slot_1.Slot.findOne({
        courtId: courts[0]._id,
        date: startDateStr,
        startTime: '18:00',
    });
    if (sampleSlot1) {
        const demoBooking = await Booking_1.Booking.create({
            bookingNumber: 'CB-DEMO-001',
            userId: player1._id,
            courtId: courts[0]._id,
            venueId: venue1._id,
            slotIds: [sampleSlot1._id],
            date: startDateStr,
            startTime: '18:00',
            endTime: '19:00',
            originalAmount: 400,
            discountAmount: 50,
            finalAmount: 350,
            offerId: offers[0]._id,
            offerCode: 'WELCOME50',
            status: 'CONFIRMED',
            paymentStatus: 'PAID',
            paymentDetails: {
                transactionId: 'TXN_SEED_1001',
                method: 'UPI',
                paidAt: new Date(),
            },
        });
        sampleSlot1.status = 'BOOKED';
        sampleSlot1.bookingId = demoBooking._id;
        await sampleSlot1.save();
        await Offer_1.Offer.updateOne({ _id: offers[0]._id }, { $inc: { usedCount: 1 } });
    }
    const sampleSlot2 = await Slot_1.Slot.findOne({
        courtId: courts[3]._id, // Football
        date: startDateStr,
        startTime: '19:00',
    });
    if (sampleSlot2) {
        const demoBooking2 = await Booking_1.Booking.create({
            bookingNumber: 'CB-DEMO-002',
            userId: player2._id,
            courtId: courts[3]._id,
            venueId: venue3._id,
            slotIds: [sampleSlot2._id],
            date: startDateStr,
            startTime: '19:00',
            endTime: '20:00',
            originalAmount: 1200,
            discountAmount: 100,
            finalAmount: 1100,
            offerId: offers[1]._id,
            offerCode: 'FLAT100',
            status: 'CONFIRMED',
            paymentStatus: 'PAID',
            paymentDetails: {
                transactionId: 'TXN_SEED_1002',
                method: 'NET_BANKING',
                paidAt: new Date(),
            },
        });
        sampleSlot2.status = 'BOOKED';
        sampleSlot2.bookingId = demoBooking2._id;
        await sampleSlot2.save();
    }
    console.log('[Seed] Demo bookings created for initial analytics.');
    console.log('[Seed] Database seeding completed successfully! 🎉');
    process.exit(0);
}
seedDatabase().catch((err) => {
    console.error('[Seed] Error during seeding:', err);
    process.exit(1);
});
