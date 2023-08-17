var express = require('express');
var router = express.Router();
const Item = require('./models/itemModel');

router.put('/putMONGO/:id', async (req, res) => {
    try {
        const id = req.params.id; // Extract the item ID from the URL parameter

        const updatedItem = await Item.findOneAndUpdate(
            { id: id }, 
            { $set: req.body }, 
            { new: true } 
        );

        if (!updatedItem) {
            return res.status(404).json({ message: `Cannot find any product with ID ${id}` });
        }

        res.status(200).json(updatedItem);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
});

module.exports = router;