import express, { Request, Response } from 'express';
import cors from 'cors';
import { ENV } from './config/env';
import { connectDatabase } from './config/database';
import apiRouter from './routes';
import { errorHandler } from './middleware/errorHandler';
import { SlotService } from './services/slotService';

const app = express();

// Standard middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Minimal development request logger
if (ENV.NODE_ENV !== 'production') {
  app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
    next();
  });
}

// API Routes
app.use('/api', apiRouter);

// Fallback 404 handler
app.use((req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    status: 404,
    message: `API endpoint ${req.method} ${req.originalUrl} not found`,
  });
});

// Centralized error handling
app.use(errorHandler);

// Connect to MongoDB and start HTTP server


async function startServer() {
  await connectDatabase();

  // Periodic background cleanup: release expired holds across courts every 30 seconds
  setInterval(() => {
    SlotService.releaseExpiredHolds().catch((err) => {
      console.error('[SlotHoldCleanup] Error releasing expired holds:', err);
    });
  }, 30000);

  const PORT = Number(process.env.PORT || ENV.PORT);

  const server = app.listen(PORT, '0.0.0.0', () => {
    console.log('====================================================');
    console.log(`🚀 CourtBook API Server running on port ${PORT}`);
    console.log(`📡 Base URL: http://localhost:${PORT}/api`);
    console.log(`🏥 Health check: http://localhost:${PORT}/api/health`);
    console.log('====================================================');
  });

  return server;
}


if (process.env.NODE_ENV !== 'test') {
  startServer();
}

export { app, startServer };
