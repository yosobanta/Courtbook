"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SlotController = void 0;
const slotService_1 = require("../services/slotService");
const errors_1 = require("../utils/errors");
class SlotController {
    static async getCourtSlots(req, res, next) {
        try {
            const courtId = req.params.id;
            const date = req.query.date || new Date().toISOString().split('T')[0];
            if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
                throw new errors_1.BadRequestError('Date must be formatted as YYYY-MM-DD');
            }
            const slots = await slotService_1.SlotService.getCourtSlotsByDate(courtId, date);
            res.status(200).json({ success: true, count: slots.length, date, data: slots });
        }
        catch (error) {
            next(error);
        }
    }
    static async holdSlots(req, res, next) {
        try {
            const userId = req.user._id.toString();
            const { slotIds, holdMinutes } = req.body;
            if (!slotIds || !Array.isArray(slotIds) || slotIds.length === 0) {
                throw new errors_1.BadRequestError('Array of slotIds is required');
            }
            const result = await slotService_1.SlotService.holdSlots(userId, slotIds, holdMinutes);
            res.status(200).json({
                success: true,
                message: 'Slots reserved on temporary hold',
                data: result,
            });
        }
        catch (error) {
            next(error);
        }
    }
}
exports.SlotController = SlotController;
