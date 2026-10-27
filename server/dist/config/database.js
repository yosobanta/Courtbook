"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.isReplicaSet = void 0;
exports.connectDatabase = connectDatabase;
const mongoose_1 = __importDefault(require("mongoose"));
const env_1 = require("./env");
exports.isReplicaSet = false;
async function connectDatabase() {
    try {
        mongoose_1.default.set('strictQuery', true);
        await mongoose_1.default.connect(env_1.ENV.MONGODB_URI, {
            serverSelectionTimeoutMS: 5000,
        });
        console.log(`[MongoDB] Connected successfully to: ${mongoose_1.default.connection.host}:${mongoose_1.default.connection.port}/${mongoose_1.default.connection.name}`);
        // Check if connected instance supports transactions (requires replica set)
        try {
            const adminDb = mongoose_1.default.connection.db?.admin();
            const serverStatus = await adminDb?.command({ serverStatus: 1 });
            if (serverStatus?.repl) {
                exports.isReplicaSet = true;
                console.log(`[MongoDB] Replica Set detected (${serverStatus.repl.setName}). Multi-document ACID transactions with snapshot isolation ENABLED.`);
            }
            else {
                exports.isReplicaSet = false;
                console.log(`[MongoDB] Running in Standalone mode. Multi-document transactions require a replica set (mongod --replSet or MongoDB Atlas). Atomic conditional operations will be used.`);
            }
        }
        catch {
            // If serverStatus command is restricted, test if startSession supports transactions
            exports.isReplicaSet = false;
            console.log(`[MongoDB] Transaction capability check skipped. Defaulting to replica-set detection on session.`);
        }
        mongoose_1.default.connection.on('error', (err) => {
            console.error('[MongoDB] Connection error:', err);
        });
        mongoose_1.default.connection.on('disconnected', () => {
            console.warn('[MongoDB] Disconnected. Attempting reconnection...');
        });
    }
    catch (error) {
        console.error('[MongoDB] Failed to connect to database:', error);
        // Do not terminate process in dev mode so the app can start or display helpful messages
    }
}
