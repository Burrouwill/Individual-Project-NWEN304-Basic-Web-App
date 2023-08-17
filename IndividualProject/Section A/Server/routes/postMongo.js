var express = require('express');
var router = express.Router();
const Item = require('./models/itemModel');

router.post('/postMONGO', async (req, res) => {
    try {
        const item = await Item.create(req.body);
        res.status(200).json(item);
    } catch (error) {
        console.log(error.message);
        res.status(500).json({ message: error.message });
    }
});

module.exports = router;



