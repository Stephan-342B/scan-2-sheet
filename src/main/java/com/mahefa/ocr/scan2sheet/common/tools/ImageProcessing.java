package com.mahefa.ocr.scan2sheet.common.tools;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImageProcessing {

    protected static List<Rect> processHeader(final Mat mat) {
        // ROI
        Rect rect = new Rect(0, 0, mat.width(), 22);
        Mat section = new Mat(mat, rect);

        // Convert the image to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(section, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply Canny edge detection
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 100, 255, 3, false);

        // Apply a distance transform with DIST_L2 and a kernel size of 5
        Mat dist = new Mat();
        Imgproc.distanceTransform(edges, dist, Imgproc.DIST_L2, 5);

        // Normalize the distance transform
        Core.normalize(dist, dist, 0, 1.0, Core.NORM_MINMAX);

        // Convert the distance transform to an unsigned 8-bit integer
        dist.convertTo(dist, CvType.CV_8U, 255);

        // Threshold the distance transform using Otsu's method
        Mat thresholded = new Mat();
        Imgproc.threshold(dist, thresholded, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        // Uncomment only for debugging
//        Imgcodecs.imwrite("/home/stephan/Workspace/Personnal/Serious_Project/scan-2-sheet/folder/processed/edges.png", thresholded);

        return detectTextRegions(thresholded, section);
    }

    protected Mat processChunkImage(final Mat roi, int row, int col) {
        Mat grayscale = new Mat();

        if (roi.channels() > 1) {
            Imgproc.cvtColor(roi, grayscale, Imgproc.COLOR_BGR2GRAY);
        } else {
            roi.copyTo(grayscale);
        }

        // Upscale so the small (~10px) glyphs have enough pixels for recognition.
        Mat resized = new Mat();
        Imgproc.resize(grayscale, resized,
                new Size(grayscale.width() * 5, grayscale.height() * 5), 0, 0, Imgproc.INTER_CUBIC);

//        Imgcodecs.imwrite("/home/stephan/Workspace/Personnal/Serious_Project/scan-2-sheet/folder/processed/resized_" + row + "_" + col + ".png", resized);

        return resized;
    }

    protected Mat getROI(Mat originalImage, int x, int y, int width, int height) {
        final Rect row = new Rect(x, y, width, height);
        return new Mat(originalImage, row);
    }

    private static List<Rect> detectTextRegions(Mat modified, Mat original) {
        // Text Detection using Contours
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>(0);
        List<Rect> regions = new ArrayList<>(0);

        Imgproc.findContours(modified, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Iterate through detected text regions
        for (MatOfPoint contour : contours) {
            final Rect region = Imgproc.boundingRect(contour);

            if (region.width > 5 && region.height > 5 && region.width < 250 && region.height < 20) {
                regions.add(region);
            }
        }

        // Uncomment only for debugging
//        regions = mergeRegions(regions);
//
//        for (Rect region : regions) {
//            // Draw a rectangle around the text region
//            Imgproc.rectangle(
//                    original, new Point(region.x, region.y), new Point(region.x + region.width, region.y + region.height),
//                    new Scalar(0, 255, 0), 1
//            );
//        }
//
//        Imgcodecs.imwrite("/home/stephan/Workspace/Personnal/Serious_Project/scan-2-sheet/folder/processed/header.png", original);

//        return regions;

        return mergeRegions(regions);
    }

    public static List<Rect> mergeRegions(List<Rect> regions) {
        List<Rect> mergedRegions = new ArrayList<>(0);
        regions.sort(Comparator.comparingInt(region -> region.x));

        for (Rect region : regions) {
            boolean merged = false;

            for (int i = 0; i < mergedRegions.size(); i++) {
                Rect existingRegion = mergedRegions.get(i);

                if (isOverlap(region, existingRegion) || isNearby(region, existingRegion)) {
                    // Merge the rectangles if they overlap
                    Rect mergedRect = merge(region, existingRegion);
                    mergedRegions.remove(existingRegion);
                    mergedRegions.add(mergedRect);
                    merged = true;
                }
            }

            if (!merged) {
                // If the rectangle did not overlap with any existing merged rectangle, add it as is
                mergedRegions.add(region);
            }
        }

        mergedRegions.sort(Comparator.comparingInt(region -> region.x));

        return mergedRegions;
    }

    // Check if two rectangles overlap
    private static boolean isOverlap(Rect rect1, Rect rect2) {
        return (rect1.x <= (rect2.x + rect2.width) && (rect1.x + rect1.width) >= rect2.x)
                && (rect1.y <= (rect2.y + rect2.height) && (rect1.y + rect1.height) >= rect2.y);
    }

    // Check if two rectangles are nearby
    private static boolean isNearby(Rect rect1, Rect rect2) {
        return (rect1.x - (rect2.x + rect2.width) >= 1 && rect1.x - (rect2.x + rect2.width) <= 5);
    }

    // Merge two rectangles into one
    private static Rect merge(Rect rect1, Rect rect2) {
        int x = Math.min(rect1.x, rect2.x);
        int y = Math.min(rect1.y, rect2.y);
        int width = Math.max(rect1.x + rect1.width, rect2.x + rect2.width) - x;
        int height;

        // Adjust height
        if (rect1.y + rect1.height >= 22) {
            height = (rect2.y + rect2.height) - y;
        } else if (rect2.y + rect2.height >= 22) {
            height = (rect1.y + rect1.height) - y;
        } else {
            height = Math.max(rect1.y + rect1.height, rect2.y + rect2.height) - y;
        }

        return new Rect(x, y, width, height);
    }
}
