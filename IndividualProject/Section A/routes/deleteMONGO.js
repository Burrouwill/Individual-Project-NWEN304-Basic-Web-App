var express = require('express');
var router = express.Router();
const Item = require('./models/itemModel');


router.delete('/deleteMONGO/:id', async (req, res) => {
    try {
        const id = req.params.id; // Extract the item ID from the URL parameter
        const item = await Item.findOneAndDelete({ id: id }); // Use findOneAndDelete to delete the item by its id field
        if (!item) {
            return res.status(404).json({ message: `Cannot find any product with ID ${id}` });
        }
        res.status(200).json(item);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
});

module.exports = router;
