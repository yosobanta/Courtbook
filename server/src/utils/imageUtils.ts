import fs from 'fs';
import path from 'path';

/**
 * Resolves Google Photos share links, Google Drive links, and web page links to direct image URLs.
 */
export async function normalizeImageUrl(rawUrl: string): Promise<string> {
  if (!rawUrl || typeof rawUrl !== 'string') return '';
  const trimmed = rawUrl.trim();
  if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
    return trimmed;
  }

  try {
    // 1. Google Drive view/open link -> Direct image link
    const driveMatch = trimmed.match(/drive\.google\.com\/(?:file\/d\/|open\?id=)([a-zA-Z0-9_-]+)/);
    if (driveMatch && driveMatch[1]) {
      return `https://drive.google.com/uc?export=view&id=${driveMatch[1]}`;
    }

    // 2. Google Photos link (photos.app.goo.gl or photos.google.com/share)
    if (trimmed.includes('photos.app.goo.gl') || trimmed.includes('photos.google.com')) {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 6000);
      const res = await fetch(trimmed, {
        headers: {
          'User-Agent':
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8',
        },
        redirect: 'follow',
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      const html = await res.text();
      // Look for og:image meta tag
      const ogMatch =
        html.match(/<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']/i) ||
        html.match(/<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']/i) ||
        html.match(/<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']/i);

      if (ogMatch && ogMatch[1]) {
        return ogMatch[1].replace(/&amp;/g, '&');
      }
    }
  } catch (err) {
    console.warn('[normalizeImageUrl] Failed to resolve URL:', trimmed, err);
  }

  return trimmed;
}

/**
 * Saves a base64 encoded image to the uploads directory and returns its relative URL path.
 */
export async function saveBase64Image(base64Data: string, filenamePrefix = 'turf'): Promise<string> {
  const uploadsDir = path.join(process.cwd(), 'uploads');
  if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
  }

  let cleanBase64 = base64Data;
  let ext = 'jpg';
  const match = base64Data.match(/^data:image\/([a-zA-Z0-9+]+);base64,(.+)$/);
  if (match) {
    ext = match[1] === 'jpeg' ? 'jpg' : match[1];
    cleanBase64 = match[2];
  }

  const filename = `${filenamePrefix}_${Date.now()}_${Math.random().toString(36).substring(2, 8)}.${ext}`;
  const filePath = path.join(uploadsDir, filename);

  const buffer = Buffer.from(cleanBase64, 'base64');
  await fs.promises.writeFile(filePath, buffer);

  return `/uploads/${filename}`;
}
