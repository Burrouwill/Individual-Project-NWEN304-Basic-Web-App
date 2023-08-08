var express = require('express');
var router = express.Router();
const Item = require('./models/itemModel');


router.delete('/deleteMONGO/:id', async(req, res) =>{
    try {
        const Item = req.body;
        const item = await Item.delete({});
        if(!item){
            return res.status(404).json({message: `cannot find any product with ID ${id}`})
        }
        res.status(200).json(product);
        
    } catch (error) {
        res.status(500).json({message: error.message})
    }
})

module.exports = router;