"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.errorHandler = errorHandler;
function errorHandler(err, req, res, next) {
    // If headers already sent, delegate to Express default handler
    if (res.headersSent) {
        return next(err);
    }
    let statusCode = err.statusCode || 500;
    let message = err.message || 'Internal server error';
    // Handle Mongoose duplicate key error (E11000)
    if (err.code === 11000) {
        statusCode = 409;
        const field = Object.keys(err.keyPattern || {})[0] || 'field';
        message = `Duplicate resource error: A record with this ${field} already exists.`;
    }
    // Handle Mongoose CastError (invalid ObjectId format)
    if (err.name === 'CastError') {
        statusCode = 400;
        message = `Invalid ID format for path: ${err.path}`;
    }
    // Handle Mongoose ValidationError
    if (err.name === 'ValidationError') {
        statusCode = 400;
        const errors = Object.values(err.errors).map((el) => el.message);
        message = `Validation failed: ${errors.join(', ')}`;
    }
    // Handle WriteConflict in MongoDB transactions
    if (err.name === 'MongoServerError' && err.hasErrorLabel && err.hasErrorLabel('TransientTransactionError')) {
        statusCode = 409;
        message = 'Concurrent modification detected during booking transaction. Please retry.';
    }
    if (statusCode >= 500) {
        console.error('[Unhandled Exception]:', err);
    }
    res.status(statusCode).json({
        success: false,
        status: statusCode,
        message,
        ...(process.env.NODE_ENV === 'development' && { stack: err.stack }),
    });
}
