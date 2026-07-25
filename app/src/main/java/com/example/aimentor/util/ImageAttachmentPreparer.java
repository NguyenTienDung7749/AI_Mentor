package com.example.aimentor.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.example.aimentor.ai.ImageAttachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Downsamples, rotates and strips metadata from one image before upload. */
public final class ImageAttachmentPreparer {

    private static final int MAX_EDGE_PX = 1800;
    private static final int MAX_ENCODED_BYTES = 5 * 1024 * 1024;

    private ImageAttachmentPreparer() { }

    public static Result prepare(@NonNull Context context, @NonNull Uri uri)
            throws IOException {
        ContentResolver resolver = context.getContentResolver();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IOException("Image could not be opened.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Unsupported image.");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IOException("Image could not be opened.");
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IOException("Image could not be decoded.");

        Bitmap oriented = rotateIfNeeded(decoded, readOrientation(resolver, uri));
        if (oriented != decoded) decoded.recycle();
        Bitmap scaled = scaleDown(oriented);
        if (scaled != oriented) oriented.recycle();

        byte[] encoded;
        try {
            encoded = encodeJpeg(scaled);
        } finally {
            scaled.recycle();
        }
        if (encoded.length > MAX_ENCODED_BYTES) {
            throw new IOException("Image is too large after compression.");
        }
        String base64 = Base64.encodeToString(encoded, Base64.NO_WRAP);
        return new Result(
                new ImageAttachment("image/jpeg", base64), encoded);
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        while (Math.max(width / sample, height / sample) > MAX_EDGE_PX * 2) {
            sample *= 2;
        }
        return sample;
    }

    private static int readOrientation(ContentResolver resolver, Uri uri) {
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) return ExifInterface.ORIENTATION_NORMAL;
            return new ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException | RuntimeException ignored) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static Bitmap rotateIfNeeded(Bitmap source, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1f, -1f);
                break;
            default:
                return source;
        }
        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    private static Bitmap scaleDown(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= MAX_EDGE_PX) return source;
        float ratio = MAX_EDGE_PX / (float) longest;
        return Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(width * ratio)),
                Math.max(1, Math.round(height * ratio)), true);
    }

    private static byte[] encodeJpeg(Bitmap bitmap) throws IOException {
        int quality = 92;
        byte[] result;
        do {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                throw new IOException("Image could not be compressed.");
            }
            result = output.toByteArray();
            quality -= 8;
        } while (result.length > MAX_ENCODED_BYTES && quality >= 68);
        return result;
    }

    public static final class Result {
        public final ImageAttachment attachment;
        public final byte[] previewBytes;

        Result(ImageAttachment attachment, byte[] previewBytes) {
            this.attachment = attachment;
            this.previewBytes = previewBytes;
        }
    }
}
