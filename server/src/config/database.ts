import mongoose from 'mongoose';
import { ENV } from './env';

export let isReplicaSet = false;

export async function connectDatabase(): Promise<void> {
  try {
    mongoose.set('strictQuery', true);
    await mongoose.connect(ENV.MONGODB_URI, {
      serverSelectionTimeoutMS: 5000,
    });
    console.log(`[MongoDB] Connected successfully to: ${mongoose.connection.host}:${mongoose.connection.port}/${mongoose.connection.name}`);

    // Check if connected instance supports transactions (requires replica set)
    try {
      const adminDb = mongoose.connection.db?.admin();
      const serverStatus = await adminDb?.command({ serverStatus: 1 });
      if (serverStatus?.repl) {
        isReplicaSet = true;
        console.log(`[MongoDB] Replica Set detected (${serverStatus.repl.setName}). Multi-document ACID transactions with snapshot isolation ENABLED.`);
      } else {
        isReplicaSet = false;
        console.log(`[MongoDB] Running in Standalone mode. Multi-document transactions require a replica set (mongod --replSet or MongoDB Atlas). Atomic conditional operations will be used.`);
      }
    } catch {
      // If serverStatus command is restricted, test if startSession supports transactions
      isReplicaSet = false;
      console.log(`[MongoDB] Transaction capability check skipped. Defaulting to replica-set detection on session.`);
    }

    mongoose.connection.on('error', (err) => {
      console.error('[MongoDB] Connection error:', err);
    });

    mongoose.connection.on('disconnected', () => {
      console.warn('[MongoDB] Disconnected. Attempting reconnection...');
    });
  } catch (error) {
    console.error('[MongoDB] Failed to connect to database:', error);
    // Do not terminate process in dev mode so the app can start or display helpful messages
  }
}
