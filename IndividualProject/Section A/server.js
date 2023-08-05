const express = require('express');
const app = express();
const fs = require('fs');
const cors = require('cors'); // require cors

// Define CORS options
const corsOptions = {
  origin: '*',
  credentials: true,
  optionSuccessStatus: 200,
};

// Enable CORS
app.use(cors(corsOptions)); // Use this before defining routes

// Endpoint to Get a list of users
app.get('/getData', function(req, res) {
  fs.readFile(__dirname + "/" + "database.json", 'utf8', function(err, data) {
    console.log(data);
    res.end(data);
  });
});

// Create a server to listen at port 8080
const server = app.listen(8080, function() {
  const host = server.address().address;
  const port = server.address().port;
  console.log("REST API listening at http://%s:%s", host, port);
});