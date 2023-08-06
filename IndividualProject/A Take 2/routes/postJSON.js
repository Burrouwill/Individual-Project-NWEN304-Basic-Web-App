var express = require('express');
var router = express.Router();
const fs = require('fs'); 

// Endpoint to Add new data via POST request
router.post('/updateJSON', (req, res) => {
  // Assuming itemOperations is defined and accessible in this scope
  console.log(JSON.stringify(req.body));
  fs.writeFile(__dirname + "/" + 'database.json', JSON.stringify(req.body), 'utf8', (err) => {
      if (err) {
          console.error('Error updating data:', err);
          res.status(500).json({ error: 'Error updating data.' });
          return;
      }
      res.json({ message: 'Data updated successfully.' });
  });
});

module.exports = router;