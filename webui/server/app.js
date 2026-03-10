const path = require('path');
const express = require('express');
const { createKityMinderUploadRouter } = require('./express-upload-route.example');

const app = express();
const port = Number(process.env.PORT || 3030);

// Serve uploaded files so the editor can resolve returned image URLs.
app.use('/server/upload', express.static(path.join(__dirname, 'upload')));
app.use(createKityMinderUploadRouter());

app.get('/healthz', (_req, res) => {
  res.json({ ok: true });
});

app.listen(port, () => {
  console.log(`[kityminder-upload] listening on http://localhost:${port}`);
});
