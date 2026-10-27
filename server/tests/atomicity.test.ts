/**
 * COURTBOOK VIVA DEMONSTRATION TEST 2:
 * MULTI-SLOT ACID ATOMICITY & ROLLBACK
 *
 * This test simulates a user attempting to book multiple consecutive slots:
 * - Slot 1 (18:00–19:00): AVAILABLE
 * - Slot 2 (19:00–20:00): AVAILABLE
 * - Slot 3 (20:00–21:00): ALREADY BOOKED by another user
 *
 * Guaranteed Outcome:
 * - Entire multi-slot transaction ABORTS
 * - Returns Conflict (HTTP 409)
 * - Slots 1 and 2 remain AVAILABLE (no partial booking!)
 * - Slot 3 remains BOOKED
 * - Proves all-or-nothing atomicity in multi-document transactions
 */

import mongoose from 'mongoose';
import { connectDatabase } from '../src/config/database';
import { Court } from '../src/models/Court';
import { Slot } from '../src/models/Slot';
import { Booking } from '../src/models/Booking';
import { User } from '../src/models/User';
import { BookingService } from '../src/services/bookingService';

async function runAtomicityTest() {
  console.log('===============================================================');
  console.log('🧪 RUNNING MULTI-SLOT ALL-OR-NOTHING ATOMICITY ROLLBACK TEST');
  console.log('===============================================================');

  await connectDatabase();

  let court = await Court.findOne();
  if (!court) {
    console.error('No court found. Please run "npm run seed" first.');
    process.exit(1);
  }

  const testDate = '2026-10-16';

  // Clean existing test data
  await Booking.deleteMany({ courtId: court._id, date: testDate });
  await Slot.deleteMany({ courtId: court._id, date: testDate });

  // 1. Create Slot 1 (18:00 - AVAILABLE)
  const slot1 = await Slot.create({
    courtId: court._id,
    venueId: court.venueId,
    date: testDate,
    startTime: '18:00',
    endTime: '19:00',
    price: 400,
    status: 'AVAILABLE',
  });

  // 2. Create Slot 2 (19:00 - AVAILABLE)
  const slot2 = await Slot.create({
    courtId: court._id,
    venueId: court.venueId,
    date: testDate,
    startTime: '19:00',
    endTime: '20:00',
    price: 400,
    status: 'AVAILABLE',
  });

  // 3. Create Slot 3 (20:00 - BOOKED by user X)
  const slot3 = await Slot.create({
    courtId: court._id,
    venueId: court.venueId,
    date: testDate,
    startTime: '20:00',
    endTime: '21:00',
    price: 400,
    status: 'BOOKED', // Pre-booked!
  });

  console.log('[Test Setup] Created 3 consecutive slots:');
  console.log(` - Slot 1 (18:00–19:00): ${slot1.status}`);
  console.log(` - Slot 2 (19:00–20:00): ${slot2.status}`);
  console.log(` - Slot 3 (20:00–21:00): ${slot3.status} (Pre-booked)`);

  let testUser = await User.findOne({ email: 'atomicity_tester@courtbook.test' });
  if (!testUser) {
    testUser = await User.create({
      name: 'Atomicity Tester',
      email: 'atomicity_tester@courtbook.test',
      phone: '+919111111111',
      passwordHash: 'dummy_hash',
      role: 'PLAYER',
      isActive: true,
    });
  }

  console.log('[Execution] User attempting to book all 3 slots [Slot 1, Slot 2, Slot 3] in one transaction...');

  let transactionFailed = false;
  try {
    await BookingService.createBooking({
      userId: testUser._id.toString(),
      courtId: court._id.toString(),
      slotIds: [slot1._id.toString(), slot2._id.toString(), slot3._id.toString()],
      paymentMethod: 'TEST_ATOMICITY',
    });
  } catch (error: any) {
    transactionFailed = true;
    console.log(`[Result] Transaction aborted as expected: "${error.message}"`);
  }

  // Verify Database State
  const verifiedSlot1 = await Slot.findById(slot1._id);
  const verifiedSlot2 = await Slot.findById(slot2._id);
  const verifiedSlot3 = await Slot.findById(slot3._id);
  const userBookings = await Booking.find({ userId: testUser._id, date: testDate });

  console.log('---------------------------------------------------------------');
  console.log('[Database State After Rollback]');
  console.log(` - Slot 1 status: ${verifiedSlot1?.status} (Expected: AVAILABLE)`);
  console.log(` - Slot 2 status: ${verifiedSlot2?.status} (Expected: AVAILABLE)`);
  console.log(` - Slot 3 status: ${verifiedSlot3?.status} (Expected: BOOKED)`);
  console.log(` - User bookings created: ${userBookings.length} (Expected: 0)`);
  console.log('---------------------------------------------------------------');

  const isPassed =
    transactionFailed &&
    verifiedSlot1?.status === 'AVAILABLE' &&
    verifiedSlot2?.status === 'AVAILABLE' &&
    verifiedSlot3?.status === 'BOOKED' &&
    userBookings.length === 0;

  if (isPassed) {
    console.log('🎉 TEST PASSED: All-or-nothing atomicity verified! No partial slots were booked.');
    process.exit(0);
  } else {
    console.error('🚨 TEST FAILED: Atomicity broken or partial booking occurred!');
    process.exit(1);
  }
}

runAtomicityTest().catch((err) => {
  console.error('Test execution failed:', err);
  process.exit(1);
});
