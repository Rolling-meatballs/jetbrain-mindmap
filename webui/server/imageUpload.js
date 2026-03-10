/**
 * Demo Node.js upload handler for KityMinder image upload.
 *
 * Response format must stay compatible with the editor:
 * { errno: number, msg: string, data: { url: string } }
 */
const path = require('path');
const fs = require('fs/promises');
const crypto = require('crypto');

const MAX_SIZE_BYTES = 1 * 1000 * 1000;
const ALLOWED_MIME_TYPES = new Set([
  'image/gif',
  'image/jpeg',
  'image/jpg',
  'image/png',
]);

function getExtName(file) {
  if (!file || file.originalname === 'blob') return 'png';
  const ext = path.extname(file.originalname || '').replace('.', '').toLowerCase();
  return ext || 'png';
}

async function imageUploadHandler(req, res) {
  try {
    const file = req.file;
    if (!file) {
      return res.json({ errno: 416, msg: 'File is invalid', data: { url: '' } });
    }

    if (!ALLOWED_MIME_TYPES.has(file.mimetype) || file.size > MAX_SIZE_BYTES) {
      return res.json({ errno: 416, msg: 'File is invalid', data: { url: '' } });
    }

    const extName = getExtName(file);
    const sha1Name = `${crypto.createHash('sha1').update(file.buffer).digest('hex')}.${extName}`;
    const uploadDir = path.join(__dirname, 'upload');
    const targetPath = path.join(uploadDir, sha1Name);

    await fs.mkdir(uploadDir, { recursive: true });
    await fs.writeFile(targetPath, file.buffer);

    const publicBase = process.env.IMAGE_UPLOAD_PUBLIC_BASE || '';
    const base = publicBase.endsWith('/') ? publicBase.slice(0, -1) : publicBase;
    const url = base ? `${base}/server/upload/${sha1Name}` : `/server/upload/${sha1Name}`;

    return res.json({ errno: 0, msg: 'ok', data: { url } });
  } catch (err) {
    return res.json({
      errno: 500,
      msg: err && err.message ? err.message : 'Upload failed',
      data: { url: '' },
    });
  }
}

module.exports = {
  imageUploadHandler,
};
