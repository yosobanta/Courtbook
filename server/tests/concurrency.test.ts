/**
 * COURTBOOK VIVA DEMONSTRATION TEST 1:
 * CONCURRENCY CONTROL & DOUBLE-BOOKING PREVENTION
 *
 * This test simulates 100 concurrent users attempting to book the EXACT SAME
 * court slot simultaneously.
 *
 * Guaranteed Outcome:
 * - Exactly ONE booking succeeds (HTTP 201 / success)
 * - Exactly 99 attempts fail with Conflict (HTTP 409)
 * - Database contains exactly ONE confirmed booking for that slot
 * - Proves atomic conditional updates and transaction isolation prevent race conditions
 */

import mongoose from 'mongoose';
import { connectDatabase } from '../src/config/database';
import { Court } from '../src/models/Court';
import { Slot } from '../src/models/Slot';
import { Booking } from '../src/models/Booking';
import { User } from '../src/models/User';
import { BookingService } from '../src/services/bookingService';

async function runConcurrencyTest() {
  console.log('===============================================================');
  console.log('🧪 RUNNING 100-USER CONCURRENT DOUBLE-BOOKING RACE TEST');
  console.log('===============================================================');

  await connectDatabase();

  // Find or create test court
  let court = await Court.findOne();
  if (!court) {
    console.error('No court found. Please run "npm run seed" first.');
    process.exit(1);
  }

  const testDate = '2026-10-15';
  const testStartTime = '20:00';
  const testEndTime = '21:00';

  // Clean up any existing test slot/bookings for this test date
  await Booking.deleteMany({ courtId: court._id, date: testDate });
  await Slot.deleteMany({ courtId: court._id, date: testDate, startTime: testStartTime });

  // Create a clean AVAILABLE slot
  const testSlot = await Slot.create({
    courtId: court._id,
    venueId: court.venueId,
    date: testDate,
    startTime: testStartTime,
    endTime: testEndTime,
    price: 400,
    status: 'AVAILABLE',
    heldBy: null,
    holdExpiresAt: null,
    bookingId: null,
    version: 0,
  });

  console.log(`[Test Setup] Target Slot Created: ID ${testSlot._id} (${testDate} ${testStartTime}-${testEndTime})`);

  // Create or retrieve 100 simulated test users
  const totalConcurrentUsers = 100;
  console.log(`[Test Setup] Preparing ${totalConcurrentUsers} distinct concurrent player requests...`);

  const userIds: string[] = [];
  for (let i = 1; i <= totalConcurrentUsers; i++) {
    const email = `concurrent_tester_${i}@courtbook.test`;
    let user = await User.findOne({ email });
    if (!user) {
      user = await User.create({
        name: `Tester ${i}`,
        email,
        phone: `+9190000000${i.toString().padStart(2, '0')}`,
        passwordHash: 'dummy_hash',
        role: 'PLAYER',
        isActive: true,
      });
    }
    userIds.push(user._id.toString());
  }

  console.log(`[Execution] Firing ${totalConcurrentUsers} simultaneous booking requests...`);

  let successCount = 0;
  let conflictCount = 0;
  let otherErrorCount = 0;

  const startTime = Date.now();

  // Dispatch all 100 requests concurrently
  const bookingPromises = userIds.map(async (userId, index) => {
    try {
      await BookingService.createBooking({
        userId,
        courtId: court._id.toString(),
        slotIds: [testSlot._id.toString()],
        paymentMethod: 'TEST_CONCURRENT',
      });
      successCount++;
    } catch (error: any) {
      if (error.statusCode === 409 || error.message.includes('booked') || error.message.includes('unavailable')) {
        conflictCount++;
      } else {
        otherErrorCount++;
        console.error(`Unexpected error for user ${index}:`, error.message);
      }
    }
  });

  await Promise.all(bookingPromises);
  const duration = Date.now() - startTime;

  console.log('---------------------------------------------------------------');
  console.log(`⏱️ Completed 100 concurrent requests in ${duration}ms`);
  console.log(`✅ Successful bookings: ${successCount}`);
  console.log(`❌ Rejected conflicts (HTTP 409): ${conflictCount}`);
  console.log(`⚠️ Other errors: ${otherErrorCount}`);
  console.log('---------------------------------------------------------------');

  // Verify Database State
  const finalSlot = await Slot.findById(testSlot._id);
  const bookingsInDb = await Booking.countDocuments({
    courtId: court._id,
    date: testDate,
    slotIds: testSlot._id,
  });

  console.log(`[Database Verification]`);
  console.log(` - Slot Status in DB: ${finalSlot?.status}`);
  console.log(` - Total Confirmed Bookings in DB: ${bookingsInDb}`);

  if (successCount === 1 && conflictCount === totalConcurrentUsers - 1 && bookingsInDb === 1 && finalSlot?.status === 'BOOKED') {
    console.log('🎉 TEST PASSED: Concurrency control strictly prevented double booking!');
    process.exit(0);
  } else {
    console.error('🚨 TEST FAILED: Inconsistent database state or race condition occurred!');
    process.exit(1);
  }
}

runConcurrencyTest().catch((err) => {
  console.error('Test execution failed:', err);
  process.exit(1);
});
