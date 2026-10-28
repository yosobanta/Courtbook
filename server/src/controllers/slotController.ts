import { Request, Response, NextFunction } from 'express';
import { SlotService } from '../services/slotService';
import { AuthRequest } from '../middleware/auth';
import { BadRequestError } from '../utils/errors';

export class SlotController {
  public static async getCourtSlots(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const courtId = req.params.id;
      const date = (req.query.date as string) || new Date().toISOString().split('T')[0];

      if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        throw new BadRequestError('Date must be formatted as YYYY-MM-DD');
      }

      const slots = await SlotService.getCourtSlotsByDate(courtId, date);
      res.status(200).json({ success: true, count: slots.length, date, data: slots });
    } catch (error) {
      next(error);
    }
  }

  public static async holdSlots(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!._id.toString();
      const { slotIds, holdMinutes } = req.body;

      if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
        throw new BadRequestError('Array of slotIds is required');
      }

      const result = await SlotService.holdSlots(userId, slotIds, holdMinutes);
      res.status(200).json({
        success: true,
        message: 'Slots reserved on temporary hold',
        data: result,
      });
    } catch (error) {
      next(error);
    }
  }

  public static async releaseHold(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!._id.toString();
      const { slotIds } = req.body || {};

      const result = await SlotService.releaseHold(userId, slotIds);
      res.status(200).json({
        success: true,
        message: `Released ${result.releasedCount} held slot(s)`,
        data: result,
      });
    } catch (error) {
      next(error);
    }
  }
}
