package com.mahefa.ocr.scan2sheet.common.tools;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageConverter {

    public static BufferedImage toBufferedImage(File gifFile) {
        List<BufferedImage> frameImages = new ArrayList<>(0);

        try {
            ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
            reader.setInput(ImageIO.createImageInputStream(gifFile));

            int numFrames = reader.getNumImages(true);

            for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
                BufferedImage frameImage = reader.read(frameIndex);

                // Add the frame image to the list
                frameImages.add(frameImage);
            }

            if (!frameImages.isEmpty()) {
                BufferedImage bufferedImage = frameImages.get(0);

                // Check if the BufferedImage has an IndexColorModel
                if (bufferedImage.getColorModel() instanceof IndexColorModel) {
                    // Convert the indexed image to RGB
                    BufferedImage convertedImage = new BufferedImage(
                            bufferedImage.getWidth(),
                            bufferedImage.getHeight(),
                            BufferedImage.TYPE_3BYTE_BGR
                    );

                    convertedImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
                    return convertedImage;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static BufferedImage toBufferedImage(final Mat mat) {
        // Create buffered image
        final BufferedImage bufferedImage = new BufferedImage(
                mat.width(),
                mat.height(),
                mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR
        );

        // Write data to image
        final WritableRaster raster = bufferedImage.getRaster();
        final DataBufferByte dataBufferByte = (DataBufferByte) raster.getDataBuffer();
        mat.get(0, 0, dataBufferByte.getData());

        return bufferedImage;
    }

    public static Mat toOpenCVMat(BufferedImage bufferedImage) {
        // Get the number of channels (assuming it's 3 for RGB)
        int numChannels = 3;

        // Get the width and height of the image
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // Create a Mat of the correct size and type
        Mat mat = new Mat(height, width, CvType.CV_8UC(numChannels));

        // Get the data from the BufferedImage
        DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();

        if (dataBuffer instanceof DataBufferByte) {
            // If the data is in byte format (common for RGB images)
            byte[] data = ((DataBufferByte) dataBuffer).getData();
            mat.put(0, 0, data);
        } else if (dataBuffer instanceof DataBufferInt) {
            // If the data is in integer format, convert it to bytes
            int[] data = ((DataBufferInt) dataBuffer).getData();
            byte[] byteData = new byte[data.length * 4]; // 4 bytes per int

            for (int i = 0; i < data.length; i++) {
                byteData[i * 3] = (byte) ((data[i] >> 16) & 0xFF); // Red channel
                byteData[i * 3 + 1] = (byte) ((data[i] >> 8) & 0xFF); // Green channel
                byteData[i * 3 + 2] = (byte) (data[i] & 0xFF); // Blue channel
            }

            mat.put(0, 0, byteData);
        } else {
            throw new IllegalArgumentException("Unsupported data buffer type: " + dataBuffer.getClass().getName());
        }

        return mat;
    }
}
