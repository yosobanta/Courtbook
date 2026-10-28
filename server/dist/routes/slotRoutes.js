"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const slotController_1 = require("../controllers/slotController");
const auth_1 = require("../middleware/auth");
const router = (0, express_1.Router)();
// Slot hold reservation endpoint (two-phase locking)
router.post('/hold', auth_1.authenticate, slotController_1.SlotController.holdSlots);
// Explicit release of held slots back to available
router.post('/release', auth_1.authenticate, slotController_1.SlotController.releaseHold);
exports.default = router;
