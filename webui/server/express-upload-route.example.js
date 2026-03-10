/**
 * Express route wiring example for KityMinder image uploads.
 *
 * Usage (in your server entry):
 *   const express = require('express');
 *   const { createKityMinderUploadRouter } = require('./webui/server/express-upload-route.example');
 *   const app = express();
 *   app.use(createKityMinderUploadRouter());
 */
const express = require('express');
const multer = require('multer');
const { imageUploadHandler } = require('./imageUpload');

function createKityMinderUploadRouter() {
  const router = express.Router();
  const upload = multer({ storage: multer.memoryStorage() });

  // Front-end expects this endpoint from config.service.js / main.js
  router.post('/api/upload-image', upload.single('upload_file'), imageUploadHandler);

  return router;
}

module.exports = {
  createKityMinderUploadRouter,
};
