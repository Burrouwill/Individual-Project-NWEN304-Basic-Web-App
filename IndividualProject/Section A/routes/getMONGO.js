var express = require('express');
var router = express.Router();
const Item = require('./models/itemModel');


router.get('/getMONGO', async (req, res) => {
    try {
      const items = await Item.find({});
      res.status(200).json(items)
    } catch (error) {
      res.status(500).json({message: error.message})
    }
  });



module.exports = router;
