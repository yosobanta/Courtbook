"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const offerController_1 = require("../controllers/offerController");
const router = (0, express_1.Router)();
router.get('/active', offerController_1.OfferController.getActiveOffers);
router.post('/validate', offerController_1.OfferController.validateOffer);
exports.default = router;
