var express = require('express');
var router = express.Router();
const fs = require('fs'); 

// Endpoint to Get a list of users
router.get('/getJSON', function(req, res) {
  fs.readFile(__dirname + "/" + "database.json", 'utf8', function(err, data) {
    res.end(data);
  });
});


module.exports = router;
