var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

var getJSON = require('./routes/getJSON');
var postJSON = require('./routes/postJSON');
var postMONGO = require('./routes/postMONGO');
var getMONGO = require('./routes/getMONGO');
var deleteMONGO = require('./routes/deleteMONGO');
var putMONGO = require('./routes/putMONGO');

var cors = require('cors'); 

var app = express();

// Define CORS options
const corsOptions = {
  origin: '*',
  credentials: true,
  optionSuccessStatus: 200,
};

// Enable CORS
app.use(cors(corsOptions)); // Use this before defining routes


// MongoDB
const mongoose = require('mongoose')
mongoose.set("strictQuery", false)
mongoose.
connect('mongodb+srv://admin:admin@cluster0.oifliqb.mongodb.net/node-api?retryWrites=true&w=majority')
.then(() => {
    console.log('Connected to MongoDB')
}).catch((error) => {
    console.log(error)
})


// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', getJSON);
app.use('/p', postJSON);
app.use('/m', postMONGO);
app.use('/g', getMONGO);
app.use('/d', deleteMONGO);
app.use('/u', putMONGO);


// catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;

