import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

export const ENV = {
  PORT: parseInt(process.env.PORT || '5000', 10),
  MONGODB_URI: process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/courtbook',
  JWT_SECRET: process.env.JWT_SECRET || 'courtbook_super_secret_jwt_key_for_academic_course_project_2026',
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',
  NODE_ENV: process.env.NODE_ENV || 'development',
  HOLD_DURATION_MINUTES: parseInt(process.env.HOLD_DURATION_MINUTES || '5', 10),
};
