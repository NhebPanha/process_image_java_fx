package com.nhebpanha.cvstudio.pipeline;

import com.nhebpanha.cvstudio.model.Frame;
import com.nhebpanha.cvstudio.model.PipelineSettings;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

/**
 * Stage 2: Preprocessing.
 * Enhances images by adjusting brightness, contrast, denoising (Gaussian blur),
 * downsampling/resizing, grayscale conversion, and CLAHE equalization.
 */
public class PreprocessStage implements PipelineStage {

    @Override
    public String name() {
        return "2 Preprocessing";
    }

    @Override
    public Frame apply(Frame frame, PipelineSettings s) {
        Mat src = frame.getOriginal();
        if (src == null || src.empty()) {
            return frame;
        }

        Mat out = new Mat();

        // 1. Brightness (beta) and contrast (alpha): out = alpha * src + beta
        src.convertTo(out, -1, s.getContrast(), s.getBrightness());

        // 2. Optional resize for consistent downstream throughput
        if (s.isResizeEnabled() && s.getTargetWidth() > 0 && s.getTargetHeight() > 0) {
            Imgproc.resize(out, out, new Size(s.getTargetWidth(), s.getTargetHeight()), 0, 0, Imgproc.INTER_AREA);
        }

        // 3. Gaussian Blur Denoise (odd kernel size)
        int k = s.getBlurKernel();
        if (k > 1) {
            if (k % 2 == 0) k++;
            Imgproc.GaussianBlur(out, out, new Size(k, k), 0);
        }

        // 4. Grayscale conversion
        if (s.isGrayscale() && out.channels() == 3) {
            Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2GRAY);
        }

        // 5. Contrast-Limited Adaptive Histogram Equalization (CLAHE)
        if (s.isClaheEnabled()) {
            Mat gray;
            boolean converted = false;
            if (out.channels() == 1) {
                gray = out;
            } else {
                gray = new Mat();
                Imgproc.cvtColor(out, gray, Imgproc.COLOR_BGR2GRAY);
                converted = true;
            }

            CLAHE clahe = Imgproc.createCLAHE(s.getClaheClipLimit(), new Size(8, 8));
            clahe.apply(gray, gray);

            if (converted) {
                out.release();
                out = gray;
            }
        }

        // Release old processed Mat if already set
        if (frame.getProcessed() != null && frame.getProcessed() != src) {
            frame.getProcessed().release();
        }
        frame.setProcessed(out);

        return frame;
    }
}
